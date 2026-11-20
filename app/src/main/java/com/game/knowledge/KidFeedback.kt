package com.game.knowledge

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.compositionLocalOf

/** The math game's light / success / retry haptic vocabulary, with an independent toggle. */
class KidFeedback(context: Context, private val audio: WorldAudio) {
    private val vibrator = if (Build.VERSION.SDK_INT >= 31)
        context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
    else @Suppress("DEPRECATION")
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    var enabled: Boolean = true
    private var lastAt = 0L

    @Suppress("DEPRECATION")
    private fun pulse(duration: Long, double: Boolean = false) {
        val motor = vibrator ?: return
        if (!enabled || !motor.hasVibrator()) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastAt < 75L) return
        lastAt = now
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (double) motor.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0, 18, 52, 27), intArrayOf(0, 80, 0, 80), -1
                )) else motor.vibrate(VibrationEffect.createOneShot(duration, 75))
            } else {
                if (double) motor.vibrate(longArrayOf(0, 18, 52, 27), -1)
                else motor.vibrate(duration)
            }
        }
    }

    // Sounds and vibration are independent user preferences: WorldAudio
    // honors the effects switch; pulse honors the haptics switch.
    fun tap() {pulse(14);audio.tap()}
    fun success() {pulse(35);audio.success()}
    fun error() {pulse(38,double=true);audio.retry()}
}

val LocalKidFeedback = compositionLocalOf<KidFeedback?> { null }
