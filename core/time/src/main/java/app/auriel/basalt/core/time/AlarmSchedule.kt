package app.auriel.basalt.core.time

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * When an alarm rule next comes due.
 *
 * Kept as pure functions over `java.time` so the awkward cases — repeat
 * masks, midnight wrap, and the two days a year the local clock is not
 * 24 hours long — are testable without a device.
 */
object AlarmSchedule {

    /**
     * The next local date-time at or after [from] that matches [time] and
     * [days], exclusive of [from] itself.
     *
     * A non-repeating alarm resolves to today if the time has not passed,
     * and tomorrow otherwise. A repeating alarm walks forward up to eight
     * days: eight rather than seven because today may match but already
     * have passed, in which case the answer is today-next-week.
     */
    fun nextOccurrence(
        time: LocalTime,
        days: Weekdays,
        from: LocalDateTime,
    ): LocalDateTime {
        val todayAt = from.toLocalDate().atTime(time)
        if (!days.isRepeating) {
            return if (todayAt.isAfter(from)) todayAt else todayAt.plusDays(1)
        }
        for (offset in 0..7) {
            val candidate = todayAt.plusDays(offset.toLong())
            if (candidate.dayOfWeek in days && candidate.isAfter(from)) return candidate
        }
        // Unreachable while the mask has any bit set, but returning a real
        // value beats throwing inside a scheduler.
        return todayAt.plusDays(7)
    }

    /**
     * Resolves a local date-time to an instant in [zone].
     *
     * This is where daylight saving is actually handled, and the behaviour
     * is `java.time`'s, which is the behaviour a user expects:
     *
     * - **Spring forward.** 02:30 does not exist on the changeover day, so
     *   the alarm is pushed to 03:30 — it rings once, an hour "late" by the
     *   wall clock, which is the only option that is not "never".
     * - **Autumn back.** 02:30 happens twice; the *earlier* offset wins, so
     *   the alarm rings the first time round rather than an hour later.
     *
     * Storing an instant instead would be worse in a different way: the
     * alarm would keep its absolute moment and drift to the wrong local
     * hour, which is exactly what a wake-up alarm must never do.
     */
    fun toEpochMillis(local: LocalDateTime, zone: ZoneId): Long =
        ZonedDateTime.of(local, zone).toInstant().toEpochMilli()
}
