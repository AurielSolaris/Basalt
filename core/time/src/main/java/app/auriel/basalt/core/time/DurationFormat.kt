package app.auriel.basalt.core.time

import java.util.Locale
import kotlin.math.abs

/**
 * Formatting for elapsed and remaining durations.
 *
 * Timers and the stopwatch both need a stable character count so the
 * dot-matrix readout does not reflow while it runs; these formatters pad
 * accordingly and only widen when a field genuinely overflows.
 */
object DurationFormat {

    /** `H:MM:SS` once past an hour, `M:SS` below it. Used by timers. */
    fun coarse(millis: Long): String {
        val negative = millis < 0
        val total = abs(millis) / 1000
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        val body = if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
        return if (negative) "-$body" else body
    }

    /** `MM:SS.hh` — hundredths, as the stopwatch and its laps want. */
    fun precise(millis: Long): String {
        val total = abs(millis)
        val hours = total / 3_600_000
        val minutes = (total % 3_600_000) / 60_000
        val seconds = (total % 60_000) / 1000
        val hundredths = (total % 1000) / 10
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
        } else {
            String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
        }
    }
}
