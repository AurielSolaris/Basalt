package app.auriel.basalt.core.data.repository

import app.auriel.basalt.core.data.model.Alarm
import app.auriel.basalt.core.data.model.AlarmInstance
import app.auriel.basalt.core.data.model.AlarmInstanceState
import app.auriel.basalt.core.data.model.BedtimeSchedule
import app.auriel.basalt.core.data.model.City
import app.auriel.basalt.core.data.model.Lap
import app.auriel.basalt.core.data.model.Settings
import app.auriel.basalt.core.data.model.Stopwatch
import app.auriel.basalt.core.data.model.Timer
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    val alarms: Flow<List<Alarm>>
    suspend fun getAll(): List<Alarm>
    suspend fun get(id: Long): Alarm?
    suspend fun upsert(alarm: Alarm): Long
    suspend fun delete(id: Long)
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun setSkipNext(id: Long, skip: Boolean)
}

interface AlarmInstanceRepository {
    val instances: Flow<List<AlarmInstance>>
    suspend fun getAll(): List<AlarmInstance>
    suspend fun get(id: Long): AlarmInstance?
    suspend fun getForAlarm(alarmId: Long): List<AlarmInstance>
    suspend fun insert(instance: AlarmInstance): Long
    suspend fun update(instance: AlarmInstance)
    suspend fun setState(id: Long, state: AlarmInstanceState)
    suspend fun delete(id: Long)
    suspend fun deleteForAlarm(alarmId: Long)
    suspend fun deleteAll()
}

interface TimerRepository {
    val timers: Flow<List<Timer>>
    suspend fun getAll(): List<Timer>
    suspend fun get(id: Long): Timer?
    suspend fun add(timer: Timer): Long
    suspend fun update(timer: Timer)
    suspend fun remove(id: Long)
}

interface StopwatchRepository {
    val stopwatch: Flow<Stopwatch>
    val laps: Flow<List<Lap>>
    suspend fun get(): Stopwatch
    suspend fun update(stopwatch: Stopwatch)
    suspend fun addLap(lap: Lap)
    suspend fun lapCount(): Int
    suspend fun clearLaps()
}

interface CityRepository {
    val selected: Flow<List<City>>
    suspend fun all(): List<City>
    suspend fun setSelected(cities: List<City>)
}

interface SettingsRepository {
    val settings: Flow<Settings>
    suspend fun get(): Settings
    suspend fun update(transform: (Settings) -> Settings)
    suspend fun setBedtime(schedule: BedtimeSchedule)
}
