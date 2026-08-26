package app.auriel.basalt.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * When an alarm rule next comes due.
 *
 * The interesting cases are the ones that only happen occasionally and are
 * therefore never noticed until they go wrong: today-but-already-past, a
 * single repeat day landing on itself, and the two mornings a year the
 * local clock is not twenty-four hours long.
 */
class AlarmScheduleTest {

    private val sevenAm = LocalTime.of(7, 0)

    // 2026-08-26 is a Wednesday.
    private fun wednesdayAt(hour: Int, minute: Int = 0): LocalDateTime =
        LocalDateTime.of(2026, 8, 26, hour, minute)

    // -- one-shot ---------------------------------------------------------

    @Test
    fun `a one-shot before its time fires today`() {
        val next = AlarmSchedule.nextOccurrence(sevenAm, Weekdays.None, wednesdayAt(6, 30))
        assertEquals(LocalDateTime.of(2026, 8, 26, 7, 0), next)
    }

    @Test
    fun `a one-shot after its time fires tomorrow`() {
        val next = AlarmSchedule.nextOccurrence(sevenAm, Weekdays.None, wednesdayAt(7, 30))
        assertEquals(LocalDateTime.of(2026, 8, 27, 7, 0), next)
    }

    @Test
    fun `exactly on the minute counts as passed`() {
        // Exclusive of `from`. Resolving to the current instant would arm an
        // alarm for a moment that has already gone, which the system either
        // fires immediately or drops.
        val next = AlarmSchedule.nextOccurrence(sevenAm, Weekdays.None, wednesdayAt(7, 0))
        assertEquals(LocalDateTime.of(2026, 8, 27, 7, 0), next)
    }

    @Test
    fun `midnight is handled like any other time`() {
        val next = AlarmSchedule.nextOccurrence(
            LocalTime.MIDNIGHT,
            Weekdays.None,
            wednesdayAt(23, 59),
        )
        assertEquals(LocalDateTime.of(2026, 8, 27, 0, 0), next)
    }

    // -- repeating --------------------------------------------------------

    @Test
    fun `a repeat day that is today and still ahead fires today`() {
        val wednesdays = Weekdays.of(DayOfWeek.WEDNESDAY)
        val next = AlarmSchedule.nextOccurrence(sevenAm, wednesdays, wednesdayAt(6, 0))
        assertEquals(LocalDateTime.of(2026, 8, 26, 7, 0), next)
    }

    @Test
    fun `a single repeat day that has passed today lands a week out`() {
        // The eighth step in the walk exists for exactly this: today matches
        // the mask but the time has gone, so the answer is today next week.
        val wednesdays = Weekdays.of(DayOfWeek.WEDNESDAY)
        val next = AlarmSchedule.nextOccurrence(sevenAm, wednesdays, wednesdayAt(8, 0))
        assertEquals(LocalDateTime.of(2026, 9, 2, 7, 0), next)
        assertEquals(DayOfWeek.WEDNESDAY, next.dayOfWeek)
    }

    @Test
    fun `a repeating alarm skips days that are not in the mask`() {
        val monFri = Weekdays.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        val next = AlarmSchedule.nextOccurrence(sevenAm, monFri, wednesdayAt(8, 0))
        assertEquals(DayOfWeek.FRIDAY, next.dayOfWeek)
        assertEquals(LocalDateTime.of(2026, 8, 28, 7, 0), next)
    }

    @Test
    fun `a weekday alarm on friday evening waits for monday`() {
        val friday = LocalDateTime.of(2026, 8, 28, 20, 0)
        val next = AlarmSchedule.nextOccurrence(sevenAm, Weekdays.Weekdays5, friday)
        assertEquals(DayOfWeek.MONDAY, next.dayOfWeek)
        assertEquals(LocalDateTime.of(2026, 8, 31, 7, 0), next)
    }

    @Test
    fun `everyday never skips a day`() {
        var from = wednesdayAt(7, 30)
        repeat(14) {
            val next = AlarmSchedule.nextOccurrence(sevenAm, Weekdays.Everyday, from)
            assertEquals(from.toLocalDate().plusDays(1).atTime(sevenAm), next)
            from = next
        }
    }

    @Test
    fun `chaining from the occurrence that just fired never repeats a morning`() {
        // This is what AlarmStateManager does on dismissal: schedule the next
        // one *from the occurrence that just passed*, not from "now". Feeding
        // an occurrence back in must always move forward.
        var occurrence = LocalDateTime.of(2026, 8, 26, 7, 0)
        val seen = mutableSetOf(occurrence)
        repeat(20) {
            val next = AlarmSchedule.nextOccurrence(sevenAm, Weekdays.Weekdays5, occurrence)
            assertTrue("went backwards: $next after $occurrence", next.isAfter(occurrence))
            assertTrue("repeated an occurrence: $next", seen.add(next))
            occurrence = next
        }
    }

    @Test
    fun `it always moves forward whatever the mask and start`() {
        val masks = listOf(
            Weekdays.None,
            Weekdays.Everyday,
            Weekdays.Weekend,
            Weekdays.Weekdays5,
            Weekdays.of(DayOfWeek.SUNDAY),
            Weekdays.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
        )
        for (mask in masks) {
            for (hour in 0..23) {
                for (dayOffset in 0..7) {
                    val from = wednesdayAt(hour, 17).plusDays(dayOffset.toLong())
                    val next = AlarmSchedule.nextOccurrence(sevenAm, mask, from)
                    assertTrue("$mask at $from produced $next", next.isAfter(from))
                    assertEquals(sevenAm, next.toLocalTime())
                    if (mask.isRepeating) assertTrue(next.dayOfWeek in mask)
                }
            }
        }
    }

    // -- daylight saving --------------------------------------------------

    private val newYork = ZoneId.of("America/New_York")

    @Test
    fun `spring forward pushes a time that does not exist`() {
        // 2026-03-08: clocks go 02:00 to 03:00 in the US, so 02:30 never
        // happens. Ringing an hour "late" is the only option that is not
        // "never".
        val gap = LocalDateTime.of(2026, 3, 8, 2, 30)
        val millis = AlarmSchedule.toEpochMillis(gap, newYork)
        assertEquals(Instant.parse("2026-03-08T07:30:00Z").toEpochMilli(), millis)
    }

    @Test
    fun `autumn back takes the earlier of the two passes`() {
        // 2026-11-01: clocks go 02:00 back to 01:00, so 01:30 happens twice,
        // at -04:00 and again at -05:00. The alarm rings the first time.
        val overlap = LocalDateTime.of(2026, 11, 1, 1, 30)
        val millis = AlarmSchedule.toEpochMillis(overlap, newYork)
        assertEquals(Instant.parse("2026-11-01T05:30:00Z").toEpochMilli(), millis)
    }

    @Test
    fun `an ordinary morning either side of the change is still that morning`() {
        // The reason occurrences are stored as local date-times at all: 07:00
        // stays 07:00 on both sides of a transition, and the absolute gap
        // between them is 23 hours, not 24.
        val before = AlarmSchedule.toEpochMillis(LocalDateTime.of(2026, 3, 7, 7, 0), newYork)
        val after = AlarmSchedule.toEpochMillis(LocalDateTime.of(2026, 3, 8, 7, 0), newYork)
        assertEquals(23 * 3_600_000L, after - before)
    }

    @Test
    fun `the same local time in two zones is two different instants`() {
        val local = LocalDateTime.of(2026, 8, 26, 7, 0)
        val tokyo = AlarmSchedule.toEpochMillis(local, ZoneId.of("Asia/Tokyo"))
        val india = AlarmSchedule.toEpochMillis(local, ZoneId.of("Asia/Kolkata"))
        // Tokyo is UTC+9, Kolkata UTC+5:30. Waking at 07:00 in Tokyo happens
        // three and a half hours earlier in absolute terms.
        assertEquals(-(3 * 3_600_000L + 30 * 60_000L), tokyo - india)
    }

    @Test
    fun `resolving is stable when repeated`() {
        val local = LocalDateTime.of(2026, 8, 26, 7, 0)
        val once = AlarmSchedule.toEpochMillis(local, newYork)
        assertEquals(once, AlarmSchedule.toEpochMillis(local, newYork))
    }
}
