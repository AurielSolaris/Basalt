package app.auriel.basalt.core.data.repository

import app.auriel.basalt.core.data.model.Alarm
import app.auriel.basalt.core.data.model.City
import app.auriel.basalt.core.data.model.Lap
import app.auriel.basalt.core.data.model.Stopwatch
import app.auriel.basalt.core.data.model.Timer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicLong

/**
 * Volatile stand-ins for the real, Room-backed repositories.
 *
 * They exist so the UI can be built and exercised before persistence lands.
 * Nothing here survives process death, and nothing here should ship.
 */
class InMemoryAlarmRepository : AlarmRepository {
    private val ids = AtomicLong(1)
    private val state = MutableStateFlow<List<Alarm>>(emptyList())
    override val alarms: Flow<List<Alarm>> = state.asStateFlow()

    override suspend fun upsert(alarm: Alarm): Long {
        val id = if (alarm.id == 0L) ids.getAndIncrement() else alarm.id
        state.update { current ->
            current.filterNot { it.id == id } + alarm.copy(id = id)
        }
        return id
    }

    override suspend fun delete(id: Long) {
        state.update { current -> current.filterNot { it.id == id } }
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        state.update { current ->
            current.map { if (it.id == id) it.copy(enabled = enabled) else it }
        }
    }
}

class InMemoryTimerRepository : TimerRepository {
    private val ids = AtomicLong(1)
    private val state = MutableStateFlow<List<Timer>>(emptyList())
    override val timers: Flow<List<Timer>> = state.asStateFlow()

    override suspend fun add(timer: Timer): Long {
        val id = ids.getAndIncrement()
        state.update { it + timer.copy(id = id) }
        return id
    }

    override suspend fun update(timer: Timer) {
        state.update { current -> current.map { if (it.id == timer.id) timer else it } }
    }

    override suspend fun remove(id: Long) {
        state.update { current -> current.filterNot { it.id == id } }
    }
}

class InMemoryStopwatchRepository : StopwatchRepository {
    private val stopwatchState = MutableStateFlow(Stopwatch())
    private val lapState = MutableStateFlow<List<Lap>>(emptyList())

    override val stopwatch: Flow<Stopwatch> = stopwatchState.asStateFlow()
    override val laps: Flow<List<Lap>> = lapState.asStateFlow()

    override suspend fun update(stopwatch: Stopwatch) {
        stopwatchState.value = stopwatch
    }

    override suspend fun addLap(lap: Lap) {
        lapState.update { it + lap }
    }

    override suspend fun clearLaps() {
        lapState.value = emptyList()
    }
}

/**
 * A handful of cities so the board is not empty during development. The
 * real dataset is imported from AOSP's `cities.xml` in the data pass.
 */
class InMemoryCityRepository : CityRepository {
    private val catalogue = listOf(
        City("london", "LONDON", ZoneId.of("Europe/London")),
        City("newyork", "NEW YORK", ZoneId.of("America/New_York")),
        City("tokyo", "TOKYO", ZoneId.of("Asia/Tokyo")),
        City("sydney", "SYDNEY", ZoneId.of("Australia/Sydney")),
        City("berlin", "BERLIN", ZoneId.of("Europe/Berlin")),
    )
    private val state = MutableStateFlow(catalogue.take(3))
    override val selected: Flow<List<City>> = state.asStateFlow()

    override suspend fun all(): List<City> = catalogue

    override suspend fun setSelected(cities: List<City>) {
        state.value = cities.mapIndexed { index, city -> city.copy(order = index) }
    }
}
