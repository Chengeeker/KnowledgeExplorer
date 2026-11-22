package com.game.knowledge

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.media.audiofx.LoudnessEnhancer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.io.File

/** Parent recordings override installed Android TTS; no bundled speech model. */
class CantoneseSpeaker(context:Context) {
    private val app=context.applicationContext
    private val prefs=app.getSharedPreferences("cantonese_learning",Context.MODE_PRIVATE)
    internal val system=CantoneseSystemTts(app)
    internal val http=CantoneseHttpTts(app)
    init {
        // One-time cleanup after removing the v0.1.24 LAN server experiment.
        prefs.edit().remove("lanTtsEnabled").remove("lanTtsHost")
            .remove("lanTtsPort").remove("lanTtsPairingKey").apply()
        runCatching { File(app.filesDir,"cantonese_lan_cache").deleteRecursively() }
    }
    private val folder=File(app.filesDir,"cantonese_samples").apply {mkdirs()}
    private var player:MediaPlayer?=null
    private var loudness:LoudnessEnhancer?=null
    private var closed=false
    private var playbackEpoch=0L
    var onSpeakingChanged:((Boolean)->Unit)?=null
    init {
        system.onSpeakingChanged={ speaking -> onSpeakingChanged?.invoke(speaking) }
        // Delete previous model files asynchronously, without touching parent recordings.
        Thread {
            runCatching { File(app.filesDir,"cosyvoice3-mnn").deleteRecursively() }
        }.start()
    }

    val ready:Boolean get()=system.ready || http.available
    val message:String get()=if(http.enabled)http.status else system.status
    var volumePercent by mutableIntStateOf(
        CantoneseVolumePolicy.clamp(prefs.getInt("cantoneseVoiceVolume",CantoneseVolumePolicy.DEFAULT_PERCENT)))
        private set

    fun updateVolumePercent(value:Int) {
        val clamped=CantoneseVolumePolicy.clamp(value)
        if(volumePercent==clamped) return
        volumePercent=clamped
        prefs.edit().putInt("cantoneseVoiceVolume",clamped).apply()
        updatePlayingVolume()
    }
    private fun updatePlayingVolume() {
        val active=player ?: return
        val gain=CantoneseVolumePolicy.playerGain(volumePercent)
        runCatching {active.setVolume(gain,gain)}
        val boost=CantoneseVolumePolicy.boostMillibels(volumePercent)
        if(boost==0) {
            runCatching {loudness?.enabled=false}
            return
        }
        val effect=loudness ?: runCatching {
            LoudnessEnhancer(active.audioSessionId).also {loudness=it}
        }.getOrNull()
        runCatching {
            effect?.setTargetGain(boost)
            if(effect!=null) effect.enabled=true
        }
    }
    fun sampleFile(id:String):File {
        require(id.matches(Regex("[a-z0-9_]+")))
        return File(folder,id+".m4a")
    }
    fun hasSample(word:CantoneseWord):Boolean=sampleFile(word.id).isFile
    fun hasAudio(word:CantoneseWord):Boolean=hasSample(word) || system.ready || http.available
    fun speakText(text:String,slow:Boolean=false):Boolean {
        if(closed || text.isBlank()) return false
        stop()
        val clean=text.trim().take(120)
        if(http.available) {
            val epoch=playbackEpoch
            http.generate(clean,slow) {file->
                if(closed || epoch!=playbackEpoch)return@generate
                if(file!=null)playFile(file)
                else if(system.ready)system.speak(clean,slow,volumePercent)
            }
            return true
        }
        return system.speak(clean,slow,volumePercent)
    }
    fun previewOnline(text:String,slow:Boolean=false) {
        if(closed || !http.available || text.isBlank()) return
        stop()
        val epoch=playbackEpoch
        http.generate(text.trim().take(120),slow) {file->
            if(!closed && epoch==playbackEpoch && file!=null) playFile(file)
        }
    }
    fun speak(word:CantoneseWord,slow:Boolean=false,phrase:String?=null):Boolean {
        if(closed) return false
        val sample=sampleFile(word.id)
        if(sample.isFile) return playFile(sample,slow)
        val text=phrase ?: word.traditional
        return speakText(text,slow)
    }
    fun playFile(file:File,slow:Boolean=false):Boolean {
        if(!file.isFile || closed) return false
        stop()
        return runCatching {
            player=MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build())
                setDataSource(file.absolutePath)
                setOnCompletionListener { stop() }
                setOnErrorListener { _,_,_ -> stop();true }
                prepare()
                if(slow) runCatching { playbackParams=PlaybackParams().setSpeed(.8f) }
                player=this
                updatePlayingVolume()
                start()
            }
            onSpeakingChanged?.invoke(true)
            true
        }.getOrElse {stop();false}
    }
    fun stop() {
        system.stop()
        playbackEpoch++
        val effect=loudness
        loudness=null
        if(effect!=null) runCatching {effect.release()}
        val p=player
        player=null
        if(p!=null) runCatching {p.release()}
        onSpeakingChanged?.invoke(false)
    }
    fun close() {
        if(closed) return
        stop()
        closed=true
        system.close()
        http.close()
        onSpeakingChanged=null
    }
}

/**
 * Explicit microphone operations only. All clips remain in application-private
 * storage. Practice voice is a temporary local file and can be deleted.
 */
class CantoneseVoiceRecorder(private val context:Context) {
    private var recorder:MediaRecorder?=null
    private var destination:File?=null
    private var pendingFile:File?=null
    var recordingId:String?=null
        private set

    fun isRecording():Boolean = recorder!=null
    fun practiceFile():File = File(context.cacheDir,"cantonese_practice.m4a")
    @Suppress("DEPRECATION")
    fun start(id:String,file:File):Boolean {
        if(recorder!=null) return false
        require(id.matches(Regex("[a-z0-9_]+")))
        file.parentFile?.mkdirs()
        // Never erase a verified parent demo before its replacement is complete.
        val temp=File(file.parentFile,file.name+".recording")
        runCatching {temp.delete()}
        var next:MediaRecorder?=null
        return runCatching {
            next=MediaRecorder()
            val prepared=next
            prepared.setAudioSource(MediaRecorder.AudioSource.MIC)
            prepared.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            prepared.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            prepared.setAudioEncodingBitRate(64000)
            prepared.setAudioSamplingRate(24000)
            prepared.setOutputFile(temp.absolutePath)
            prepared.prepare()
            prepared.start()
            recorder=prepared
            recordingId=id
            destination=file
            pendingFile=temp
            true
        }.getOrElse {
            runCatching {next?.release()}
            recorder=null
            recordingId=null
            destination=null
            pendingFile=null
            runCatching {temp.delete()}
            false
        }
    }
    fun stop(keep:Boolean=true):File? {
        val active=recorder?:return null
        val file=destination
        val temp=pendingFile
        recorder=null
        destination=null
        pendingFile=null
        recordingId=null
        val success=runCatching {active.stop()}.isSuccess
        runCatching {active.release()}
        if(!keep || !success || file==null || temp==null || temp.length()<600L) {
            runCatching {temp?.delete()}
            return null
        }
        // The previous sample remains intact if stop fails or the child cancels.
        val moved=runCatching {
            java.nio.file.Files.move(temp.toPath(),file.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            true
        }.getOrDefault(false)
        if(!moved) {runCatching {temp.delete()};return null}
        return file
    }
    fun close() {
        stop(keep=false)
        practiceFile().delete()
    }
}
