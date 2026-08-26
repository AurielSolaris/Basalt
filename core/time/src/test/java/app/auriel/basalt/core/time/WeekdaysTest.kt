package app.auriel.basalt.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * The repeat mask.
 *
 * Worth testing carefully despite being twenty lines: it is a bitfield with
 * a hand-rolled bit order, it is persisted, and every wrong answer it can
 * give is an alarm on the wrong day.
 */
class WeekdaysTest {

    @Test
    fun `none contains nothing and is not repeating`() {
        DayOfWeek.values().forEach { assertFalse(it in Weekdays.None) }
        assertFalse(Weekdays.None.isRepeating)
        assertEquals(0, Weekdays.None.count)
    }

    @Test
    fun `everyday contains every day`() {
        DayOfWeek.values().forEach { assertTrue(it in Weekdays.Everyday) }
        assertEquals(7, Weekdays.Everyday.count)
    }

    @Test
    fun `weekend is exactly saturday and sunday`() {
        assertEquals(2, Weekdays.Weekend.count)
        assertTrue(DayOfWeek.SATURDAY in Weekdays.Weekend)
        assertTrue(DayOfWeek.SUNDAY in Weekdays.Weekend)
        assertFalse(DayOfWeek.FRIDAY in Weekdays.Weekend)
    }

    @Test
    fun `weekdays5 is monday to friday and disjoint from the weekend`() {
        assertEquals(5, Weekdays.Weekdays5.count)
        assertTrue(DayOfWeek.MONDAY in Weekdays.Weekdays5)
        assertTrue(DayOfWeek.FRIDAY in Weekdays.Weekdays5)
        assertFalse(DayOfWeek.SATURDAY in Weekdays.Weekdays5)
        assertEquals(0, Weekdays.Weekdays5.bits and Weekdays.Weekend.bits)
        assertEquals(Weekdays.Everyday.bits, Weekdays.Weekdays5.bits or Weekdays.Weekend.bits)
    }

    @Test
    fun `each day owns a distinct bit`() {
        val seen = DayOfWeek.values().map { Weekdays.of(it).bits }
        assertEquals(7, seen.toSet().size)
        assertEquals(Weekdays.Everyday.bits, seen.reduce(Int::or))
    }

    @Test
    fun `with adds and removes only the named day`() {
        val monday = Weekdays.of(DayOfWeek.MONDAY)
        val both = monday.with(DayOfWeek.THURSDAY, true)
        assertEquals(2, both.count)
        assertTrue(DayOfWeek.THURSDAY in both)

        val backToMonday = both.with(DayOfWeek.THURSDAY, false)
        assertEquals(monday.bits, backToMonday.bits)
    }

    @Test
    fun `with is idempotent in both directions`() {
        val set = Weekdays.of(DayOfWeek.TUESDAY)
        assertEquals(set.bits, set.with(DayOfWeek.TUESDAY, true).bits)
        assertEquals(Weekdays.None.bits, set.with(DayOfWeek.TUESDAY, false).bits)
        assertEquals(Weekdays.None.bits, Weekdays.None.with(DayOfWeek.TUESDAY, false).bits)
    }

    @Test
    fun `toggle flips`() {
        val once = Weekdays.None.toggle(DayOfWeek.WEDNESDAY)
        assertTrue(DayOfWeek.WEDNESDAY in once)
        assertFalse(DayOfWeek.WEDNESDAY in once.toggle(DayOfWeek.WEDNESDAY))
    }

    @Test
    fun `daysUntilNext returns zero when today is a repeat day`() {
        val mwf = Weekdays.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertEquals(0, mwf.daysUntilNext(DayOfWeek.MONDAY))
    }

    @Test
    fun `daysUntilNext walks forward to the next set day`() {
        val mwf = Weekdays.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertEquals(2, mwf.daysUntilNext(DayOfWeek.SATURDAY))
        assertEquals(1, mwf.daysUntilNext(DayOfWeek.TUESDAY))
    }

    @Test
    fun `daysUntilNext wraps around the end of the week`() {
        val monday = Weekdays.of(DayOfWeek.MONDAY)
        assertEquals(1, monday.daysUntilNext(DayOfWeek.SUNDAY))
        assertEquals(6, monday.daysUntilNext(DayOfWeek.TUESDAY))
    }

    @Test
    fun `daysUntilNext is null when nothing repeats`() {
        assertNull(Weekdays.None.daysUntilNext(DayOfWeek.MONDAY))
    }
}
