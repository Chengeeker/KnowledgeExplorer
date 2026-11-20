package com.game.knowledge

import org.junit.Assert.*
import org.junit.Test

class SpeechPlaybackHeadTest {
    @Test fun longSpeechTracksFramesInSmallChunks() {
        val progress=SpeechPlaybackHead(0)
        // Simulate 30 seconds of 48kHz PCM emitted across a 1.8s buffer.
        repeat(1500) { i -> assertTrue(progress.observe((i+1L)*960)) }
        assertEquals(1_440_000L,progress.playedFrames)
        assertEquals(0,progress.counterResets)
    }

    @Test fun playbackHeadResetCannotInstantlyCompleteSpeech() {
        val progress=SpeechPlaybackHead(123_000)
        assertTrue(progress.observe(124_000))
        assertFalse(progress.observe(0))
        assertEquals(1_000L,progress.playedFrames)
        assertEquals(1,progress.counterResets)
        assertTrue(progress.observe(960))
        assertEquals(1_960L,progress.playedFrames)
    }

    @Test fun wrapAtUnsigned32BitBoundaryIsNormalForwardProgress() {
        val progress=SpeechPlaybackHead(0xfffffe00L)
        assertTrue(progress.observe(0x00000100L))
        assertEquals(768L,progress.playedFrames)
        assertEquals(0,progress.counterResets)
    }

    @Test fun unchangedCounterMeansPlaybackDidNotAdvance() {
        val progress=SpeechPlaybackHead(400)
        repeat(200) { assertFalse(progress.observe(400)) }
        assertEquals(0L,progress.playedFrames)
        assertFalse(progress.observe(120_000))
        assertEquals(1,progress.counterResets)
    }
}
