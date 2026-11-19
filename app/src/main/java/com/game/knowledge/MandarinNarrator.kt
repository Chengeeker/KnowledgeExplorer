package com.game.knowledge

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/**
 * Adaptive Chinese narration:
 * - Tablet default: TTS synthesizes each phrase to a cached local WAV;
 *   a serialized software-PCM player handles 48kHz stereo playback.
 * - Phones and unsupported TTS engines: normal Android TTS speech.
 *
 * Math Planet has a fixed PCM vocabulary, but this game's 1200 different
 * questions need on-device text synthesis before the same PCM output approach.
 */
class MandarinNarrator(private val context:Context) {
    private val main=Handler(Looper.getMainLooper())
    private val pcm=BufferedSpeechPlayer()
    private val cache=File(context.cacheDir,"knowledge_speech").apply {
        mkdirs()
        listFiles()?.filter {it.name.startsWith("tmp_") && it.name.endsWith(".wav")}?.forEach{
            runCatching{it.delete()}
        }
    }
    private val tablet=context.resources.configuration.let {config->
        config.smallestScreenWidthDp>=600 ||
            (config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK)>=
                Configuration.SCREENLAYOUT_SIZE_LARGE
    }
    private var tts:TextToSpeech?=null
    private var activeId:String?=null
    private var activeText:String?=null
    private var activeTemp:File?=null
    private var activeCache:File?=null
    private var closed=false
    private var serial=0
    private var pcmEnabled=tablet

    var onSpeakingChanged:((Boolean)->Unit)?=null
    var ready by mutableStateOf(false)
        private set
    var message by mutableStateOf("正在准备中文朗读")
        private set

    init {
        tts=TextToSpeech(context.applicationContext) {status->
            main.post {
                if(closed) return@post
                if(status!=TextToSpeech.SUCCESS){
                    ready=false
                    message="系统中文语音引擎不可用，请检查系统语音服务"
                } else {
                    val result=runCatching {
                        tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                            ?:TextToSpeech.LANG_NOT_SUPPORTED
                    }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                    ready=result!=TextToSpeech.LANG_MISSING_DATA &&
                        result!=TextToSpeech.LANG_NOT_SUPPORTED
                    tts?.setSpeechRate(.86f)
                    tts?.setPitch(1.03f)
                    tts?.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
                        override fun onStart(utteranceId:String?) {
                            main.post {
                                if(!closed && utteranceId==activeId) onSpeakingChanged?.invoke(true)
                            }
                        }
                        override fun onDone(utteranceId:String?) {
                            main.post {if(!closed) handleDone(utteranceId)}
                        }
                        @Deprecated("Use onError with code")
                        override fun onError(utteranceId:String?) {
                            main.post {if(!closed) handleError(utteranceId)}
                        }
                        override fun onError(utteranceId:String?,errorCode:Int){
                            main.post {if(!closed) handleError(utteranceId)}
                        }
                    })
                    refreshStatus()
                    if(ready && pcmEnabled) pcm.prepare()
                }
            }
        }
    }

    fun setPcmEnabled(enabled:Boolean) {
        if(pcmEnabled==enabled) {
            if(enabled && ready) pcm.prepare()
            return
        }
        stop()
        pcmEnabled=enabled
        if(enabled && ready) pcm.prepare()
        else if(!enabled) pcm.releaseOutput()
        refreshStatus()
    }

    private fun refreshStatus() {
        message=if(!ready) "设备没有可用的中文语音，请在系统中安装中文语音"
        else if(pcmEnabled) "中文朗读：平板 48 kHz PCM 缓冲（不使用媒体播放器）"
        else "中文朗读：系统语音引擎"
    }

    fun say(text:String) {
        if(closed || !ready || text.isBlank()) return
        // Cancel the previous phrase without briefly unducking and restoring
        // background music between back-to-back speech requests.
        activeId=null
        activeText=null
        activeCache=null
        activeTemp=null
        pcm.stop()
        runCatching{tts?.stop()}
        val id="k-${++serial}-${SystemClock.elapsedRealtime()}"
        activeId=id
        activeText=text
        onSpeakingChanged?.invoke(true)
        if(!pcmEnabled){
            speakDirect(text,id)
            return
        }
        val key=MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .take(16).joinToString(""){"%02x".format(it)}
        val destination=File(cache,"$key.wav")
        activeCache=destination
        if(destination.length()>44){
            destination.setLastModified(System.currentTimeMillis())
            playCached(destination,id)
            return
        }
        // TTS synthesis runs in the system TTS service. No audio decoding or
        // file write happens on the Compose/UI thread.
        val temp=File(cache,"tmp_$id.wav")
        activeTemp=temp
        runCatching {
            val result=tts?.synthesizeToFile(text,Bundle(),temp,id)
                ?:TextToSpeech.ERROR
            check(result==TextToSpeech.SUCCESS) {"TTS cannot synthesize to WAV"}
        }.onFailure {
            Log.w("KnowledgeNarrator","PCM synthesis unavailable, switching this phrase to TTS",it)
            temp.delete()
            activeTemp=null
            speakDirect(text,id)
        }
    }

    private fun speakDirect(text:String,id:String) {
        if(id!=activeId) return
        if(pcmEnabled)
            message="PCM 暂不可用，当前语句已自动回退系统朗读；可在设置关闭 PCM"
        val result=runCatching {
            tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,id)
        }.getOrNull()
        if(result!=TextToSpeech.SUCCESS) finish(id)
    }

    private fun handleDone(id:String?) {
        if(id!=activeId) return
        val temp=activeTemp
        if(temp!=null){
            activeTemp=null
            val dest=activeCache
            val file=if(temp.length()>44 && dest!=null && temp.renameTo(dest))
                dest else temp
            if(file.length()<=44){
                file.delete()
                activeText?.let {speakDirect(it,id!!)} ?: finish(id)
            } else {
                playCached(file,id!!)
            }
        } else {
            // Normal Android TTS playback is complete.
            finish(id)
        }
    }

    private fun playCached(file:File,id:String) {
        pcm.play(file){result->
            if(closed || activeId!=id) return@play
            when {
                result.completed -> {
                    message="中文朗读：48 kHz PCM 音轨复用播放正常"
                    if(file.name.startsWith("tmp_")) file.delete()
                    trimCache()
                    finish(id)
                }
                result.started -> {
                    // The buffered track began this sentence; a system TTS
                    // fallback here would start the entire sentence twice.
                    message="PCM 播放中断（已避免重复朗读），请重试或关闭 PCM"
                    Log.w("KnowledgeNarrator",
                        "PCM interrupted after audio started; NOT replaying whole utterance")
                    finish(id)
                }
                else -> {
                    // Decoder or both AudioTrack backends failed BEFORE play().
                    // Safe to let Android TTS read the sentence once.
                    if(file==activeCache) file.delete()
                    else if(file.name.startsWith("tmp_")) file.delete()
                    Log.w("KnowledgeNarrator",
                        "PCM output never started; falling back to system TTS")
                    activeText?.let {speakDirect(it,id)} ?: finish(id)
                }
            }
        }
    }

    private fun handleError(id:String?) {
        if(id!=activeId) return
        val temp=activeTemp
        if(temp!=null) {
            activeTemp=null
            temp.delete()
            activeText?.let {speakDirect(it,id!!)} ?: finish(id)
        } else finish(id)
    }

    private fun finish(id:String?) {
        if(id!=activeId) return
        activeId=null
        activeText=null
        activeCache=null
        activeTemp=null
        onSpeakingChanged?.invoke(false)
    }

    private fun trimCache() {
        // Cache is optional and bounded: 80 phrases at most, around tens of MB.
        val files=cache.listFiles()?.filter {it.name.endsWith(".wav") &&
            !it.name.startsWith("tmp_")}?.sortedByDescending{it.lastModified()} ?:return
        files.drop(80).forEach{runCatching{it.delete()}}
    }

    fun stop() {
        activeId=null
        activeText=null
        activeCache=null
        activeTemp=null
        pcm.stop()
        runCatching {tts?.stop()}
        onSpeakingChanged?.invoke(false)
    }

    fun close() {
        if(closed) return
        stop()
        closed=true
        ready=false
        pcm.close()
        runCatching{tts?.shutdown()}
        tts=null
    }
}
