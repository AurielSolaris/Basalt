package app.auriel.basalt.core.data

import app.auriel.basalt.core.data.model.Alarm
import app.auriel.basalt.core.time.Weekdays
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class AlarmTest {

    private fun alarm(start: Long = 0L, end: Long = 0L) = Alarm(
        time = LocalTime.of(7, 0),
        ringtoneStartMillis = start,
        ringtoneEndMillis = end,
    )

    @Test
    fun `an untrimmed alarm plays the whole file`() {
        assertFalse(alarm().hasTrim)
    }

    @Test
    fun `a start offset alone counts as trimmed`() {
        assertTrue(alarm(start = 45_000).hasTrim)
    }

    @Test
    fun `an end offset alone counts as trimmed`() {
        // Zero means "to the end", so any non-zero end is a real trim.
        assertTrue(alarm(end = 90_000).hasTrim)
    }

    @Test
    fun `both offsets count as trimmed`() {
        assertTrue(alarm(start = 45_000, end = 90_000).hasTrim)
    }

    @Test
    fun `a new alarm is enabled, one-shot and unlabelled`() {
        val fresh = Alarm(time = LocalTime.of(6, 30))
        assertTrue(fresh.enabled)
        assertFalse(fresh.repeatDays.isRepeating)
        assertFalse(fresh.skipNext)
        assertTrue(fresh.label.isEmpty())
    }

    @Test
    fun `repeat days round-trip through the mask`() {
        val weekdays = Alarm(
            time = LocalTime.of(6, 30),
            repeatDays = Weekdays.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        )
        assertTrue(DayOfWeek.MONDAY in weekdays.repeatDays)
        assertTrue(DayOfWeek.WEDNESDAY in weekdays.repeatDays)
        assertFalse(DayOfWeek.TUESDAY in weekdays.repeatDays)
    }
}
