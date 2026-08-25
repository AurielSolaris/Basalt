package app.auriel.basalt.feature.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.auriel.basalt.core.data.BasaltRepositories
import app.auriel.basalt.core.data.model.Alarm
import app.auriel.basalt.core.data.repository.AlarmRepository
import app.auriel.basalt.core.time.SystemTimeSource
import app.auriel.basalt.core.time.TimeSource
import app.auriel.basalt.core.time.Weekdays
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

data class AlarmUiState(
    /** The time being dialled in on the setter. */
    val draft: LocalTime = LocalTime.of(7, 0),
    val alarms: List<Alarm> = emptyList(),
    /** Time until the soonest enabled alarm, or null when none is armed. */
    val nextAlarmIn: Duration? = null,
)

class AlarmViewModel(
    private val repository: AlarmRepository = BasaltRepositories.alarms,
    private val timeSource: TimeSource = SystemTimeSource(),
) : ViewModel() {

    private val draft = MutableStateFlow(LocalTime.of(7, 0))

    val state: StateFlow<AlarmUiState> =
        combine(draft, repository.alarms) { draftTime, alarms ->
            val sorted = alarms.sortedWith(compareBy({ it.time }, { it.id }))
            AlarmUiState(
                draft = draftTime,
                alarms = sorted,
                nextAlarmIn = soonest(sorted),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AlarmUiState(),
        )

    fun adjustDraft(hours: Int = 0, minutes: Int = 0) {
        draft.update { it.plusHours(hours.toLong()).plusMinutes(minutes.toLong()) }
    }

    fun addDraftAlarm() {
        val time = draft.value
        viewModelScope.launch { repository.upsert(Alarm(time = time)) }
    }

    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(alarm.id, enabled) }
    }

    fun toggleDay(alarm: Alarm, day: java.time.DayOfWeek) {
        viewModelScope.launch {
            repository.upsert(alarm.copy(repeatDays = alarm.repeatDays.toggle(day)))
        }
    }

    fun remove(alarm: Alarm) {
        viewModelScope.launch { repository.delete(alarm.id) }
    }

    /**
     * How far off the soonest enabled alarm is.
     *
     * Computed here from the rule rather than read from a materialised
     * instance, because instances do not exist yet: the scheduler and its
     * state machine are a later pass. When they land this becomes a read of
     * the next [app.auriel.basalt.core.data.model.AlarmInstance] instead, and this
     * function goes away.
     */
    private fun soonest(alarms: List<Alarm>): Duration? {
        val now = LocalDateTime.ofInstant(timeSource.now(), timeSource.zone())
        return alarms
            .filter { it.enabled }
            .mapNotNull { alarm -> nextOccurrence(alarm, now) }
            .minOrNull()
            ?.let { Duration.between(now, it) }
    }

    private fun nextOccurrence(alarm: Alarm, now: LocalDateTime): LocalDateTime? {
        val todayAt = now.toLocalDate().atTime(alarm.time)
        if (!alarm.repeatDays.isRepeating) {
            return if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
        }
        // Walk forward a week; the first repeat day that is still in the
        // future wins. Starting at today rather than tomorrow matters:
        // an alarm repeating on today that has not yet fired is due today.
        for (offset in 0..7) {
            val candidate = todayAt.plusDays(offset.toLong())
            if (candidate.dayOfWeek in alarm.repeatDays && candidate.isAfter(now)) {
                return candidate
            }
        }
        return null
    }
}

/** Monday-first initials for the repeat row. */
internal val WeekdayInitials = listOf(
    java.time.DayOfWeek.MONDAY to "M",
    java.time.DayOfWeek.TUESDAY to "T",
    java.time.DayOfWeek.WEDNESDAY to "W",
    java.time.DayOfWeek.THURSDAY to "T",
    java.time.DayOfWeek.FRIDAY to "F",
    java.time.DayOfWeek.SATURDAY to "S",
    java.time.DayOfWeek.SUNDAY to "S",
)

internal fun Weekdays.summary(): String = when {
    !isRepeating -> "ONCE"
    this == Weekdays.Everyday -> "EVERY DAY"
    this == Weekdays.Weekdays5 -> "WEEKDAYS"
    this == Weekdays.Weekend -> "WEEKENDS"
    else -> WeekdayInitials.filter { it.first in this }.joinToString(" ") { it.second }
}
