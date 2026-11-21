package com.game.knowledge

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

internal data class InstalledTtsEngine(val name: String, val label: String)
internal data class InstalledTtsVoice(val name: String, val locale: String, val network: Boolean)

/** Android-native TTS only; no embedded speech model, server or API keys. */
internal class CantoneseSystemTts(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("cantonese_learning", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var active: TextToSpeech? = null
    private var generation = 0
    private var currentUtterance: String? = null
    private var counter = 0L
    private var closed = false

    var ready by mutableStateOf(false)
        private set
    var status by mutableStateOf("正在查找手机已安装的朗读引擎……")
        private set
    var engines by mutableStateOf<List<InstalledTtsEngine>>(emptyList())
        private set
    var voices by mutableStateOf<List<InstalledTtsVoice>>(emptyList())
        private set
    var enginePackage by mutableStateOf(prefs.getString("cantoneseSystemTtsEngine", "").orEmpty())
        private set
    var voiceName by mutableStateOf(prefs.getString("cantoneseSystemTtsVoice", "").orEmpty())
        private set
    var onSpeakingChanged: ((Boolean) -> Unit)? = null

    init { initEngine(enginePackage) }

    private fun cantonese(tag: String): Boolean {
        val normalized = tag.lowercase(Locale.ROOT).replace('_', '-')
        return normalized == "yue" || normalized.startsWith("yue-") ||
            normalized == "zh-hk" || normalized.startsWith("zh-hk-") ||
            normalized == "zh-mo" || normalized.startsWith("zh-mo-")
    }

    fun refresh() = initEngine(enginePackage)
    fun chooseEngine(name: String) {
        if (enginePackage == name && ready) return
        enginePackage = name
        voiceName = ""
        prefs.edit().putString("cantoneseSystemTtsEngine", name)
            .remove("cantoneseSystemTtsVoice").apply()
        initEngine(name)
    }

    fun chooseVoice(name: String) {
        val engine = active ?: return
        val selected = engine.voices?.firstOrNull { it.name == name } ?: return
        if (engine.setVoice(selected) == TextToSpeech.SUCCESS) {
            voiceName = name
            prefs.edit().putString("cantoneseSystemTtsVoice", name).apply()
            ready = true
            status = if (cantonese(selected.locale.toLanguageTag()))
                "粤语音色已就绪：" + name
            else "音色已就绪，但语言是 " + selected.locale.toLanguageTag() + "，可能读成普通话"
        } else {
            status = "音色启用失败，请选择其他音色"
        }
    }

    private fun initEngine(name: String) {
        val epoch = ++generation
        stop()
        active?.shutdown()
        active = null
        ready = false
        status = "正在初始化系统语音引擎……"
        voices = emptyList()
        val listener = TextToSpeech.OnInitListener { code ->
            handler.post {
                if (closed || epoch != generation) return@post
                val engine = active
                if (code != TextToSpeech.SUCCESS || engine == null) {
                    ready = false
                    status = "朗读引擎初始化失败，请换其他已安装的语音引擎"
                    return@post
                }
                engines = engine.engines.orEmpty().map {
                    InstalledTtsEngine(it.name, it.label?.toString().orEmpty().ifBlank { it.name })
                }.distinctBy { it.name }.sortedBy { it.label }
                voices = engine.voices.orEmpty().map {
                    InstalledTtsVoice(it.name, it.locale.toLanguageTag(), it.isNetworkConnectionRequired)
                }.distinctBy { it.name }.sortedWith(
                    compareBy<InstalledTtsVoice> { !cantonese(it.locale) }
                        .thenBy { it.locale }.thenBy { it.name })
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String) { changeSpeaking(epoch, id, true) }
                    override fun onDone(id: String) { changeSpeaking(epoch, id, false) }
                    @Deprecated("Deprecated Android TTS callback")
                    override fun onError(id: String) { changeSpeaking(epoch, id, false) }
                    override fun onError(id: String, errorCode: Int) { changeSpeaking(epoch, id, false) }
                })
                val stored = engine.voices?.firstOrNull { it.name == voiceName }
                val automatic = engine.voices?.firstOrNull {
                    cantonese(it.locale.toLanguageTag())
                }
                val ok = when {
                    stored != null -> engine.setVoice(stored) == TextToSpeech.SUCCESS
                    automatic != null -> engine.setVoice(automatic) == TextToSpeech.SUCCESS
                    else -> engine.setLanguage(Locale.forLanguageTag("zh-HK")) >= TextToSpeech.LANG_AVAILABLE
                }
                ready = ok
                status = when {
                    !ok -> "未找到可用的粤语音色；可切换引擎或手动选择音色"
                    stored != null && !cantonese(stored.locale.toLanguageTag()) ->
                        "已选音色不是粤语，可能读成普通话；请先试听"
                    else -> "系统 TTS 已就绪；请试听确认粤语发音"
                }
            }
        }
        active = try {
            if (name.isBlank()) TextToSpeech(app, listener)
            else TextToSpeech(app, listener, name)
        } catch (ex: Exception) {
            status = "启动系统 TTS 失败：" + ex.message
            null
        }
    }

    private fun changeSpeaking(epoch: Int, id: String, speaking: Boolean) {
        handler.post {
            if (closed || epoch != generation || currentUtterance != id) return@post
            if (!speaking) currentUtterance = null
            onSpeakingChanged?.invoke(speaking)
        }
    }

    fun speak(text: String, slow: Boolean, volumePercent: Int): Boolean {
        if (!ready || closed || text.isBlank()) return false
        val engine = active ?: return false
        stop()
        val id = "canto_" + (++counter)
        currentUtterance = id
        engine.setSpeechRate(if (slow) 0.8f else 1f)
        val arguments = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, (volumePercent / 100f).coerceIn(0f, 1f))
        }
        val result = runCatching {
            engine.speak(text.take(120), TextToSpeech.QUEUE_FLUSH, arguments, id)
        }.getOrDefault(TextToSpeech.ERROR)
        if (result == TextToSpeech.ERROR) {
            currentUtterance = null
            status = "语音引擎无法朗读；可尝试切换音色或朗读引擎"
            return false
        }
        return true
    }

    fun stop() {
        currentUtterance = null
        active?.stop()
        onSpeakingChanged?.invoke(false)
    }

    fun close() {
        if (closed) return
        stop()
        closed = true
        ++generation
        active?.shutdown()
        active = null
        onSpeakingChanged = null
    }
}
