package app.auriel.basalt.core.data.model

import app.auriel.basalt.core.time.Weekdays
import java.time.LocalTime

/**
 * A user-configured alarm: the *rule*, not an occurrence of it.
 *
 * AOSP DeskClock splits the same way — an `Alarm` row describes intent, and
 * an [AlarmInstance] is materialised for each firing, so that snoozing or
 * dismissing one morning cannot mutate the schedule itself.
 */
data class Alarm(
    val id: Long = 0L,
    val time: LocalTime,
    val repeatDays: Weekdays = Weekdays.None,
    val label: String = "",
    val enabled: Boolean = true,
    val vibrate: Boolean = true,

    /**
     * What to play. Null means the device's default alarm sound.
     *
     * May be a `content://` URI from the media store (a system alarm tone)
     * or one the user opened through the document picker (their own MP3 or
     * WAV). In the second case a persistable read permission is taken, so
     * the URI keeps working across reboots.
     */
    val ringtoneUri: String? = null,

    /** Where in the sound to start, in milliseconds. */
    val ringtoneStartMillis: Long = 0L,

    /**
     * Where to stop, in milliseconds. Zero means "play to the end".
     *
     * Basalt trims where the user says, rather than fading in the first
     * thirty seconds and hoping: the good part of a song is rarely at the
     * beginning, and an alarm you chose is one you are more likely to
     * actually get up for.
     */
    val ringtoneEndMillis: Long = 0L,

    /** Suppresses only the next occurrence, leaving the schedule intact. */
    val skipNext: Boolean = false,
) {
    val hasTrim: Boolean get() = ringtoneStartMillis > 0L || ringtoneEndMillis > 0L
}
