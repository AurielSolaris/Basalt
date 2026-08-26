package app.auriel.basalt.feature.bedtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.auriel.basalt.core.data.BasaltGraph
import app.auriel.basalt.core.data.model.BedtimeSchedule
import app.auriel.basalt.core.data.repository.SettingsRepository
import app.auriel.basalt.core.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

data class BedtimeUiState(
    val schedule: BedtimeSchedule = BedtimeSchedule(),
    /** Now, as a fraction of the day — drives the gauge needle. */
    val dayFraction: Float = 0f,
    /** Time until wind-down begins, or null when it has already started. */
    val untilWindDown: Duration? = null,
    val insideWindow: Boolean = false,
)

class BedtimeViewModel(
    private val settings: SettingsRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    constructor(graph: BasaltGraph.Graph) : this(graph.settings, graph.timeSource)

    private val tick = MutableStateFlow(LocalDateTime.MIN)

    val state: StateFlow<BedtimeUiState> =
        combine(settings.settings.map { it.bedtime }, tick) { schedule, now ->
            BedtimeUiState(
                schedule = schedule,
                dayFraction = now.toLocalTime().toSecondOfDay() / 86_400f,
                untilWindDown = untilWindDown(schedule, now),
                insideWindow = insideWindow(schedule, now.toLocalTime()),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BedtimeUiState(),
        )

    init {
        viewModelScope.launch {
            while (true) {
                tick.value = LocalDateTime.ofInstant(timeSource.now(), timeSource.zone())
                delay(20_000L)
            }
        }
    }

    fun setEnabled(enabled: Boolean) = edit { it.copy(enabled = enabled) }

    fun adjustBedtime(minutes: Int) = edit {
        it.copy(bedtime = it.bedtime.plusMinutes(minutes.toLong()))
    }

    fun adjustWake(minutes: Int) = edit {
        it.copy(wakeTime = it.wakeTime.plusMinutes(minutes.toLong()))
    }

    fun adjustWindDown(minutes: Int) = edit {
        it.copy(windDownMinutes = (it.windDownMinutes + minutes).coerceIn(0, 120))
    }

    fun toggleDay(day: DayOfWeek) = edit {
        val mask = 1 shl (day.value - 1)
        it.copy(days = it.days xor mask)
    }

    fun isOn(schedule: BedtimeSchedule, day: DayOfWeek): Boolean =
        schedule.days and (1 shl (day.value - 1)) != 0

    private fun edit(transform: (BedtimeSchedule) -> BedtimeSchedule) {
        viewModelScope.launch {
            val current = settings.settings.first().bedtime
            settings.setBedtime(transform(current))
        }
    }

    /**
     * True when now falls between bedtime and wake.
     *
     * The window normally straddles midnight, so the comparison cannot be a
     * plain `in start..end`: when the end is *earlier* in the day than the
     * start, the window is the union of the two ends of the clock.
     */
    private fun insideWindow(schedule: BedtimeSchedule, now: LocalTime): Boolean {
        val start = schedule.windDownAt
        val end = schedule.wakeTime
        return if (start <= end) now >= start && now < end else now >= start || now < end
    }

    private fun untilWindDown(schedule: BedtimeSchedule, now: LocalDateTime): Duration? {
        if (insideWindow(schedule, now.toLocalTime())) return null
        val todayAt = now.toLocalDate().atTime(schedule.windDownAt)
        val next = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
        return Duration.between(now, next)
    }
}
