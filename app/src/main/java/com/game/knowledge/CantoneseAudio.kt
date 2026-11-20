package com.game.knowledge

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.Locale

/**
 * Cantonese voice only. Never fall back to Mandarin: if no appropriate engine
 * is installed, parents can record a pronunciation that stays on this device.
 */
class CantoneseSpeaker(context:Context) {
    private val app=context.applicationContext
    private val main=Handler(Looper.getMainLooper())
    private val prefs=app.getSharedPreferences("cantonese_learning",Context.MODE_PRIVATE)
    private val folder=File(app.filesDir,"cantonese_samples").apply {mkdirs()}
    // Shipped PCM audio stays offline; Android AssetManager lists archive
    // contents without needing a network request or any speech package.
    private val bundledIds: Set<String> by lazy {
        runCatching {
            app.assets.list("cantonese_audio").orEmpty()
                .filter {it.endsWith(".wav")}
                .map {it.removeSuffix(".wav")}
                .toSet()
        }.getOrDefault(emptySet())
    }
    private val assetCache=File(app.cacheDir,"cantonese_builtin_v13").apply {mkdirs()}
    fun builtInCount():Int=bundledIds.size
    fun hasBuiltIn(word:CantoneseWord):Boolean=word.id in bundledIds
    private fun bundledFile(id:String):File? {
        if(id !in bundledIds || !id.matches(Regex("[a-z0-9_]+"))) return null
        val output=File(assetCache,"$id.wav")
        if(output.isFile && output.length()>3000L) return output
        val temp=File(assetCache,"$id.part")
        return runCatching {
            app.assets.open("cantonese_audio/$id.wav").use { input ->
                temp.outputStream().use {output -> input.copyTo(output)}
            }
            if(temp.length()<3000L) throw IllegalStateException("Packaged audio too short")
            if(!temp.renameTo(output)) {
                temp.copyTo(output,overwrite=true)
                temp.delete()
            }
            output
        }.getOrElse {
            temp.delete()
            null
        }
    }
    private var engine:TextToSpeech?=null
    private var player:MediaPlayer?=null
    private var loudness:LoudnessEnhancer?=null
    var preferSystemTts by mutableStateOf(prefs.getBoolean("preferSystemTts",false))
        private set

    fun chooseSystemTts(prefer:Boolean) {
        preferSystemTts=prefer
        prefs.edit().putBoolean("preferSystemTts",prefer).apply()
    }
    var volumePercent by androidx.compose.runtime.mutableIntStateOf(
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
    private var activeToken:String?=null
    private var sequence=0
    private var closed=false
    private var localeLabel:String?=null
    var onSpeakingChanged:((Boolean)->Unit)?=null
    var ready by mutableStateOf(false)
        private set
    var previewRequired by mutableStateOf(false)
        private set
    var message by mutableStateOf("正在检查粤语语音…")
        private set

    init {
        engine=TextToSpeech(app) { status ->
            main.post {
                if(closed) return@post
                val tts=engine
                if(status!=TextToSpeech.SUCCESS || tts==null) {
                    message="已内置 ${builtInCount()} 条粤语发音，无需语音引擎或家长录音。系统 TTS 为可选备用。"
                    return@post
                }
                val locales=listOf(
                    Locale.forLanguageTag("yue-HK") to "粤语 yue-HK",
                    Locale.forLanguageTag("zh-HK") to "香港中文 zh-HK（请试听核对口音）")
                val selected=locales.firstOrNull { (locale,_) ->
                    val result=runCatching {tts.isLanguageAvailable(locale)}
                        .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                    if(result<TextToSpeech.LANG_AVAILABLE) false
                    else {
                        val setResult=runCatching {tts.setLanguage(locale)}
                            .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                        setResult>=TextToSpeech.LANG_AVAILABLE &&
                            tts.voice?.let { candidate ->
                                !candidate.isNetworkConnectionRequired &&
                                    (candidate.locale.country.equals("HK",true) ||
                                        candidate.locale.language.equals("yue",true)) &&
                                    (locale.language!="yue" ||
                                        candidate.locale.language.equals("yue",true))
                            } == true
                    }
                }
                localeLabel=selected?.second
                val hkOnly=selected?.first?.language=="zh"
                previewRequired=hkOnly && !prefs.getBoolean("hkVoiceConfirmed",false) &&
                    !prefs.getBoolean("hkVoiceRejected",false)
                ready=selected!=null && (!hkOnly ||
                    prefs.getBoolean("hkVoiceConfirmed",false))
                message="已内置 ${builtInCount()} 条离线粤语发音。"+when {
                    ready -> "可选择系统 ${selected!!.second} 音色。"
                    previewRequired -> "发现本地香港中文音色。请家长试听，确认发音确实是粤语后才能开启听音闯关。"
                    selected!=null -> "香港中文音色未启用。可使用家长示范录音。"
                    else -> "未找到离线粤语音色。不会改用普通话，请安装粤语语音包或录入家长示范音。"
                }
                tts.setOnUtteranceProgressListener(object:UtteranceProgressListener() {
                    override fun onStart(utteranceId:String?) {
                        main.post {if(!closed && activeToken==utteranceId) onSpeakingChanged?.invoke(true)}
                    }
                    override fun onDone(utteranceId:String?) {endIfCurrent(utteranceId)}
                    @Deprecated("Old Android callback")
                    override fun onError(utteranceId:String?) {endIfCurrent(utteranceId)}
                    override fun onError(utteranceId:String?,errorCode:Int) {endIfCurrent(utteranceId)}
                    private fun endIfCurrent(id:String?) {
                        main.post {
                            if(!closed && activeToken==id) {
                                activeToken=null
                                onSpeakingChanged?.invoke(false)
                            }
                        }
                    }
                })
            }
        }
    }

    fun auditionHK():Boolean {
        if(!previewRequired || closed) return false
        stop()
        val tts=engine?:return false
        val current=tts.voice?:return false
        if(current.isNetworkConnectionRequired ||
            !current.locale.country.equals("HK",true)) return false
        val id="canto-preview-${++sequence}"
        activeToken=id
        tts.setSpeechRate(.88f)
        val params=Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,CantoneseVolumePolicy.ttsGain(volumePercent))
        }
        return tts.speak("早晨，唔該，多謝。",TextToSpeech.QUEUE_FLUSH,params,id)==TextToSpeech.SUCCESS
    }
    fun confirmHK() {
        if(!previewRequired) return
        prefs.edit().putBoolean("hkVoiceConfirmed",true).putBoolean("hkVoiceRejected",false).apply()
        previewRequired=false
        ready=true
        message="家长已确认香港中文音色为粤语。仍可录制标准广州话示范音。"
    }
    fun rejectHK() {
        if(!previewRequired) return
        stop()
        prefs.edit().putBoolean("hkVoiceRejected",true).apply()
        previewRequired=false
        ready=false
        message="已停用这个香港中文音色。不会播放普通话，可用家长录音。"
    }

    fun hasSample(word:CantoneseWord):Boolean = sampleFile(word.id).isFile
    fun hasAudio(word:CantoneseWord):Boolean =
        hasSample(word) || hasBuiltIn(word) || ready
    fun sampleFile(id:String):File {
        require(id.matches(Regex("[a-z0-9_]+")))
        return File(folder,"$id.m4a")
    }
    fun speak(word:CantoneseWord,slow:Boolean=false,phrase:String?=null):Boolean {
        if(closed) return false
        val sample=sampleFile(word.id)
        if(sample.isFile) return playFile(sample,slow)
        // Optional native Yue voice may sound more natural on certain devices.
        // If none is approved/installed, always use the built-in offline sample.
        if(preferSystemTts && ready && speakText(phrase ?: word.traditional,slow)) return true
        val orderId=if(phrase!=null) "order_${word.id}" else null
        val builtIn=orderId?.let(::bundledFile) ?: bundledFile(word.id)
        if(builtIn!=null) return playFile(builtIn,slow)
        return speakText(phrase ?: word.traditional,slow)
    }
    fun speakText(text:String,slow:Boolean=false):Boolean {
        if(closed || !ready) return false
        stop()
        val token="canto-${++sequence}"
        activeToken=token
        val tts=engine ?: return false
        tts.setSpeechRate(if(slow) .72f else .88f)
        // Guard against the engine choosing a Mandarin voice after switching.
        val voice=tts.voice
        if(voice==null || voice.isNetworkConnectionRequired ||
                (!voice.locale.country.equals("HK",true) &&
                    !voice.locale.language.equals("yue",true))) {
            activeToken=null
            ready=false
            message="检测到语音引擎切换了语言；已停止播放，请录入粤语示范音"
            return false
        }
        val params=Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,CantoneseVolumePolicy.ttsGain(volumePercent))
        }
        val result=tts.speak(text,TextToSpeech.QUEUE_FLUSH,params,token)
        if(result!=TextToSpeech.SUCCESS) {
            activeToken=null
            message="粤语语音播放失败，可以尝试重新安装语音包"
            return false
        }
        return true
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
                if(slow) runCatching {
                    playbackParams=PlaybackParams().setSpeed(.8f)
                }
                // Initial playback uses the same volume that the UI slider controls.
                // Configure on a prepared player with a valid audio session id.
                player=this
                updatePlayingVolume()
                start()
            }
            onSpeakingChanged?.invoke(true)
            true
        }.getOrElse {
            stop()
            false
        }
    }
    fun stop() {
        activeToken=null
        runCatching {engine?.stop()}
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
        runCatching {engine?.shutdown()}
        engine=null
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
