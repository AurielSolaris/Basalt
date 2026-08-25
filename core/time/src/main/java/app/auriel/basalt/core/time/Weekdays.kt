package app.auriel.basalt.core.time

import java.time.DayOfWeek

/**
 * The set of weekdays an alarm repeats on, as a 7-bit mask.
 *
 * Ported from AOSP DeskClock's `Weekdays`, which stores repeat days as a
 * bitfield so an alarm row needs one integer column rather than seven
 * booleans. The bit order here is [DayOfWeek]'s (Monday = 1), not the
 * legacy `Calendar` order, so the value is not wire-compatible with
 * DeskClock's database.
 */
@JvmInline
value class Weekdays(val bits: Int) {

    operator fun contains(day: DayOfWeek): Boolean =
        bits and day.mask() != 0

    fun with(day: DayOfWeek, repeating: Boolean): Weekdays =
        Weekdays(if (repeating) bits or day.mask() else bits and day.mask().inv())

    fun toggle(day: DayOfWeek): Weekdays = with(day, day !in this)

    val isRepeating: Boolean get() = bits != 0

    val count: Int get() = Integer.bitCount(bits)

    /**
     * Days until the next occurrence on or after [from], or null when the
     * alarm does not repeat. Returns 0 when [from] itself is a repeat day —
     * callers still have to decide whether today's firing time has passed.
     */
    fun daysUntilNext(from: DayOfWeek): Int? {
        if (!isRepeating) return null
        for (offset in 0 until 7) {
            if (from.plus(offset.toLong()) in this) return offset
        }
        return null
    }

    companion object {
        val None = Weekdays(0)
        val Weekend = Weekdays(DayOfWeek.SATURDAY.mask() or DayOfWeek.SUNDAY.mask())
        val Weekdays5 = Weekdays(0b111_1111).let { all ->
            Weekdays(all.bits and Weekend.bits.inv())
        }
        val Everyday = Weekdays(0b111_1111)

        fun of(vararg days: DayOfWeek): Weekdays =
            Weekdays(days.fold(0) { acc, day -> acc or day.mask() })
    }
}

private fun DayOfWeek.mask(): Int = 1 shl (value - 1)
