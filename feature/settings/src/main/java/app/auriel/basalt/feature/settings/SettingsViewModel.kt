package app.auriel.basalt.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.auriel.basalt.core.data.BasaltGraph
import app.auriel.basalt.core.data.model.Settings
import app.auriel.basalt.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {

    constructor(graph: BasaltGraph.Graph) : this(graph.settings)

    val settings: StateFlow<Settings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Settings(),
    )

    fun set24Hour(value: Boolean) = edit { it.copy(use24Hour = value) }
    fun setShowSeconds(value: Boolean) = edit { it.copy(showSeconds = value) }
    fun setVolumeCrescendo(value: Boolean) = edit { it.copy(volumeCrescendo = value) }
    fun setVibrate(value: Boolean) = edit { it.copy(vibrateByDefault = value) }
    fun setWeather(value: Boolean) = edit { it.copy(weatherEnabled = value) }

    fun cycleWeekStart(forward: Boolean) = edit {
        val next = if (forward) it.weekStart.plus(1) else it.weekStart.minus(1)
        it.copy(weekStart = next)
    }

    fun adjustSnooze(minutes: Int) = edit {
        it.copy(snoozeMinutes = (it.snoozeMinutes + minutes).coerceIn(1, 60))
    }

    /** Zero is meaningful here: it means "ring until someone deals with it". */
    fun adjustSilenceAfter(minutes: Int) = edit {
        it.copy(silenceAfterMinutes = (it.silenceAfterMinutes + minutes).coerceIn(0, 60))
    }

    private fun edit(transform: (Settings) -> Settings) {
        viewModelScope.launch { repository.update(transform) }
    }

    companion object {
        fun weekStartLabel(day: DayOfWeek): String = day.name.take(3)
    }
}
