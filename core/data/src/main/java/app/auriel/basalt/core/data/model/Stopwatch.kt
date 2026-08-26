package app.auriel.basalt.core.data.model

/**
 * The stopwatch: accumulated time plus an optional start mark.
 *
 * Keeping [accumulatedMillis] separate from the current run means pausing
 * needs no clock reading of its own, and elapsed time does not drift across
 * repeated start/stop cycles.
 *
 * The start mark is on the wall clock, for the same reason timers are: it
 * has to survive a reboot, and the monotonic clock does not.
 */
data class Stopwatch(
    val state: StopwatchState = StopwatchState.Reset,
    val accumulatedMillis: Long = 0L,
    val startedAtWallMillis: Long = 0L,
) {
    fun elapsedMillis(nowWallMillis: Long): Long = when (state) {
        StopwatchState.Running ->
            accumulatedMillis + (nowWallMillis - startedAtWallMillis)

        StopwatchState.Paused, StopwatchState.Reset -> accumulatedMillis
    }

    val isRunning: Boolean get() = state == StopwatchState.Running
}

enum class StopwatchState { Reset, Running, Paused }

/**
 * One recorded lap.
 *
 * Both figures are stored rather than derived: [totalMillis] is what the
 * stopwatch read when the lap was taken, and [lapMillis] is the split from
 * the previous lap.
 */
data class Lap(
    val number: Int,
    val lapMillis: Long,
    val totalMillis: Long,
)
