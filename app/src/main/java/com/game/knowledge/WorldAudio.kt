package com.game.knowledge

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * Original Math Planet music/effects reused in the user's other game.
 * Keeps playback on the UI thread, pauses on app background and ducks under TTS.
 */
internal object WorldMusicLevels {
    fun gain(musicEnabled:Boolean,volumePercent:Int,foreground:Boolean,speaking:Boolean):Float {
        if(!foreground || !musicEnabled) return 0f
        return (.35f * volumePercent.coerceIn(0,100)/100f *
            (if(speaking) .16f else 1f)).coerceIn(0f,1f)
    }
}

class WorldAudio(context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var disposed = false
    private var foreground = false
    private var musicEnabled = true
    private var effectsEnabled = true
    private var volumePercent = 55
    private var narratorSpeaking = false
    private var fadeSerial = 0
    private var currentVolume = 0f
    private var lastClickAt = 0L

    private val player = runCatching {
        MediaPlayer.create(context, R.raw.adventure_loop)?.apply {
            isLooping = true
            setVolume(0f, 0f)
            setOnErrorListener { _, what, extra ->
                Log.w("KnowledgeMusic", "MediaPlayer error $what/$extra")
                true
            }
        }
    }.onFailure { Log.w("KnowledgeMusic", "Background music unavailable", it) }.getOrNull()

    private val soundPool = runCatching {
        SoundPool.Builder().setMaxStreams(2)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build()
    }.getOrNull()
    private val tapId = soundPool?.load(context, R.raw.tap, 1) ?: 0
    private val successId = soundPool?.load(context, R.raw.success, 1) ?: 0
    private val retryId = soundPool?.load(context, R.raw.retry, 1) ?: 0

    fun configure(music: Boolean, percent: Int, effects: Boolean) {
        if (disposed) return
        musicEnabled = music
        volumePercent = percent.coerceIn(0, 100)
        effectsEnabled = effects
        refresh()
    }

    fun setForeground(active: Boolean) {
        if (disposed || foreground == active) return
        foreground = active
        refresh()
    }

    fun setNarrating(active: Boolean) {
        if (disposed || narratorSpeaking == active) return
        narratorSpeaking = active
        refresh()
    }

    private fun refresh() {
        val p = player ?: return
        val play = foreground && musicEnabled && volumePercent > 0
        if (!play) {
            fadeSerial++
            currentVolume = 0f
            runCatching {
                p.setVolume(0f, 0f)
                if (p.isPlaying) p.pause()
            }.onFailure { Log.w("KnowledgeMusic", "Pause failed", it) }
            return
        }
        runCatching {
            if (!p.isPlaying) {
                currentVolume = 0f
                p.setVolume(0f, 0f)
                p.start()
            }
            // The music is intentionally quiet and drops below speech (not over it).
            val target = WorldMusicLevels.gain(
                musicEnabled,volumePercent,foreground,narratorSpeaking)
            fadeTo(target, if (narratorSpeaking) 110L else 240L)
        }.onFailure { Log.w("KnowledgeMusic", "Start failed", it) }
    }

    private fun fadeTo(target: Float, durationMs: Long) {
        fadeSerial++
        val serial = fadeSerial
        val initial = currentVolume
        val start = SystemClock.uptimeMillis()
        main.post(object : Runnable {
            override fun run() {
                if (disposed || serial != fadeSerial || !foreground || !musicEnabled) return
                val fraction = ((SystemClock.uptimeMillis() - start).toFloat() / durationMs)
                    .coerceIn(0f, 1f)
                val next = initial + (target - initial) * fraction
                currentVolume = next
                runCatching {player?.setVolume(next, next)}
                if (fraction < 1f) main.postDelayed(this, 16L)
            }
        })
    }

    fun tap() {
        val now = SystemClock.elapsedRealtime()
        if (disposed || !foreground || !effectsEnabled || narratorSpeaking ||
            now - lastClickAt < 170L) return
        lastClickAt = now
        runCatching {soundPool?.play(tapId, .30f, .30f, 1, 0, 1f)}
    }

    fun success() {
        if (disposed || !foreground || !effectsEnabled || narratorSpeaking) return
        runCatching {soundPool?.play(successId, .70f, .70f, 1, 0, 1f)}
    }

    fun retry() {
        if (disposed || !foreground || !effectsEnabled || narratorSpeaking) return
        runCatching {soundPool?.play(retryId, .53f, .53f, 1, 0, 1f)}
    }

    fun close() {
        disposed = true
        fadeSerial++
        runCatching {
            player?.stop()
            player?.release()
            soundPool?.release()
        }
    }
}
