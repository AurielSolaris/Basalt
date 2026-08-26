package app.auriel.basalt.feature.alarm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.auriel.basalt.core.data.BasaltGraph
import app.auriel.basalt.core.data.model.Alarm
import app.auriel.basalt.core.data.model.AlarmInstance
import app.auriel.basalt.core.data.model.AlarmInstanceState
import app.auriel.basalt.core.time.Weekdays
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

data class AlarmUiState(
    /** The time being dialled in on the setter. */
    val draft: LocalTime = LocalTime.of(7, 0),
    val alarms: List<Alarm> = emptyList(),
    /** Scheduled and snoozed occurrences, keyed by the alarm they belong to. */
    val instances: Map<Long, AlarmInstance> = emptyMap(),
    val nextAlarmIn: Duration? = null,
)

class AlarmViewModel(
    private val context: Context,
) : ViewModel() {

    private val graph = BasaltGraph.get(context)
    private val scheduler = AlarmScheduler(context)
    private val draft = kotlinx.coroutines.flow.MutableStateFlow(LocalTime.of(7, 0))

    val state: StateFlow<AlarmUiState> =
        combine(
            draft,
            graph.alarms.alarms,
            graph.alarmInstances.instances,
        ) { draftTime, alarms, instances ->
            val byAlarm = instances
                .filter { it.state == AlarmInstanceState.Scheduled || it.state == AlarmInstanceState.Snoozed }
                .sortedBy { it.firesAt }
                .associateBy { it.alarmId }
            AlarmUiState(
                draft = draftTime,
                alarms = alarms,
                instances = byAlarm,
                // Read from the materialised instances rather than
                // recomputed from the rules: the instance is what will
                // actually ring, so it is the only honest answer.
                nextAlarmIn = byAlarm.values.minByOrNull { it.firesAt }?.let {
                    Duration.between(localNow(), it.firesAt).takeIf { d -> !d.isNegative }
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AlarmUiState(),
        )

    init {
        // Catch up on anything that changed while the app was not running.
        viewModelScope.launch { scheduler.rescheduleAll() }
    }

    fun adjustDraft(hours: Int = 0, minutes: Int = 0) {
        draft.value = draft.value.plusHours(hours.toLong()).plusMinutes(minutes.toLong())
    }

    fun addDraftAlarm() {
        val time = draft.value
        edit {
            val id = graph.alarms.upsert(
                Alarm(time = time, vibrate = graph.settings.get().vibrateByDefault),
            )
            graph.alarms.get(id)?.let { scheduler.scheduleNext(it) }
        }
    }

    fun setEnabled(alarm: Alarm, enabled: Boolean) = edit {
        graph.alarms.setEnabled(alarm.id, enabled)
        if (enabled) {
            graph.alarms.get(alarm.id)?.let { scheduler.scheduleNext(it) }
        } else {
            graph.alarmInstances.getForAlarm(alarm.id).forEach {
                scheduler.cancel(it)
                AlarmNotifications.cancelAllFor(context, it.id)
                graph.alarmInstances.delete(it.id)
            }
        }
    }

    fun toggleDay(alarm: Alarm, day: DayOfWeek) = edit {
        val updated = alarm.copy(repeatDays = alarm.repeatDays.toggle(day))
        graph.alarms.upsert(updated)
        // The rule changed, so any occurrence built from the old one is
        // stale: drop it and materialise a fresh one.
        graph.alarmInstances.getForAlarm(alarm.id)
            .filter { it.state == AlarmInstanceState.Scheduled }
            .forEach {
                scheduler.cancel(it)
                graph.alarmInstances.delete(it.id)
            }
        scheduler.scheduleNext(updated)
    }

    fun setSkipNext(alarm: Alarm, skip: Boolean) = edit {
        graph.alarms.setSkipNext(alarm.id, skip)
        graph.alarmInstances.getForAlarm(alarm.id)
            .filter { it.state == AlarmInstanceState.Scheduled }
            .forEach {
                scheduler.cancel(it)
                graph.alarmInstances.delete(it.id)
            }
        graph.alarms.get(alarm.id)?.let { scheduler.scheduleNext(it) }
    }

    fun remove(alarm: Alarm) = edit {
        graph.alarmInstances.getForAlarm(alarm.id).forEach {
            scheduler.cancel(it)
            AlarmNotifications.cancelAllFor(context, it.id)
        }
        graph.alarmInstances.deleteForAlarm(alarm.id)
        graph.alarms.delete(alarm.id)
    }

    private fun edit(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun localNow(): LocalDateTime =
        LocalDateTime.ofInstant(graph.timeSource.now(), graph.timeSource.zone())
}

/** Monday-first initials for the repeat row. */
internal val WeekdayInitials = listOf(
    DayOfWeek.MONDAY to "M",
    DayOfWeek.TUESDAY to "T",
    DayOfWeek.WEDNESDAY to "W",
    DayOfWeek.THURSDAY to "T",
    DayOfWeek.FRIDAY to "F",
    DayOfWeek.SATURDAY to "S",
    DayOfWeek.SUNDAY to "S",
)

internal fun Weekdays.summary(): String = when {
    !isRepeating -> "ONCE"
    this == Weekdays.Everyday -> "EVERY DAY"
    this == Weekdays.Weekdays5 -> "WEEKDAYS"
    this == Weekdays.Weekend -> "WEEKENDS"
    else -> WeekdayInitials.filter { it.first in this }.joinToString(" ") { it.second }
}
