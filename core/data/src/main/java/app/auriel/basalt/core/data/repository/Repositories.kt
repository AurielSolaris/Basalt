package app.auriel.basalt.core.data.repository

import app.auriel.basalt.core.data.model.Alarm
import app.auriel.basalt.core.data.model.City
import app.auriel.basalt.core.data.model.Lap
import app.auriel.basalt.core.data.model.Stopwatch
import app.auriel.basalt.core.data.model.Timer
import kotlinx.coroutines.flow.Flow

/**
 * The data-layer contracts.
 *
 * v0.1.0 defines the interfaces and ships in-memory implementations so the
 * feature modules can be built and navigated against real types. Room-backed
 * implementations replace them in the persistence pass; nothing above this
 * layer should need to change when they do.
 */
interface AlarmRepository {
    val alarms: Flow<List<Alarm>>
    suspend fun upsert(alarm: Alarm): Long
    suspend fun delete(id: Long)
    suspend fun setEnabled(id: Long, enabled: Boolean)
}

interface TimerRepository {
    val timers: Flow<List<Timer>>
    suspend fun add(timer: Timer): Long
    suspend fun update(timer: Timer)
    suspend fun remove(id: Long)
}

interface StopwatchRepository {
    val stopwatch: Flow<Stopwatch>
    val laps: Flow<List<Lap>>
    suspend fun update(stopwatch: Stopwatch)
    suspend fun addLap(lap: Lap)
    suspend fun clearLaps()
}

interface CityRepository {
    /** Cities the user has pinned to the board, in their chosen order. */
    val selected: Flow<List<City>>

    /** Everything selectable. Backed by the bundled dataset. */
    suspend fun all(): List<City>

    suspend fun setSelected(cities: List<City>)
}
