package app.auriel.basalt.core.data.repository

import app.auriel.basalt.core.data.db.AlarmDao
import app.auriel.basalt.core.data.db.AlarmEntity
import app.auriel.basalt.core.data.db.AlarmInstanceDao
import app.auriel.basalt.core.data.db.AlarmInstanceEntity
import app.auriel.basalt.core.data.db.LapDao
import app.auriel.basalt.core.data.db.LapEntity
import app.auriel.basalt.core.data.db.TimerDao
import app.auriel.basalt.core.data.db.TimerEntity
import app.auriel.basalt.core.data.model.Alarm
import app.auriel.basalt.core.data.model.AlarmInstance
import app.auriel.basalt.core.data.model.AlarmInstanceState
import app.auriel.basalt.core.data.model.BedtimeSchedule
import app.auriel.basalt.core.data.model.City
import app.auriel.basalt.core.data.model.Lap
import app.auriel.basalt.core.data.model.Settings
import app.auriel.basalt.core.data.model.Stopwatch
import app.auriel.basalt.core.data.model.Timer
import app.auriel.basalt.core.data.model.TimerState
import app.auriel.basalt.core.data.prefs.BasaltPreferences
import app.auriel.basalt.core.time.TimeSource
import app.auriel.basalt.core.time.Weekdays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class RoomAlarmRepository(private val dao: AlarmDao) : AlarmRepository {
    override val alarms: Flow<List<Alarm>> = dao.observeAll().map { it.map(::toModel) }
    override suspend fun getAll(): List<Alarm> = dao.getAll().map(::toModel)
    override suspend fun get(id: Long): Alarm? = dao.get(id)?.let(::toModel)
    override suspend fun upsert(alarm: Alarm): Long {
        val rowId = dao.upsert(toEntity(alarm))
        // Upsert returns -1 when it updated rather than inserted.
        return if (rowId > 0) rowId else alarm.id
    }
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
    override suspend fun setSkipNext(id: Long, skip: Boolean) = dao.setSkipNext(id, skip)

    private fun toModel(e: AlarmEntity) = Alarm(
        id = e.id,
        time = LocalTime.of(e.hour, e.minute),
        repeatDays = Weekdays(e.repeatBits),
        label = e.label,
        enabled = e.enabled,
        vibrate = e.vibrate,
        ringtoneUri = e.ringtoneUri,
        ringtoneStartMillis = e.ringtoneStartMillis,
        ringtoneEndMillis = e.ringtoneEndMillis,
        skipNext = e.skipNext,
    )

    private fun toEntity(a: Alarm) = AlarmEntity(
        id = a.id,
        hour = a.time.hour,
        minute = a.time.minute,
        repeatBits = a.repeatDays.bits,
        label = a.label,
        enabled = a.enabled,
        vibrate = a.vibrate,
        ringtoneUri = a.ringtoneUri,
        ringtoneStartMillis = a.ringtoneStartMillis,
        ringtoneEndMillis = a.ringtoneEndMillis,
        skipNext = a.skipNext,
    )
}

class RoomAlarmInstanceRepository(
    private val dao: AlarmInstanceDao,
    private val timeSource: TimeSource,
) : AlarmInstanceRepository {
    override val instances: Flow<List<AlarmInstance>> =
        dao.observeAll().map { it.map(::toModel) }

    override suspend fun getAll(): List<AlarmInstance> = dao.getAll().map(::toModel)
    override suspend fun get(id: Long): AlarmInstance? = dao.get(id)?.let(::toModel)
    override suspend fun getForAlarm(alarmId: Long): List<AlarmInstance> =
        dao.getForAlarm(alarmId).map(::toModel)

    override suspend fun insert(instance: AlarmInstance): Long =
        dao.insert(toEntity(instance, timeSource.zone()))

    override suspend fun update(instance: AlarmInstance) =
        dao.update(toEntity(instance, timeSource.zone()))

    override suspend fun setState(id: Long, state: AlarmInstanceState) {
        val existing = dao.get(id) ?: return
        dao.update(existing.copy(state = state.name))
    }

    override suspend fun delete(id: Long) {
        dao.get(id)?.let { dao.delete(it) }
    }

    override suspend fun deleteForAlarm(alarmId: Long) = dao.deleteForAlarm(alarmId)
    override suspend fun deleteAll() = dao.deleteAll()

    private fun toModel(e: AlarmInstanceEntity) = AlarmInstance(
        id = e.id,
        alarmId = e.alarmId,
        firesAt = LocalDateTime.of(e.year, e.month, e.day, e.hour, e.minute),
        state = runCatching { AlarmInstanceState.valueOf(e.state) }
            .getOrDefault(AlarmInstanceState.Scheduled),
    )

    private fun toEntity(i: AlarmInstance, zone: ZoneId) = AlarmInstanceEntity(
        id = i.id,
        alarmId = i.alarmId,
        year = i.firesAt.year,
        month = i.firesAt.monthValue,
        day = i.firesAt.dayOfMonth,
        hour = i.firesAt.hour,
        minute = i.firesAt.minute,
        firesAtEpochMillis = i.firesAt.atZone(zone).toInstant().toEpochMilli(),
        state = i.state.name,
    )
}

class RoomTimerRepository(
    private val dao: TimerDao,
    private val timeSource: TimeSource,
) : TimerRepository {
    override val timers: Flow<List<Timer>> = dao.observeAll().map { it.map(::toModel) }
    override suspend fun getAll(): List<Timer> = dao.getAll().map(::toModel)
    override suspend fun get(id: Long): Timer? = dao.get(id)?.let(::toModel)

    override suspend fun add(timer: Timer): Long =
        dao.upsert(toEntity(timer.copy(createdAtMillis = timeSource.now().toEpochMilli())))

    override suspend fun update(timer: Timer) {
        dao.upsert(toEntity(timer))
    }

    override suspend fun remove(id: Long) = dao.delete(id)

    private fun toModel(e: TimerEntity) = Timer(
        id = e.id,
        totalMillis = e.totalMillis,
        label = e.label,
        state = runCatching { TimerState.valueOf(e.state) }.getOrDefault(TimerState.Reset),
        deadlineWallMillis = e.deadlineWallMillis,
        pausedRemainingMillis = e.pausedRemainingMillis,
        createdAtMillis = e.createdAtMillis,
    )

    private fun toEntity(t: Timer) = TimerEntity(
        id = t.id,
        totalMillis = t.totalMillis,
        label = t.label,
        state = t.state.name,
        deadlineWallMillis = t.deadlineWallMillis,
        pausedRemainingMillis = t.pausedRemainingMillis,
        createdAtMillis = t.createdAtMillis,
    )
}

/**
 * The stopwatch record lives in DataStore and its laps live in Room.
 *
 * The split is on purpose: the record is one small value read on every
 * widget update, and the laps are an unbounded list nobody reads unless the
 * screen is open.
 */
class StoredStopwatchRepository(
    private val prefs: BasaltPreferences,
    private val dao: LapDao,
) : StopwatchRepository {
    override val stopwatch: Flow<Stopwatch> = prefs.stopwatch
    override val laps: Flow<List<Lap>> = dao.observeAll().map { rows ->
        rows.map { Lap(number = it.number, lapMillis = it.lapMillis, totalMillis = it.totalMillis) }
    }

    override suspend fun get(): Stopwatch = prefs.stopwatch.first()
    override suspend fun update(stopwatch: Stopwatch) = prefs.setStopwatch(stopwatch)
    override suspend fun addLap(lap: Lap) =
        dao.insert(LapEntity(number = lap.number, lapMillis = lap.lapMillis, totalMillis = lap.totalMillis))
    override suspend fun lapCount(): Int = dao.count()
    override suspend fun clearLaps() = dao.deleteAll()
}

class StoredSettingsRepository(private val prefs: BasaltPreferences) : SettingsRepository {
    override val settings: Flow<Settings> = prefs.settings
    override suspend fun get(): Settings = prefs.settings.first()
    override suspend fun update(transform: (Settings) -> Settings) = prefs.update(transform)
    override suspend fun setBedtime(schedule: BedtimeSchedule) =
        prefs.update { it.copy(bedtime = schedule) }
}

/**
 * Cities come from a bundled catalogue; only the user's selection is stored.
 *
 * The catalogue is a placeholder until DeskClock's `cities.xml` is imported
 * as data — see NOTICES.MD.
 */
class StoredCityRepository(private val prefs: BasaltPreferences) : CityRepository {
    private val catalogue = listOf(
        City("london", "LONDON", ZoneId.of("Europe/London")),
        City("newyork", "NEW YORK", ZoneId.of("America/New_York")),
        City("losangeles", "LOS ANGELES", ZoneId.of("America/Los_Angeles")),
        City("chennai", "CHENNAI", ZoneId.of("Asia/Kolkata")),
        City("tokyo", "TOKYO", ZoneId.of("Asia/Tokyo")),
        City("sydney", "SYDNEY", ZoneId.of("Australia/Sydney")),
        City("berlin", "BERLIN", ZoneId.of("Europe/Berlin")),
        City("dubai", "DUBAI", ZoneId.of("Asia/Dubai")),
        City("saopaulo", "SAO PAULO", ZoneId.of("America/Sao_Paulo")),
    )

    override val selected: Flow<List<City>> = prefs.selectedCityIds.map { ids ->
        ids.mapNotNull { id -> catalogue.firstOrNull { it.id == id } }
            .mapIndexed { index, city -> city.copy(order = index) }
    }

    override suspend fun all(): List<City> = catalogue

    override suspend fun setSelected(cities: List<City>) =
        prefs.setSelectedCityIds(cities.map(City::id))
}
