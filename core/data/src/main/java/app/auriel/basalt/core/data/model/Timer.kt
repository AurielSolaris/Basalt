package app.auriel.basalt.core.data.model

/**
 * A countdown timer.
 *
 * A running timer stores a *deadline*; a paused one stores a *remainder*.
 * Only the deadline is meaningful across a process restart, and only the
 * remainder is meaningful while paused, so keeping both fields and letting
 * the state say which one is live beats trying to make one field do both.
 *
 * The deadline is on the **wall clock**, not the monotonic one.
 * `SystemClock.elapsedRealtime()` restarts at zero on reboot, so a
 * persisted monotonic deadline would be nonsense after a restart — which is
 * exactly when a timer most needs to still be right. The cost is that
 * moving the system clock moves running timers with it; that is the rarer
 * and more visible failure, and it is the trade AOSP makes too.
 *
 * A timer that reaches zero does not stop. It keeps counting, negatively,
 * so "how long ago did that go off" is answerable — hence a signed
 * [remainingMillis].
 */
data class Timer(
    val id: Long = 0L,
    val totalMillis: Long,
    val label: String = "",
    val state: TimerState = TimerState.Reset,
    /** Wall-clock deadline; meaningful while running or expired. */
    val deadlineWallMillis: Long = 0L,
    /** Frozen remainder; meaningful while paused. */
    val pausedRemainingMillis: Long = totalMillis,
    val createdAtMillis: Long = 0L,
) {
    fun remainingMillis(nowWallMillis: Long): Long = when (state) {
        TimerState.Running, TimerState.Expired -> deadlineWallMillis - nowWallMillis
        TimerState.Paused -> pausedRemainingMillis
        TimerState.Reset -> totalMillis
    }

    fun isRunning(): Boolean = state == TimerState.Running || state == TimerState.Expired
}

enum class TimerState {
    Reset,
    Running,
    Paused,

    /** Past zero and counting up. Still running in every practical sense. */
    Expired,
}
