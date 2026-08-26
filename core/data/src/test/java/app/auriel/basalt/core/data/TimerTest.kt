package app.auriel.basalt.core.data

import app.auriel.basalt.core.data.model.Timer
import app.auriel.basalt.core.data.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The countdown.
 *
 * The two properties that matter are both about restarts: a running timer's
 * remainder must be derived from a wall-clock deadline so it survives the
 * process dying, and a paused one's must be frozen so it does not.
 */
class TimerTest {

    private val fiveMinutes = 5 * 60_000L
    private val noon = 1_774_000_000_000L

    private fun running(deadline: Long) = Timer(
        totalMillis = fiveMinutes,
        state = TimerState.Running,
        deadlineWallMillis = deadline,
    )

    @Test
    fun `a reset timer reads its full length`() {
        val timer = Timer(totalMillis = fiveMinutes)
        assertEquals(fiveMinutes, timer.remainingMillis(noon))
        assertFalse(timer.isRunning())
    }

    @Test
    fun `a running timer counts down from the deadline`() {
        val timer = running(noon + fiveMinutes)
        assertEquals(fiveMinutes, timer.remainingMillis(noon))
        assertEquals(fiveMinutes - 1_000, timer.remainingMillis(noon + 1_000))
        assertEquals(0L, timer.remainingMillis(noon + fiveMinutes))
    }

    @Test
    fun `a running timer does not stop at zero`() {
        // "How long ago did that go off" has to be answerable, so the
        // remainder is signed and keeps going.
        val timer = running(noon)
        assertEquals(-30_000L, timer.remainingMillis(noon + 30_000))
    }

    @Test
    fun `an expired timer is still running`() {
        val timer = running(noon).copy(state = TimerState.Expired)
        assertTrue(timer.isRunning())
        assertEquals(-1_000L, timer.remainingMillis(noon + 1_000))
    }

    @Test
    fun `a paused timer ignores the clock entirely`() {
        val paused = Timer(
            totalMillis = fiveMinutes,
            state = TimerState.Paused,
            deadlineWallMillis = noon,
            pausedRemainingMillis = 90_000L,
        )
        assertEquals(90_000L, paused.remainingMillis(noon))
        assertEquals(90_000L, paused.remainingMillis(noon + 10 * 60_000L))
        assertFalse(paused.isRunning())
    }

    @Test
    fun `a running timer survives a gap in the process`() {
        // This is the whole reason the deadline is on the wall clock. The
        // timer is created, nothing of ours runs for two minutes, and the
        // remainder is still correct when someone looks again.
        val timer = running(noon + fiveMinutes)
        val afterTheGap = timer.remainingMillis(noon + 2 * 60_000L)
        assertEquals(3 * 60_000L, afterTheGap)
    }

    @Test
    fun `pausing and resuming does not lose or gain time`() {
        var timer = running(noon + fiveMinutes)

        val pausedAt = noon + 60_000L
        val frozen = timer.remainingMillis(pausedAt)
        timer = timer.copy(state = TimerState.Paused, pausedRemainingMillis = frozen)
        assertEquals(4 * 60_000L, timer.remainingMillis(pausedAt + 10 * 60_000L))

        val resumedAt = pausedAt + 10 * 60_000L
        timer = timer.copy(
            state = TimerState.Running,
            deadlineWallMillis = resumedAt + timer.pausedRemainingMillis,
        )
        assertEquals(4 * 60_000L, timer.remainingMillis(resumedAt))
    }

    @Test
    fun `a fresh timer defaults its paused remainder to the full length`() {
        assertEquals(fiveMinutes, Timer(totalMillis = fiveMinutes).pausedRemainingMillis)
    }
}
