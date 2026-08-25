package app.auriel.basalt.core.time

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/** Wall-clock formatting for the dot-matrix readouts. */
object ClockFormat {

    /** `HH:MM` or `H:MM` plus a separate meridiem — see [meridiem]. */
    fun time(time: LocalTime, use24Hour: Boolean): String =
        if (use24Hour) {
            String.format(Locale.US, "%02d:%02d", time.hour, time.minute)
        } else {
            val hour = when (val h = time.hour % 12) {
                0 -> 12
                else -> h
            }
            String.format(Locale.US, "%d:%02d", hour, time.minute)
        }

    /**
     * `AM` / `PM`, or empty in 24-hour mode. Kept separate from [time] so
     * the readout can render it at a smaller cell size beside the digits.
     */
    fun meridiem(time: LocalTime, use24Hour: Boolean): String =
        if (use24Hour) "" else if (time.hour < 12) "AM" else "PM"

    /** `MON 26 AUG` — the sub-line under the main readout. */
    fun date(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        val day = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)
        val month = date.month.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)
        return "$day ${date.dayOfMonth} $month"
    }
}
