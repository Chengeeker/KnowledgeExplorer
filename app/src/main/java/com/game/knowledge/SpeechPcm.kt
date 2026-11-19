package com.game.knowledge

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.roundToInt

/**
 * Read the PCM16 RIFF/WAVE produced by Android TTS and resample the entire
 * utterance to 48kHz interleaved stereo PCM before starting AudioTrack.
 * This follows Math Planet's 48kHz software-PCM path rather than MediaPlayer.
 */
internal object SpeechPcm {
    const val OUTPUT_HZ = 48_000
    const val OUTPUT_FRAME_BYTES = 4
    // Some Android tablet audio routes briefly stutter just after play(). Start
    // the output with silence so that initial mixer/driver settling occurs
    // before the first Mandarin syllable. Speech itself is not discarded.
    const val STARTUP_SILENCE_MS = 120
    const val STARTUP_FADE_MS = 25
    data class Wave(val channels: Int, val sampleRate: Int, val samples: ByteArray)

    /**
     * Adds 120ms of silent PCM16 stereo before the TTS samples; fade in the
     * first 25ms of speech to soften any cold-start transient. The entire
     * protected utterance is still prebuffered before AudioTrack.play().
     *
     * Runs on the serialized audio worker, never on the Compose UI thread.
     */
    fun withStartupGuard(
        pcm48kStereo: ByteArray,
        silenceMs: Int = STARTUP_SILENCE_MS,
        fadeMs: Int = STARTUP_FADE_MS
    ): ByteArray {
        require(pcm48kStereo.size % OUTPUT_FRAME_BYTES == 0)
        require(silenceMs in 0..300 && fadeMs in 0..100)
        if(pcm48kStereo.isEmpty()) return pcm48kStereo
        val silentFrames=OUTPUT_HZ*silenceMs/1000
        val silentBytes=silentFrames*OUTPUT_FRAME_BYTES
        require(pcm48kStereo.size <= Int.MAX_VALUE-silentBytes)
        val result=ByteArray(silentBytes+pcm48kStereo.size)
        pcm48kStereo.copyInto(result,destinationOffset=silentBytes)
        val speechFrames=pcm48kStereo.size/OUTPUT_FRAME_BYTES
        val rampFrames=minOf(speechFrames,OUTPUT_HZ*fadeMs/1000)
        for(frame in 0 until rampFrames){
            val numerator=frame
            for(channel in 0..1){
                val pos=silentBytes+frame*OUTPUT_FRAME_BYTES+channel*2
                val sample=((result[pos].toInt() and 0xff) or
                    ((result[pos+1].toInt() and 0xff) shl 8)).toShort().toInt()
                val scaled=sample*numerator/rampFrames.coerceAtLeast(1)
                result[pos]=(scaled and 0xff).toByte()
                result[pos+1]=((scaled shr 8) and 0xff).toByte()
            }
        }
        return result
    }

    fun read(file: File): Wave {
        RandomAccessFile(file, "r").use { inFile ->
            require(inFile.length() in 45L..15_000_000L) { "Bad TTS WAV file size" }
            val size=inFile.length()
            val riff=ByteArray(12)
            inFile.readFully(riff)
            require(ascii(riff,0,4)=="RIFF" && ascii(riff,8,4)=="WAVE")
            var rate=0
            var channels=0
            var fmtFound=false
            var data:ByteArray?=null
            var pos=12L
            while(pos+8<=size) {
                inFile.seek(pos)
                val head=ByteArray(8)
                inFile.readFully(head)
                val id=ascii(head,0,4)
                val len=uint32(head,4)
                val dataPos=pos+8L
                require(len>=0 && dataPos+len<=size) {"Invalid WAV chunk"}
                when(id) {
                    "fmt " -> {
                        require(len>=16 && len<=256)
                        val bytes=ByteArray(len.toInt())
                        inFile.readFully(bytes)
                        val format=uint16(bytes,0)
                        channels=uint16(bytes,2)
                        rate=uint32(bytes,4).toInt()
                        val bits=uint16(bytes,14)
                        // Most installed TTS engines write little-endian PCM16 WAV.
                        require(format==1 && channels in 1..2 && bits==16 &&
                            rate in 8000..96000) {
                            "Unsupported TTS WAV format: type=$format ch=$channels rate=$rate bits=$bits"
                        }
                        fmtFound=true
                    }
                    "data" -> {
                        require(len in 2..12_000_000)
                        val bytes=ByteArray(len.toInt())
                        inFile.readFully(bytes)
                        data=bytes
                    }
                }
                pos=dataPos+len+(len and 1L)
                if(fmtFound && data!=null) break
            }
            val samples=requireNotNull(data) {"TTS WAV has no data"}
            require(fmtFound && samples.size % (channels*2)==0)
            return Wave(channels,rate,samples)
        }
    }

    fun to48kStereo(wave:Wave):ByteArray {
        require(wave.channels in 1..2 && wave.sampleRate in 8000..96000)
        val inFrames=wave.samples.size/(wave.channels*2)
        if(inFrames<=0) return byteArrayOf()
        val outFrames=(inFrames.toLong()*OUTPUT_HZ/wave.sampleRate).coerceAtLeast(1)
        require(outFrames<12_000_000/OUTPUT_FRAME_BYTES)
        val out=ByteArray(outFrames.toInt()*OUTPUT_FRAME_BYTES)
        val endFade=minOf(300,outFrames.toInt()/2)
        for(frame in 0 until outFrames.toInt()){
            val sourcePos=frame.toDouble()*wave.sampleRate/OUTPUT_HZ
            val i=sourcePos.toInt().coerceIn(0,inFrames-1)
            val j=minOf(i+1,inFrames-1)
            val fraction=(sourcePos-i).toFloat().coerceIn(0f,1f)
            val fade=if(endFade==0) 1f else minOf(1f,
                frame.toFloat()/endFade,
                (outFrames-1-frame).toFloat()/endFade)
            for(channel in 0..1){
                val sourceChannel=channel.coerceAtMost(wave.channels-1)
                val a=sample(wave.samples,i*wave.channels+sourceChannel)
                val b=sample(wave.samples,j*wave.channels+sourceChannel)
                val scaled=((a+(b-a)*fraction)*.78f*fade).roundToInt()
                    .coerceIn(-32768,32767)
                val offset=frame*OUTPUT_FRAME_BYTES+channel*2
                out[offset]=(scaled and 0xff).toByte()
                out[offset+1]=((scaled shr 8) and 0xff).toByte()
            }
        }
        return out
    }

    private fun sample(data:ByteArray,index:Int):Int {
        val pos=index*2
        return (((data[pos].toInt() and 0xff) or
            ((data[pos+1].toInt() and 0xff) shl 8)).toShort()).toInt()
    }
    private fun ascii(data:ByteArray,at:Int,n:Int):String=
        data.copyOfRange(at,at+n).toString(Charsets.US_ASCII)
    private fun uint16(data:ByteArray,at:Int):Int=
        (data[at].toInt() and 0xff) or ((data[at+1].toInt() and 0xff) shl 8)
    private fun uint32(data:ByteArray,at:Int):Long=
        (uint16(data,at).toLong() or (uint16(data,at+2).toLong() shl 16))
}
