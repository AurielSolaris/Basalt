package app.auriel.basalt.core.data.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Everything the user can change.
 *
 * One immutable snapshot rather than a bag of individually-observed keys:
 * a screen that reads six settings should recompose once when any of them
 * changes, not six times.
 */
data class Settings(
    /**
     * Which palette is installed, by its stable id.
     *
     * A string rather than the enum itself, because this module knows
     * nothing about :core:design and should not have to. The id is the
     * contract between the two; resolving it is the design module's job,
     * and an id it does not recognise falls back rather than failing.
     */
    val themeId: String = "copper",

    /**
     * Which lettering is installed, by its stable id.
     *
     * A second axis rather than more themes: a theme is the colours and a
     * style is the shape of the letters, and all seven themes have to work
     * under either. Stored as a string for the same reason [themeId] is —
     * this module knows nothing about :core:design, and an id it does not
     * recognise is the design module's problem to fall back on.
     */
    val uiStyleId: String = "retro",

    val use24Hour: Boolean = true,
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    val homeZoneId: String? = null,

    /** Minutes a snooze lasts. */
    val snoozeMinutes: Int = 10,

    /**
     * Minutes an unattended alarm rings before it gives up and is recorded
     * as missed. Zero means never give up.
     */
    val silenceAfterMinutes: Int = 15,

    /** Ramp the alarm volume up instead of starting at full. */
    val volumeCrescendo: Boolean = true,

    val vibrateByDefault: Boolean = true,

    /** Show seconds on the main readout. Costs a redraw a second. */
    val showSeconds: Boolean = false,

    val bedtime: BedtimeSchedule = BedtimeSchedule(),

    /**
     * Opt-in extras that reach the network. Off by default and staying that
     * way: a clock that phones home uninvited is not this clock.
     */
    val weatherEnabled: Boolean = false,
)

/**
 * The sleep window.
 *
 * Stored as two wall-clock times plus the days it applies to. Wind-down is
 * derived rather than stored — it is always a fixed lead-in before bedtime,
 * and storing it separately invites the two drifting out of step.
 */
data class BedtimeSchedule(
    val enabled: Boolean = false,
    val bedtime: LocalTime = LocalTime.of(23, 0),
    val wakeTime: LocalTime = LocalTime.of(7, 0),
    val windDownMinutes: Int = 30,
    val days: Int = 0b111_1111,
) {
    val windDownAt: LocalTime get() = bedtime.minusMinutes(windDownMinutes.toLong())

    /** Length of the sleep window, wrapping past midnight. */
    val durationMinutes: Int
        get() {
            val start = bedtime.toSecondOfDay() / 60
            val end = wakeTime.toSecondOfDay() / 60
            return if (end > start) end - start else (24 * 60) - start + end
        }
}
