package app.auriel.basalt.feature.stopwatch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.auriel.basalt.core.data.BasaltGraph
import app.auriel.basalt.core.data.model.Lap
import app.auriel.basalt.core.data.model.Stopwatch
import app.auriel.basalt.core.data.model.StopwatchState
import app.auriel.basalt.core.data.repository.StopwatchRepository
import app.auriel.basalt.core.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StopwatchUiState(
    val stopwatch: Stopwatch = Stopwatch(),
    val elapsedMillis: Long = 0L,
    val laps: List<Lap> = emptyList(),
) {
    val running: Boolean get() = stopwatch.isRunning
    val started: Boolean get() = stopwatch.state != StopwatchState.Reset
}

class StopwatchViewModel(
    private val context: Context,
) : ViewModel() {

    private val graph = BasaltGraph.get(context)
    private val repository: StopwatchRepository = graph.stopwatch
    private val timeSource: TimeSource = graph.timeSource

    private val tick = MutableStateFlow(0L)

    val state: StateFlow<StopwatchUiState> =
        combine(repository.stopwatch, repository.laps, tick) { stopwatch, laps, now ->
            StopwatchUiState(
                stopwatch = stopwatch,
                elapsedMillis = stopwatch.elapsedMillis(now),
                laps = laps,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StopwatchUiState(),
        )

    init {
        viewModelScope.launch {
            while (true) {
                tick.value = timeSource.now().toEpochMilli()
                // Hundredths are displayed, so the readout has to redraw
                // faster than they change or digits visibly skip.
                delay(TICK_MILLIS)
            }
        }
    }

    fun startOrPause() {
        viewModelScope.launch {
            val now = timeSource.now().toEpochMilli()
            val current = repository.stopwatch.first()
            val next = if (current.isRunning) {
                current.copy(
                    state = StopwatchState.Paused,
                    accumulatedMillis = current.elapsedMillis(now),
                    startedAtWallMillis = 0L,
                )
            } else {
                current.copy(state = StopwatchState.Running, startedAtWallMillis = now)
            }
            repository.update(next)
        }
    }

    fun reset() {
        viewModelScope.launch {
            repository.update(Stopwatch())
            repository.clearLaps()
            StopwatchNotifications.cancel(context)
        }
    }

    /**
     * Records a lap.
     *
     * The split is computed against the previous lap's total rather than
     * kept as running state, so a lap taken after a process restart is
     * still correct.
     */
    fun lap() {
        viewModelScope.launch {
            val now = timeSource.now().toEpochMilli()
            val current = repository.stopwatch.first()
            if (!current.isRunning) return@launch
            val total = current.elapsedMillis(now)
            val previousTotal = repository.laps.first().firstOrNull()?.totalMillis ?: 0L
            repository.addLap(
                Lap(
                    number = repository.lapCount() + 1,
                    lapMillis = total - previousTotal,
                    totalMillis = total,
                ),
            )
        }
    }

    private companion object {
        const val TICK_MILLIS = 40L
    }
}
