package app.auriel.basalt.core.data.model

import app.auriel.basalt.core.time.Weekdays
import java.time.LocalTime

/**
 * A user-configured alarm: the *rule*, not an occurrence of it.
 *
 * AOSP DeskClock splits the same way — an `Alarm` row describes intent, and
 * an [AlarmInstance] is materialised for each firing so that snoozing or
 * dismissing one morning cannot mutate the schedule itself.
 */
data class Alarm(
    val id: Long = 0L,
    val time: LocalTime,
    val repeatDays: Weekdays = Weekdays.None,
    val label: String = "",
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val ringtoneUri: String? = null,
    /** Suppresses only the next occurrence, leaving the schedule intact. */
    val skipNext: Boolean = false,
)
