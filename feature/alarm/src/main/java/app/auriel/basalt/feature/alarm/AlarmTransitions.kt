package app.auriel.basalt.feature.alarm

import app.auriel.basalt.core.data.model.Alarm
import app.auriel.basalt.core.time.AlarmSchedule
import java.time.LocalDateTime

/**
 * The parts of the alarm lifecycle that are arithmetic rather than Android.
 *
 * [AlarmStateManager] and [AlarmScheduler] are both unavoidably bound to a
 * `Context` — they talk to `AlarmManager`, the notification manager and a
 * database. The decisions they make are not: "what happens to a repeating
 * alarm once this morning's occurrence is over" is a pure function of the
 * rule and the occurrence, and it is also the part that is entangled enough
 * to get wrong.
 *
 * So the decisions live here, where they can be tested without a device,
 * and the two Android classes are left doing only the effects.
 */
object AlarmTransitions {

    /** What to do with an alarm rule once one of its occurrences is over. */
    sealed interface Next {
        /**
         * Materialise the following occurrence, walking forward from [from]
         * — the occurrence that just ended, *not* "now". Dismissing at 07:05
         * an alarm that rang at 07:00 must not re-arm for 07:00 the same
         * morning.
         */
        data class ScheduleFrom(val from: LocalDateTime) : Next

        /** A one-shot has done its one shot. */
        data object DisableAlarm : Next
    }

    /**
     * After an occurrence ends, however it ended.
     *
     * Dismissed, missed and skipped are deliberately the same answer. They
     * differ in what the user is told and in what is cancelled, which is the
     * caller's business; they do not differ in what the schedule should look
     * like afterwards, and pretending they do is how a skipped morning ends
     * up disabling a repeating alarm.
     */
    fun afterOccurrence(alarm: Alarm, occurrenceFiresAt: LocalDateTime): Next =
        if (alarm.repeatDays.isRepeating) {
            Next.ScheduleFrom(occurrenceFiresAt)
        } else {
            Next.DisableAlarm
        }

    /**
     * The occurrence to arm next, honouring [Alarm.skipNext].
     *
     * @param firesAt when to ring, or null when the alarm should be switched
     *   off instead.
     * @param clearSkipNext whether the skip flag has now been consumed.
     * @param disable whether the alarm should be switched off.
     */
    data class Firing(
        val firesAt: LocalDateTime?,
        val clearSkipNext: Boolean,
        val disable: Boolean,
    )

    fun resolveFiring(alarm: Alarm, from: LocalDateTime): Firing {
        val first = AlarmSchedule.nextOccurrence(alarm.time, alarm.repeatDays, from)
        if (!alarm.skipNext) return Firing(first, clearSkipNext = false, disable = false)

        return if (alarm.repeatDays.isRepeating) {
            // Skip this one and take the following occurrence. The flag is
            // consumed here rather than when the skipped time passes, so a
            // phone that was off over the skipped morning still ends up in
            // the right state.
            Firing(
                firesAt = AlarmSchedule.nextOccurrence(alarm.time, alarm.repeatDays, first),
                clearSkipNext = true,
                disable = false,
            )
        } else {
            // A one-shot that is skipped is simply off.
            Firing(firesAt = null, clearSkipNext = true, disable = true)
        }
    }

    /**
     * When a snooze comes due.
     *
     * Seconds are dropped so a snooze started at 07:03:47 rings at 07:13:00
     * rather than 07:13:47 — the readout only shows minutes, and an alarm
     * that rings at a time the screen never displayed reads as a bug.
     */
    fun snoozeAt(now: LocalDateTime, minutes: Int): LocalDateTime =
        now.plusMinutes(minutes.toLong()).withSecond(0).withNano(0)

    /**
     * Request codes for one instance's pending intents.
     *
     * Each instance owns a small block so two instances can never collide,
     * which would otherwise show up as one alarm silently replacing another.
     */
    fun requestCode(instanceId: Long, slot: Int): Int =
        (instanceId.toInt() * SLOTS) + slot

    const val SLOTS = 8

    /** The pending-intent slots within an instance's block. */
    const val SLOT_FIRE = 0
    const val SLOT_UPCOMING = 1
    const val SLOT_SHOW = 2
}
