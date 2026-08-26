package app.auriel.basalt.core.time

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Duration formatting.
 *
 * The requirement that is easy to lose is a *stable character count*: the
 * dot-matrix readout is laid out from the measured string, so a format that
 * drops a digit makes the whole row jump sideways once a second.
 */
class DurationFormatTest {

    @Test
    fun `coarse pads seconds but not minutes`() {
        assertEquals("0:00", DurationFormat.coarse(0))
        assertEquals("0:09", DurationFormat.coarse(9_000))
        assertEquals("1:00", DurationFormat.coarse(60_000))
        assertEquals("9:59", DurationFormat.coarse(599_000))
        assertEquals("59:59", DurationFormat.coarse(3_599_000))
    }

    @Test
    fun `coarse widens to hours only once there is an hour`() {
        assertEquals("1:00:00", DurationFormat.coarse(3_600_000))
        assertEquals("1:02:03", DurationFormat.coarse(3_723_000))
        assertEquals("25:00:00", DurationFormat.coarse(25 * 3_600_000L))
    }

    @Test
    fun `coarse truncates sub-second remainders rather than rounding up`() {
        // A timer showing 0:01 when 1.9 s remain is right; showing 0:02 means
        // the readout hits zero a whole second after the alarm has gone off.
        assertEquals("0:01", DurationFormat.coarse(1_999))
        assertEquals("0:00", DurationFormat.coarse(999))
    }

    @Test
    fun `coarse signs a timer that has run past zero`() {
        assertEquals("-0:01", DurationFormat.coarse(-1_000))
        assertEquals("-1:30", DurationFormat.coarse(-90_000))
        assertEquals("-1:00:00", DurationFormat.coarse(-3_600_000))
    }

    @Test
    fun `coarse is symmetric about zero`() {
        listOf(1_000L, 61_000L, 3_600_000L, 3_723_456L).forEach {
            assertEquals("-" + DurationFormat.coarse(it), DurationFormat.coarse(-it))
        }
    }

    @Test
    fun `zero carries no sign`() {
        // There is no negative zero on the readout: a timer sitting exactly
        // on the deadline shows 0:00, not -0:00.
        assertEquals("0:00", DurationFormat.coarse(0))
        assertEquals("0:00", DurationFormat.coarse(-0L))
    }

    @Test
    fun `precise pads to a fixed width below an hour`() {
        assertEquals("00:00.00", DurationFormat.precise(0))
        assertEquals("00:00.09", DurationFormat.precise(90))
        assertEquals("00:01.00", DurationFormat.precise(1_000))
        assertEquals("01:00.00", DurationFormat.precise(60_000))
        assertEquals("59:59.99", DurationFormat.precise(3_599_999))
    }

    @Test
    fun `precise keeps a stable width for the whole first hour`() {
        val widths = (0 until 3_600_000 step 97_003)
            .map { DurationFormat.precise(it.toLong()).length }
            .toSet()
        assertEquals(setOf(8), widths)
    }

    @Test
    fun `precise adds an hours field once there is one`() {
        assertEquals("1:00:00.00", DurationFormat.precise(3_600_000))
        assertEquals("2:03:04.56", DurationFormat.precise(2 * 3_600_000L + 184_560))
    }

    @Test
    fun `precise reports hundredths, not milliseconds`() {
        assertEquals("00:00.12", DurationFormat.precise(129))
    }
}
