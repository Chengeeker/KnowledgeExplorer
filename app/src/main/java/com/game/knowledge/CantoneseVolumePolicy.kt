package com.game.knowledge

import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * A volume control for Cantonese speech ONLY. It deliberately does not touch
 * the app's music, Mandarin narrator, global media stream or feedback sounds.
 *
 * 0..100% controls MediaPlayer gain; >100% uses Android LoudnessEnhancer to
 * increase perceived loudness (max approximately +6 dB), with device-limited
 * behavior and no unsafe system-wide volume change.
 */
internal object CantoneseVolumePolicy {
    const val MIN_PERCENT=0
    const val MAX_PERCENT=200
    const val DEFAULT_PERCENT=150
    const val STEP_PERCENT=5

    fun clamp(percent:Int):Int = percent.coerceIn(MIN_PERCENT,MAX_PERCENT)

    /** AudioTrack / MediaPlayer gain, always between silence and full scale. */
    fun playerGain(percent:Int):Float = (clamp(percent).coerceAtMost(100)/100f)

    /** Android TextToSpeech only supports values 0..1; extra amplification is for recorded clips. */
    fun ttsGain(percent:Int):Float = playerGain(percent)

    /** Millibels for LoudnessEnhancer: 150%=+3.52dB; 200%=+6.02dB. */
    fun boostMillibels(percent:Int):Int {
        val value=clamp(percent)
        if(value<=100) return 0
        return (2000.0*log10(value/100.0)).roundToInt().coerceIn(0,603)
    }

    fun step(value:Float):Int =
        (value/STEP_PERCENT).roundToInt()
            .times(STEP_PERCENT).coerceIn(MIN_PERCENT,MAX_PERCENT)
}
