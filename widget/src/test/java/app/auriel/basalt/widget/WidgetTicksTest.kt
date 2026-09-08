package app.auriel.basalt.widget

import app.auriel.basalt.widget.catalog.BasaltWidget
import app.auriel.basalt.widget.update.WidgetTicks
import app.auriel.basalt.widget.update.WidgetTicks.Cadence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The update cadence.
 *
 * This is the module's battery story reduced to arithmetic, which is exactly
 * why it is worth testing here rather than discovering on a phone: a
 * scheduling bug shows up as a widget that is quietly wrong at three in the
 * morning, or as a battery graph nobody can account for. Neither gets
 * reported as a bug.
 */
class WidgetTicksTest {

    private val clockFaces = listOf(BasaltWidget.DIGITAL, BasaltWidget.ANALOG)
    private val withStopwatch = clockFaces + BasaltWidget.STOPWATCH

    // -- choosing a cadence -----------------------------------------------

    @Test
    fun `an empty home screen has no cadence at all`() {
        // Idle is not "slow", it is "cancel the alarm". A pending exact alarm
        // for zero widgets is a wake-up a minute to draw nothing.
        assertEquals(
            Cadence.Idle,
            WidgetTicks.cadence(emptyList(), hasMotion = true, screenOn = true),
        )
    }

    @Test
    fun `clock faces tick on the minute whatever else is happening`() {
        for (motion in listOf(true, false)) {
            for (screen in listOf(true, false)) {
                assertEquals(
                    "motion=$motion screen=$screen",
                    Cadence.Minute,
                    WidgetTicks.cadence(clockFaces, motion, screen),
                )
            }
        }
    }

    @Test
    fun `the fast lane needs a face, motion and an audience`() {
        assertEquals(
            Cadence.Second,
            WidgetTicks.cadence(withStopwatch, hasMotion = true, screenOn = true),
        )
        // Drop any one of the three and there is nothing to buy.
        assertEquals(
            Cadence.Minute,
            WidgetTicks.cadence(withStopwatch, hasMotion = false, screenOn = true),
        )
        assertEquals(
            Cadence.Minute,
            WidgetTicks.cadence(withStopwatch, hasMotion = true, screenOn = false),
        )
        assertEquals(
            Cadence.Minute,
            WidgetTicks.cadence(clockFaces, hasMotion = true, screenOn = true),
        )
    }

    @Test
    fun `a pocketed phone never ticks per second`() {
        // The single most expensive mistake available here: a stopwatch
        // running behind a dark screen, redrawn sixty times a minute for
        // nobody.
        BasaltWidget.all.forEach { widget ->
            assertEquals(
                widget.id,
                Cadence.Minute,
                WidgetTicks.cadence(listOf(widget), hasMotion = true, screenOn = false),
            )
        }
    }

    // -- when the next tick lands -----------------------------------------

    @Test
    fun `a minute tick lands on the minute boundary`() {
        assertEquals(
            120_000L,
            WidgetTicks.nextTickMillis(nowMillis = 61_500L, cadence = Cadence.Minute),
        )
    }

    @Test
    fun `landing exactly on a boundary schedules the next one, not this one`() {
        // Rescheduling for the instant that just fired is a spin, and the
        // callback that reschedules is running at exactly that instant.
        assertEquals(120_000L, WidgetTicks.nextTickMillis(60_000L, Cadence.Minute))
        assertEquals(2_000L, WidgetTicks.nextTickMillis(1_000L, Cadence.Second))
    }

    @Test
    fun `a second tick lands on the second boundary`() {
        assertEquals(1_000L, WidgetTicks.nextTickMillis(1L, Cadence.Second))
        assertEquals(63_000L, WidgetTicks.nextTickMillis(62_400L, Cadence.Second))
    }

    @Test
    fun `the tick is always aligned, never now plus a period`() {
        // The whole reason for aligning: a widget scheduled as "now + 60s"
        // drifts by however late each pass ran, and ends up disagreeing with
        // the lockscreen about what time it is — which reads as a broken
        // widget, not as a late one.
        for (offset in 0L until 60_000L step 997L) {
            val at = WidgetTicks.nextTickMillis(1_700_000_000_000L + offset, Cadence.Minute)
            assertEquals("offset $offset", 0L, at % 60_000L)
        }
    }

    @Test
    fun `the next tick is always in the future`() {
        for (offset in 0L until 120_000L step 331L) {
            listOf(Cadence.Minute, Cadence.Second).forEach { cadence ->
                assertTrue(
                    "$cadence at $offset",
                    WidgetTicks.nextTickMillis(offset, cadence) > offset,
                )
            }
        }
    }

    @Test
    fun `idle has no next tick`() {
        assertEquals(5_000L, WidgetTicks.nextTickMillis(5_000L, Cadence.Idle))
        assertEquals(0L, WidgetTicks.delayToNextTick(5_000L, Cadence.Idle))
    }

    @Test
    fun `the delay is never zero`() {
        // A zero-delay exact alarm rescheduled from its own callback is a
        // spin, and the callback reschedules on every pass.
        for (offset in 0L until 60_000L step 137L) {
            listOf(Cadence.Minute, Cadence.Second).forEach { cadence ->
                assertTrue(WidgetTicks.delayToNextTick(offset, cadence) >= 1L)
            }
        }
    }

    @Test
    fun `the delay never exceeds the period`() {
        for (offset in 0L until 200_000L step 71L) {
            assertTrue(
                WidgetTicks.delayToNextTick(offset, Cadence.Minute) <= 60_000L,
            )
            assertTrue(
                WidgetTicks.delayToNextTick(offset, Cadence.Second) <= 1_000L,
            )
        }
    }

    @Test
    fun `alignment holds either side of the epoch`() {
        // Negative epoch millis are not a real clock reading, but floorMod
        // versus rem is the kind of thing that is either right everywhere or
        // wrong in one place nobody looks.
        assertEquals(-60_000L, WidgetTicks.nextTickMillis(-90_000L, Cadence.Minute))
        assertTrue(WidgetTicks.nextTickMillis(-90_000L, Cadence.Minute) > -90_000L)
    }
}
