package com.game.knowledge

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * From Math Planet v1.3.6: 48kHz stereo, 1800ms reusable buffered AudioTrack,
 * prefill 75% (up to 1350ms) before play, worker on THREAD_PRIORITY_AUDIO.
 */
internal object TabletPcmPolicy {
    const val SAMPLE_RATE=48_000
    const val FRAME_BYTES=4
    const val BUFFER_MS=1_800

    fun bufferSize(platformMinBytes:Int):Int {
        require(platformMinBytes>0)
        val target=maxOf(platformMinBytes,SAMPLE_RATE*FRAME_BYTES*BUFFER_MS/1000)
        return target+(FRAME_BYTES-target%FRAME_BYTES)%FRAME_BYTES
    }

    fun prefillBytes(bufferBytes:Int,dataBytes:Int):Int {
        val n=minOf(bufferBytes*3/4,dataBytes)
        return n-n%FRAME_BYTES
    }

    /** A partly spoken sentence must never restart from sample zero. */
    fun canFallbackAfterFailure(started:Boolean,current:Boolean):Boolean =
        !started && current
}

internal data class SpeechPlaybackResult(val completed:Boolean,val started:Boolean)

internal class BufferedSpeechPlayer {
    private val tag="KnowledgeSpeechPcm"
    private val main=Handler(Looper.getMainLooper())
    private val worker=Executors.newSingleThreadExecutor { task ->
        Thread({
            // Match the working Math Planet v1.3.6 audio-worker scheduling.
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            task.run()
        },"KnowledgeSpeechPcm").apply {isDaemon=true}
    }
    private val warmupQueued=AtomicBoolean(false)
    @Volatile private var generation=0
    @Volatile private var closed=false

    // Exclusive ownership: ONLY the serialized worker creates/uses/releases it.
    private var bufferedOutput:AudioTrack?=null

    private fun attributes():AudioAttributes=AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private fun sharedTrack():AudioTrack {
        bufferedOutput?.let {
            if(it.state==AudioTrack.STATE_INITIALIZED) return it
        }
        releaseShared()
        val min=AudioTrack.getMinBufferSize(
            SpeechPcm.OUTPUT_HZ,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT)
        check(min>0) {"No output PCM buffer: "+min}
        val bufferBytes=TabletPcmPolicy.bufferSize(min)
        val track=AudioTrack.Builder()
            .setAudioAttributes(attributes())
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(SpeechPcm.OUTPUT_HZ)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferBytes)
            .build()
        if(track.state!=AudioTrack.STATE_INITIALIZED) {
            track.release()
            error("Cannot initialize reusable PCM AudioTrack")
        }
        bufferedOutput=track
        Log.i(tag,"Created reusable output, requestedBuffer="+bufferBytes+
            " actualBuffer="+track.bufferSizeInFrames*TabletPcmPolicy.FRAME_BYTES+
            " platformMinimum="+min)
        return track
    }

    private fun releaseShared() {
        val old=bufferedOutput
        bufferedOutput=null
        runCatching {old?.release()}
    }

    /** First playback reuses this warmed audio route; no UI-thread AudioTrack. */
    fun prepare() {
        if(closed || !warmupQueued.compareAndSet(false,true)) return
        worker.execute {
            if(closed) return@execute
            var track:AudioTrack?=null
            var warmed=false
            try {
                track=sharedTrack()
                val silence=ByteArray(9600) // 50ms 48kHz stereo PCM16
                check(track.write(silence,0,silence.size,AudioTrack.WRITE_BLOCKING)==silence.size)
                track.play()
                Thread.sleep(65L)
                warmed=true
                Log.i(tag,"Reusable stream output warmed for first speech")
            } catch(e:Exception) {
                Log.w(tag,"PCM warmup failed; next phrase may retry",e)
                warmupQueued.set(false)
            } finally {
                val stopped=runCatching {
                    if(track?.playState!=AudioTrack.PLAYSTATE_STOPPED) track?.stop()
                }.isSuccess
                val flushed=runCatching {track?.flush()}.isSuccess
                if(!warmed || !stopped || !flushed) releaseShared()
            }
        }
    }

    fun play(file:File,onFinished:(SpeechPlaybackResult)->Unit) {
        prepare()
        val token=++generation
        worker.execute {
            if(!current(token)) return@execute
            val startedAt=SystemClock.uptimeMillis()
            var playbackStarted=false
            var success=false
            try {
                val wav=SpeechPcm.read(file)
                if(current(token)) {
                    val convertStart=SystemClock.uptimeMillis()
                    val rendered=SpeechPcm.to48kStereo(wav)
                    // The driver may stutter in the first 100ms after play().
                    // Buffer a silent run-up before the syllable instead of
                    // restarting the phrase or repeatedly releasing AudioTrack.
                    val data=SpeechPcm.withStartupGuard(rendered)
                    val conversionMs=SystemClock.uptimeMillis()-convertStart
                    Log.i(tag,"PCM sourceHz="+wav.sampleRate+
                        " channels="+wav.channels+
                        " bytes="+data.size+" startupSilenceMs="+
                        SpeechPcm.STARTUP_SILENCE_MS+" conversionMs="+conversionMs)
                    if(data.isNotEmpty() && current(token)) {
                        success=tryOutput(data,token,stream=true) {playbackStarted=true}
                        if(!success && TabletPcmPolicy.canFallbackAfterFailure(
                                playbackStarted,current(token))) {
                            Log.w(tag,"Stream never started; trying static PCM fallback")
                            success=tryOutput(data,token,stream=false) {playbackStarted=true}
                        }
                    }
                }
            } catch(e:Exception) {
                Log.w(tag,"PCM decode or output failed; started="+playbackStarted,e)
            } finally {
                if(current(token)) {
                    Log.i(tag,"PCM request="+token+" completed="+success+
                        " started="+playbackStarted+
                        " elapsedMs="+(SystemClock.uptimeMillis()-startedAt))
                    main.post {
                        if(current(token))
                            onFinished(SpeechPlaybackResult(success,playbackStarted))
                    }
                }
            }
        }
    }

    private fun current(token:Int)=!closed && generation==token

    private fun tryOutput(data:ByteArray,token:Int,stream:Boolean,
                          onStarted:()->Unit):Boolean =
        runCatching { if(stream) playBuffered(data,token,onStarted)
            else playStatic(data,token,onStarted)
        }.onFailure { if(current(token)) Log.e(tag,
            "PCM backend failed, stream="+stream,it) }.getOrDefault(false)

    /**
     * Feed arbitrary-length TTS speech via nonblocking PCM chunks so a stalled
     * Android audio mixer cannot freeze the only speech worker.
     */
    private fun playBuffered(data:ByteArray,token:Int,onStarted:()->Unit):Boolean {
        if(!current(token)) return false
        val track=sharedTrack()
        val bufferBytes=track.bufferSizeInFrames*TabletPcmPolicy.FRAME_BYTES
        val prefill=TabletPcmPolicy.prefillBytes(bufferBytes,data.size)
        val initialUnderruns=track.underrunCount
        var broken=false
        var written=0
        val start=SystemClock.uptimeMillis()
        try {
            track.setVolume(1f)
            // Bound prefill time instead of risking an indefinite blocking call.
            val fillDeadline=SystemClock.uptimeMillis()+350L
            while(written<prefill && current(token)){
                val size=minOf(32_768,prefill-written)
                val n=track.write(data,written,size,AudioTrack.WRITE_NON_BLOCKING)
                check(n>=0) {"PCM prefill failed: "+n}
                written+=n
                if(n==0) {
                    if(SystemClock.uptimeMillis()>=fillDeadline) break
                    Thread.sleep(5L)
                }
            }
            if(!current(token)) return false
            check(written>0) {"PCM prefill wrote no frames"}
            track.play()
            onStarted()
            val playedAt=SystemClock.uptimeMillis()
            var lastProgressAt=playedAt
            val progress=SpeechPlaybackHead(track.playbackHeadPosition.toLong())
            val frames=data.size/TabletPcmPolicy.FRAME_BYTES.toLong()
            val deadline=playedAt+frames*1000L/SpeechPcm.OUTPUT_HZ+4000L
            while(current(token)){
                val now=SystemClock.uptimeMillis()
                if(progress.observe(track.playbackHeadPosition.toLong())) {
                    lastProgressAt=now
                }
                if(written<data.size) {
                    val chunk=minOf(16_384,data.size-written)
                    val n=track.write(data,written,chunk,AudioTrack.WRITE_NON_BLOCKING)
                    check(n>=0) {"PCM nonblocking write failed: "+n}
                    written+=n
                }
                if(written==data.size &&
                    progress.playedFrames >= (frames-240L).coerceAtLeast(0L)){
                    Log.i(tag,"PCM completed request="+token+" playedMs="+(now-playedAt)+
                        " totalMs="+(now-start)+" durationMs="+
                        (frames*1000L/SpeechPcm.OUTPUT_HZ)+" bytes="+data.size+
                        " underruns="+(track.underrunCount-initialUnderruns)+
                        " counterResets="+progress.counterResets)
                    return true
                }
                if(now-lastProgressAt>1500L || now>deadline){
                    broken=true
                    Log.e(tag,"PCM watchdog request="+token+
                        " written="+written+"/"+data.size+
                        " playedFrames="+progress.playedFrames+"/"+frames+
                        " idleMs="+(now-lastProgressAt)+
                        " underruns="+(track.underrunCount-initialUnderruns)+
                        " counterResets="+progress.counterResets)
                    return false
                }
                Thread.sleep(10L)
            }
            return false
        } catch(e:Exception) {
            broken=true
            throw e
        } finally {
            val underruns=track.underrunCount-initialUnderruns
            if(underruns>0) Log.w(tag,
                "PCM UNDERRUN count="+underruns+
                " bufferBytes="+bufferBytes+" prefillBytes="+prefill+
                " utteranceBytes="+data.size)
            if(!current(token) && track.playState==AudioTrack.PLAYSTATE_PLAYING){
                runCatching {track.setVolume(.35f);Thread.sleep(10L)}
            }
            // Discard unfinished words before stopping the reused stream.
            val paused=runCatching {
                if(track.playState==AudioTrack.PLAYSTATE_PLAYING) track.pause()
            }.isSuccess
            val flushed=runCatching {track.flush()}.isSuccess
            val stopped=runCatching {track.stop()}.isSuccess
            if(broken || !paused || !flushed || !stopped || closed) releaseShared()
        }
    }

    /** Fallback is allowed only when stream playback did NOT start. */
    private fun playStatic(data:ByteArray,token:Int,onStarted:()->Unit):Boolean {
        if(!current(token)) return false
        var track:AudioTrack?=null
        try {
            track=AudioTrack.Builder()
                .setAudioAttributes(attributes())
                .setAudioFormat(AudioFormat.Builder()
                    .setSampleRate(SpeechPcm.OUTPUT_HZ)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(data.size)
                .build()
            check(track.state==AudioTrack.STATE_INITIALIZED)
            var written=0
            while(written<data.size && current(token)){
                val n=track.write(data,written,data.size-written,AudioTrack.WRITE_BLOCKING)
                check(n>0) {"Static PCM write failed: "+n}
                written+=n
            }
            if(!current(token)) return false
            track.setVolume(1f)
            track.play()
            onStarted()
            val frames=data.size/TabletPcmPolicy.FRAME_BYTES.toLong()
            val deadline=SystemClock.uptimeMillis()+
                frames*1000L/SpeechPcm.OUTPUT_HZ+2500L
            while(current(token) && SystemClock.uptimeMillis()<deadline){
                val head=track.playbackHeadPosition.toLong() and 0xffffffffL
                if(head >= (frames-240L).coerceAtLeast(0L)) return true
                Thread.sleep(20L)
            }
            return false
        } finally {
            if(!current(token) && track?.playState==AudioTrack.PLAYSTATE_PLAYING){
                runCatching {track.setVolume(.35f);Thread.sleep(10L)}
            }
            runCatching {track?.stop()}
            runCatching {track?.release()}
        }
    }

    fun stop(){generation++}

    /** Switching to system TTS frees the stream, app close always releases it. */
    fun releaseOutput(){
        stop()
        if(!closed) worker.execute {releaseShared();warmupQueued.set(false)}
    }

    fun close(){
        if(closed) return
        closed=true
        generation++
        worker.execute {releaseShared()}
        worker.shutdown()
    }
}
