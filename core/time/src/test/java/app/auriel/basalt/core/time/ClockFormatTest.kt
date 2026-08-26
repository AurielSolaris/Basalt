package app.auriel.basalt.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * Wall-clock formatting.
 *
 * Twelve-hour time has exactly two cases anyone ever gets wrong, and they
 * are both an hour that reads as zero: midnight and noon.
 */
class ClockFormatTest {

    @Test
    fun `24 hour time is always five characters`() {
        assertEquals("00:00", ClockFormat.time(LocalTime.MIDNIGHT, use24Hour = true))
        assertEquals("07:05", ClockFormat.time(LocalTime.of(7, 5), use24Hour = true))
        assertEquals("23:59", ClockFormat.time(LocalTime.of(23, 59), use24Hour = true))
    }

    @Test
    fun `midnight is twelve, not zero`() {
        assertEquals("12:00", ClockFormat.time(LocalTime.MIDNIGHT, use24Hour = false))
        assertEquals("12:30", ClockFormat.time(LocalTime.of(0, 30), use24Hour = false))
    }

    @Test
    fun `noon is twelve PM`() {
        assertEquals("12:00", ClockFormat.time(LocalTime.NOON, use24Hour = false))
        assertEquals("PM", ClockFormat.meridiem(LocalTime.NOON, use24Hour = false))
    }

    @Test
    fun `12 hour time drops the leading zero on the hour but not the minute`() {
        assertEquals("7:05", ClockFormat.time(LocalTime.of(7, 5), use24Hour = false))
        assertEquals("11:59", ClockFormat.time(LocalTime.of(23, 59), use24Hour = false))
    }

    @Test
    fun `meridiem flips exactly at noon`() {
        assertEquals("AM", ClockFormat.meridiem(LocalTime.of(11, 59), use24Hour = false))
        assertEquals("PM", ClockFormat.meridiem(LocalTime.of(12, 0), use24Hour = false))
        assertEquals("AM", ClockFormat.meridiem(LocalTime.MIDNIGHT, use24Hour = false))
    }

    @Test
    fun `meridiem is empty in 24 hour mode`() {
        assertEquals("", ClockFormat.meridiem(LocalTime.of(13, 0), use24Hour = true))
    }

    @Test
    fun `every minute of the day formats and round-trips its hour`() {
        for (minuteOfDay in 0 until 24 * 60) {
            val time = LocalTime.ofSecondOfDay(minuteOfDay * 60L)
            val twelve = ClockFormat.time(time, use24Hour = false)
            val hour = twelve.substringBefore(':').toInt()
            assertTrue("$time produced hour $hour", hour in 1..12)
            assertEquals(5, ClockFormat.time(time, use24Hour = true).length)
        }
    }

    @Test
    fun `seconds are always two padded digits`() {
        // Fixed width matters more here than anywhere else in the app: this
        // is the one field that redraws every second, and a string that
        // changes length would shove the readout sideways sixty times a
        // minute.
        for (second in 0..59) {
            val text = ClockFormat.seconds(LocalTime.of(12, 0, second))
            assertEquals(2, text.length)
            assertEquals(second, text.toInt())
        }
    }

    @Test
    fun `seconds ignore the hour and the clock mode`() {
        assertEquals("07", ClockFormat.seconds(LocalTime.of(23, 45, 7)))
        assertEquals("00", ClockFormat.seconds(LocalTime.MIDNIGHT))
    }

    @Test
    fun `the date line is upper case and abbreviated`() {
        assertEquals(
            "WED 26 AUG",
            ClockFormat.date(LocalDate.of(2026, 8, 26), Locale.US),
        )
    }

    @Test
    fun `the date line does not pad the day of month`() {
        assertEquals("SUN 1 NOV", ClockFormat.date(LocalDate.of(2026, 11, 1), Locale.US))
    }
}
