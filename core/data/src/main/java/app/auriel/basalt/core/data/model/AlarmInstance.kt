package app.auriel.basalt.core.data.model

import java.time.LocalDateTime

/**
 * One scheduled firing of an [Alarm].
 *
 * The state machine is AOSP's, and it is worth keeping intact: the
 * intermediate notification states exist so the user can dismiss an alarm
 * *before* it makes any noise, and [Missed] exists so a phone that was off
 * can still tell the user what it failed to do.
 */
data class AlarmInstance(
    val id: Long = 0L,
    val alarmId: Long,
    val firesAt: LocalDateTime,
    val state: AlarmInstanceState = AlarmInstanceState.Scheduled,
)

enum class AlarmInstanceState {
    /** Materialised, too far out to bother the user about. */
    Scheduled,

    /** Within the quiet pre-alarm window; a low-priority notice is posted. */
    PreAlarm,

    /** Imminent; the notice becomes dismissible and high priority. */
    Imminent,

    /** Ringing now. */
    Firing,

    /** Snoozed; a new firing time has been set. */
    Snoozed,

    /** Rang unheard and timed out, or the device was off when it was due. */
    Missed,

    /** Finished, one way or another. Retained briefly, then reaped. */
    Dismissed,
}
