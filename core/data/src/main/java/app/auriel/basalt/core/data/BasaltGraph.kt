package app.auriel.basalt.core.data

import android.content.Context
import app.auriel.basalt.core.data.db.BasaltDatabase
import app.auriel.basalt.core.data.prefs.BasaltPreferences
import app.auriel.basalt.core.data.repository.AlarmInstanceRepository
import app.auriel.basalt.core.data.repository.AlarmRepository
import app.auriel.basalt.core.data.repository.CityRepository
import app.auriel.basalt.core.data.repository.RoomAlarmInstanceRepository
import app.auriel.basalt.core.data.repository.RoomAlarmRepository
import app.auriel.basalt.core.data.repository.RoomTimerRepository
import app.auriel.basalt.core.data.repository.SettingsRepository
import app.auriel.basalt.core.data.repository.StopwatchRepository
import app.auriel.basalt.core.data.repository.StoredCityRepository
import app.auriel.basalt.core.data.repository.StoredSettingsRepository
import app.auriel.basalt.core.data.repository.StoredStopwatchRepository
import app.auriel.basalt.core.data.repository.TimerRepository
import app.auriel.basalt.core.time.SystemTimeSource
import app.auriel.basalt.core.time.TimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The object graph.
 *
 * Hand-rolled rather than Hilt. Basalt's graph is a database, a preferences
 * file and six repositories, and every one of them is a process singleton;
 * a dependency-injection framework would add a build step and a set of
 * annotations to express something a lazy object already expresses.
 *
 * The reason it is an object with an [install] rather than a constructor
 * parameter passed down: broadcast receivers and services are created by
 * the system, not by us, and they need the same repositories the UI has.
 * They call [get] with whatever context they were handed.
 *
 * If this grows past a dozen entries, or a second one is needed for tests,
 * that is the signal to reach for Hilt.
 */
object BasaltGraph {

    @Volatile
    private var instance: Graph? = null

    fun get(context: Context): Graph =
        instance ?: synchronized(this) {
            instance ?: Graph(context.applicationContext).also { instance = it }
        }

    class Graph internal constructor(private val context: Context) {
        val timeSource: TimeSource by lazy { SystemTimeSource() }
        val database: BasaltDatabase by lazy { BasaltDatabase.create(context) }
        val preferences: BasaltPreferences by lazy { BasaltPreferences(context) }

        val alarms: AlarmRepository by lazy { RoomAlarmRepository(database.alarms()) }
        val alarmInstances: AlarmInstanceRepository by lazy {
            RoomAlarmInstanceRepository(database.alarmInstances(), timeSource)
        }
        val timers: TimerRepository by lazy { RoomTimerRepository(database.timers(), timeSource) }
        val stopwatch: StopwatchRepository by lazy {
            StoredStopwatchRepository(preferences, database.laps())
        }
        val settings: SettingsRepository by lazy { StoredSettingsRepository(preferences) }
        val cities: CityRepository by lazy { StoredCityRepository(preferences) }

        /**
         * The stored theme id, available without suspending.
         *
         * Exposed on the graph rather than reached for through the
         * repository because the callers are windows, not screens: an
         * activity needs it in `onCreate`, before anything can be collected.
         * See [BasaltPreferences.cachedThemeId].
         */
        val cachedThemeId: String? get() = preferences.cachedThemeId

        /**
         * The stored UI style id, available without suspending.
         *
         * The lettering has the same first-frame problem the palette does,
         * and for the same callers. See [BasaltPreferences.cachedUiStyleId].
         */
        val cachedUiStyleId: String? get() = preferences.cachedUiStyleId

        /** Just the theme id, for the theme host. */
        val themeIds: Flow<String> = settings.settings.map { it.themeId }.distinctUntilChanged()

        /** Just the UI style id, for the theme host. */
        val uiStyleIds: Flow<String> =
            settings.settings.map { it.uiStyleId }.distinctUntilChanged()
    }
}
