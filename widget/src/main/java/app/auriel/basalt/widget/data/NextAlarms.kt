package app.auriel.basalt.widget.data

import app.auriel.basalt.core.data.model.Alarm
import app.auriel.basalt.feature.alarm.AlarmTransitions
import java.time.LocalDateTime

/**
 * Which alarm rings next.
 *
 * Deliberately built on [AlarmTransitions] rather than on a fresh walk of
 * the repeat mask. That object is the alarm engine's answer to "when does
 * this alarm fire", skip-next included, and it is already the thing
 * `AlarmStateManager` schedules from. A widget that worked it out
 * independently would be a second implementation of the same question, and
 * the two would eventually disagree — always on the morning the user cared,
 * because that is the morning they looked at the widget.
 *
 * That is also why `:widget` depends on `:feature:alarm`. The dependency
 * looks backwards for a moment, but the alternative is duplication of the
 * one piece of logic in this app that must not be duplicated.
 */
object NextAlarms {

    /**
     * The soonest occurrence among [alarms], or null when nothing is armed.
     *
     * Disabled alarms are excluded. Ties break on id so a home screen with
     * two alarms set to the same minute does not flicker between them from
     * one update pass to the next.
     */
    fun resolve(alarms: List<Alarm>, from: LocalDateTime): NextAlarm? =
        alarms.asSequence()
            .filter { it.enabled }
            .mapNotNull { alarm ->
                val firing = AlarmTransitions.resolveFiring(alarm, from)
                val firesAt = firing.firesAt ?: return@mapNotNull null
                NextAlarm(
                    alarmId = alarm.id,
                    firesAt = firesAt,
                    label = alarm.label,
                    repeating = alarm.repeatDays.isRepeating,
                )
            }
            .minWithOrNull(compareBy({ it.firesAt }, { it.alarmId }))
}
