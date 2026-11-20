package com.game.knowledge

/**
 * Hardware progress monitor for a reused AudioTrack. Handles a vendor HAL
 * resetting its unsigned 32-bit playback head across stop/play, without
 * interpreting the huge unsigned difference as an instant completed phrase.
 */
internal class SpeechPlaybackHead(startHead: Long) {
    private var previous = startHead and MASK
    var playedFrames: Long = 0L
        private set
    var counterResets = 0
        private set

    fun observe(head: Long): Boolean {
        val now = head and MASK
        val delta = (now - previous) and MASK
        previous = now
        return when {
            delta == 0L -> false
            delta <= MAX_POLL_FRAMES -> {
                playedFrames += delta
                true
            }
            else -> {
                counterResets++
                false
            }
        }
    }

    companion object {
        private const val MASK = 0xffffffffL
        // At 48kHz: allow up to 2s between polls; reject reset-size jumps.
        private const val MAX_POLL_FRAMES = 96_000L
    }
}
