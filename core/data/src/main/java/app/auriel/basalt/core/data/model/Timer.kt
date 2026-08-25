package app.auriel.basalt.core.data.model

/**
 * A countdown timer.
 *
 * Remaining time is stored as a *deadline* while running and as a
 * *remainder* while paused, because a running timer has to survive process
 * death: only the deadline is meaningful across a restart.
 *
 * A timer that reaches zero does not stop. It keeps counting, negatively,
 * so the user can see how long ago it went off — AOSP behaviour, and the
 * reason [remainingMillis] is signed.
 */
data class Timer(
    val id: Long = 0L,
    val totalMillis: Long,
    val label: String = "",
    val state: TimerState = TimerState.Reset,
    /** Wall-clock deadline; meaningful only while [TimerState.Running]. */
    val deadlineRealtimeMillis: Long = 0L,
    /** Frozen remainder; meaningful only while [TimerState.Paused]. */
    val pausedRemainingMillis: Long = totalMillis,
) {
    fun remainingMillis(elapsedRealtimeMillis: Long): Long = when (state) {
        TimerState.Running, TimerState.Expired ->
            deadlineRealtimeMillis - elapsedRealtimeMillis

        TimerState.Paused -> pausedRemainingMillis
        TimerState.Reset -> totalMillis
    }
}

enum class TimerState {
    Reset,
    Running,
    Paused,

    /** Past zero and counting up. Still "running" in every practical sense. */
    Expired,
}
