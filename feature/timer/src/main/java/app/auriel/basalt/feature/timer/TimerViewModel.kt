package app.auriel.basalt.feature.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.auriel.basalt.core.data.BasaltRepositories
import app.auriel.basalt.core.data.model.Timer
import app.auriel.basalt.core.data.model.TimerState
import app.auriel.basalt.core.data.repository.TimerRepository
import app.auriel.basalt.core.time.SystemTimeSource
import app.auriel.basalt.core.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What one timer row needs in order to draw itself. */
data class TimerRow(
    val timer: Timer,
    val remainingMillis: Long,
) {
    val expired: Boolean get() = remainingMillis < 0
    val running: Boolean
        get() = timer.state == TimerState.Running || timer.state == TimerState.Expired
}

data class TimerUiState(
    /** The duration being dialled in on the setter, in seconds. */
    val draftSeconds: Int = 5 * 60,
    val rows: List<TimerRow> = emptyList(),
)

class TimerViewModel(
    private val repository: TimerRepository = BasaltRepositories.timers,
    private val timeSource: TimeSource = SystemTimeSource(),
) : ViewModel() {

    private val draft = MutableStateFlow(5 * 60)

    /**
     * A ticker rather than a per-timer coroutine.
     *
     * Remaining time is *derived* from each timer's deadline, never
     * decremented, so a dropped tick loses nothing and a timer that ran
     * while the screen was off is correct the moment it is read again. The
     * tick exists only to make the UI redraw.
     */
    private val tick = MutableStateFlow(timeSource.elapsedRealtimeMillis())

    val state: StateFlow<TimerUiState> =
        combine(draft, repository.timers, tick) { draftSeconds, timers, now ->
            TimerUiState(
                draftSeconds = draftSeconds,
                rows = timers.map { TimerRow(it, it.remainingMillis(now)) },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimerUiState(),
        )

    init {
        viewModelScope.launch {
            while (true) {
                tick.value = timeSource.elapsedRealtimeMillis()
                delay(TICK_MILLIS)
            }
        }
    }

    fun adjustDraft(deltaSeconds: Int) {
        draft.update { current ->
            // Wraps rather than clamping: stepping down from zero should
            // reach 23:59:59, not stall.
            ((current + deltaSeconds) % DAY_SECONDS + DAY_SECONDS) % DAY_SECONDS
        }
    }

    fun addDraftTimer() {
        val seconds = draft.value
        if (seconds <= 0) return
        viewModelScope.launch {
            repository.add(
                Timer(
                    totalMillis = seconds * 1000L,
                    pausedRemainingMillis = seconds * 1000L,
                ),
            )
        }
    }

    fun start(timer: Timer) {
        val remaining = when (timer.state) {
            TimerState.Paused -> timer.pausedRemainingMillis
            else -> timer.totalMillis
        }
        viewModelScope.launch {
            repository.update(
                timer.copy(
                    state = TimerState.Running,
                    deadlineRealtimeMillis = timeSource.elapsedRealtimeMillis() + remaining,
                ),
            )
        }
    }

    fun pause(timer: Timer) {
        viewModelScope.launch {
            repository.update(
                timer.copy(
                    state = TimerState.Paused,
                    pausedRemainingMillis = timer.remainingMillis(timeSource.elapsedRealtimeMillis()),
                ),
            )
        }
    }

    fun reset(timer: Timer) {
        viewModelScope.launch {
            repository.update(
                timer.copy(
                    state = TimerState.Reset,
                    pausedRemainingMillis = timer.totalMillis,
                    deadlineRealtimeMillis = 0L,
                ),
            )
        }
    }

    /** Adds a minute, to a running timer or a paused one alike. */
    fun addMinute(timer: Timer) {
        viewModelScope.launch {
            repository.update(
                when (timer.state) {
                    TimerState.Running, TimerState.Expired -> timer.copy(
                        state = TimerState.Running,
                        deadlineRealtimeMillis = timer.deadlineRealtimeMillis + MINUTE_MILLIS,
                    )

                    else -> timer.copy(
                        pausedRemainingMillis = timer.pausedRemainingMillis + MINUTE_MILLIS,
                    )
                },
            )
        }
    }

    fun remove(timer: Timer) {
        viewModelScope.launch { repository.remove(timer.id) }
    }

    private companion object {
        const val TICK_MILLIS = 200L
        const val MINUTE_MILLIS = 60_000L
        const val DAY_SECONDS = 24 * 60 * 60
    }
}
