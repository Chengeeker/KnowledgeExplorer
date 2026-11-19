package com.game.knowledge

/** Monotonic-clock thinking windows, separately tuned for each learning mode.
 * Never use wall-clock time: users can change the device date.
 */
object ExplanationGate {
    const val STUDY_WAIT_MS = 3_000L
    const val QUIZ_WAIT_MS = 10_000L

    fun remainingMs(startedAt: Long, now: Long, waitMs: Long = QUIZ_WAIT_MS): Long =
        (waitMs - (now - startedAt).coerceAtLeast(0L)).coerceAtLeast(0L)

    fun canReveal(startedAt: Long, now: Long, waitMs: Long = QUIZ_WAIT_MS): Boolean =
        remainingMs(startedAt, now, waitMs) == 0L

    fun secondsRemaining(startedAt: Long, now: Long, waitMs: Long = QUIZ_WAIT_MS): Long =
        (remainingMs(startedAt, now, waitMs) + 999L) / 1000L
}
