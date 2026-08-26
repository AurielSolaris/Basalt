package app.auriel.basalt.core.data

import app.auriel.basalt.core.data.model.Lap
import app.auriel.basalt.core.data.model.Stopwatch
import app.auriel.basalt.core.data.model.StopwatchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchTest {

    private val noon = 1_774_000_000_000L

    @Test
    fun `a reset stopwatch reads zero forever`() {
        val stopwatch = Stopwatch()
        assertEquals(0L, stopwatch.elapsedMillis(noon))
        assertEquals(0L, stopwatch.elapsedMillis(noon + 10_000))
        assertFalse(stopwatch.isRunning)
    }

    @Test
    fun `a running stopwatch counts from its start mark`() {
        val stopwatch = Stopwatch(
            state = StopwatchState.Running,
            startedAtWallMillis = noon,
        )
        assertEquals(0L, stopwatch.elapsedMillis(noon))
        assertEquals(12_340L, stopwatch.elapsedMillis(noon + 12_340))
        assertTrue(stopwatch.isRunning)
    }

    @Test
    fun `a paused stopwatch holds its accumulated total`() {
        val stopwatch = Stopwatch(
            state = StopwatchState.Paused,
            accumulatedMillis = 5_000L,
            startedAtWallMillis = noon,
        )
        assertEquals(5_000L, stopwatch.elapsedMillis(noon + 60_000))
    }

    @Test
    fun `restarting adds to the accumulated total rather than replacing it`() {
        val resumed = Stopwatch(
            state = StopwatchState.Running,
            accumulatedMillis = 5_000L,
            startedAtWallMillis = noon,
        )
        assertEquals(8_000L, resumed.elapsedMillis(noon + 3_000))
    }

    @Test
    fun `repeated start and stop cycles do not drift`() {
        // Keeping the accumulated total separate from the current run means
        // pausing needs no clock reading of its own, so nothing is lost to
        // rounding across a hundred cycles.
        var stopwatch = Stopwatch()
        var now = noon
        repeat(100) {
            stopwatch = stopwatch.copy(
                state = StopwatchState.Running,
                startedAtWallMillis = now,
            )
            now += 137L
            stopwatch = stopwatch.copy(
                state = StopwatchState.Paused,
                accumulatedMillis = stopwatch.elapsedMillis(now),
            )
            now += 991L
        }
        assertEquals(100 * 137L, stopwatch.elapsedMillis(now))
    }

    @Test
    fun `laps carry both the split and the running total`() {
        val laps = listOf(
            Lap(number = 1, lapMillis = 30_000, totalMillis = 30_000),
            Lap(number = 2, lapMillis = 28_500, totalMillis = 58_500),
            Lap(number = 3, lapMillis = 31_200, totalMillis = 89_700),
        )
        assertEquals(laps.last().totalMillis, laps.sumOf(Lap::lapMillis))
        laps.zipWithNext { previous, next ->
            assertEquals(next.totalMillis - previous.totalMillis, next.lapMillis)
        }
    }
}
