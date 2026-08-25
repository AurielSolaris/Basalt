package app.auriel.basalt.core.data.model

/**
 * The stopwatch, stored as accumulated time plus an optional start mark.
 *
 * Keeping [accumulatedMillis] separate from the current run means pausing
 * needs no clock reading of its own and elapsed time never drifts across
 * repeated start/stop cycles.
 */
data class Stopwatch(
    val state: StopwatchState = StopwatchState.Reset,
    val accumulatedMillis: Long = 0L,
    /** Monotonic mark of the current run; meaningful only while running. */
    val startedAtRealtimeMillis: Long = 0L,
) {
    fun elapsedMillis(elapsedRealtimeMillis: Long): Long = when (state) {
        StopwatchState.Running ->
            accumulatedMillis + (elapsedRealtimeMillis - startedAtRealtimeMillis)

        StopwatchState.Paused, StopwatchState.Reset -> accumulatedMillis
    }
}

enum class StopwatchState { Reset, Running, Paused }

/**
 * One recorded lap.
 *
 * Both figures are stored rather than derived: [totalMillis] is what the
 * stopwatch read at the moment the lap was taken, and [lapMillis] is the
 * split from the previous lap. Storing the split avoids re-deriving the
 * whole list when one lap is inserted.
 */
data class Lap(
    val number: Int,
    val lapMillis: Long,
    val totalMillis: Long,
)
