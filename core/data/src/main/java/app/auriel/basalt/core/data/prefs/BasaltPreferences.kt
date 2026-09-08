package app.auriel.basalt.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.auriel.basalt.core.data.model.BedtimeSchedule
import app.auriel.basalt.core.data.model.Settings
import app.auriel.basalt.core.data.model.Stopwatch
import app.auriel.basalt.core.data.model.StopwatchState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("basalt")

/**
 * Everything that is a value rather than a table: settings, the bedtime
 * schedule, the stopwatch, and which cities are on the board.
 *
 * The stopwatch lives here rather than in Room deliberately. It is a single
 * small record read on every widget update, and opening a database for it
 * would be the most expensive thing a widget does.
 */
class BasaltPreferences(private val context: Context) {

    /**
     * A synchronous mirror of the theme id.
     *
     * DataStore is asynchronous by design, which is right for everything
     * except the first frame after a cold start: the activity window
     * background is painted before any composition runs, and the alarm
     * screen is put in front of someone who is asleep. Neither can wait a
     * flow emission, and both look broken if they spend that frame in the
     * wrong palette.
     *
     * SharedPreferences is the one store on Android that answers
     * immediately, so the theme id — and only the theme id — is written to
     * both. DataStore stays authoritative; this is a cache, and a stale or
     * missing value costs nothing worse than one frame.
     */
    private val mirror by lazy {
        context.getSharedPreferences(MIRROR_FILE, Context.MODE_PRIVATE)
    }

    /** The last stored theme id, available before anything has been read. */
    val cachedThemeId: String?
        get() = runCatching { mirror.getString(MIRROR_THEME_ID, null) }.getOrNull()

    /**
     * The last stored UI style id, available before anything has been read.
     *
     * Mirrored for the same reason the theme is: the alarm screen and the
     * window background are painted before DataStore can answer, and a
     * readout that changes typeface one frame in is as jarring as one that
     * changes colour. Widgets read it too, from a process that may have been
     * started only to draw them.
     */
    val cachedUiStyleId: String?
        get() = runCatching { mirror.getString(MIRROR_UI_STYLE_ID, null) }.getOrNull()

    val settings: Flow<Settings> = context.dataStore.data.map(::readSettings)

    val stopwatch: Flow<Stopwatch> = context.dataStore.data.map { prefs ->
        Stopwatch(
            state = prefs[Keys.StopwatchState]?.let(::stopwatchStateOf) ?: StopwatchState.Reset,
            accumulatedMillis = prefs[Keys.StopwatchAccumulated] ?: 0L,
            startedAtWallMillis = prefs[Keys.StopwatchStartedAtWall] ?: 0L,
        )
    }

    val selectedCityIds: Flow<List<String>> = context.dataStore.data.map { prefs ->
        // Preferences cannot store an ordered list, and order is the point
        // of a world-clock board, so the order is kept as a separate
        // delimited string rather than inferred from the set.
        prefs[Keys.CityOrder]?.split(',')?.filter(String::isNotBlank).orEmpty()
    }

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { prefs ->
            val updated = transform(readSettings(prefs))
            writeSettings(prefs, updated)
            mirror.edit()
                .putString(MIRROR_THEME_ID, updated.themeId)
                .putString(MIRROR_UI_STYLE_ID, updated.uiStyleId)
                .apply()
        }
    }

    suspend fun setStopwatch(stopwatch: Stopwatch) {
        context.dataStore.edit { prefs ->
            prefs[Keys.StopwatchState] = stopwatch.state.name
            prefs[Keys.StopwatchAccumulated] = stopwatch.accumulatedMillis
            prefs[Keys.StopwatchStartedAtWall] = stopwatch.startedAtWallMillis
        }
    }

    suspend fun setSelectedCityIds(ids: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CityOrder] = ids.joinToString(",")
            prefs[Keys.CityIds] = ids.toSet()
        }
    }

    private fun stopwatchStateOf(name: String): StopwatchState =
        runCatching { StopwatchState.valueOf(name) }.getOrDefault(StopwatchState.Reset)

    private fun readSettings(prefs: Preferences) = Settings(
        themeId = prefs[Keys.ThemeId] ?: Settings().themeId,
        uiStyleId = prefs[Keys.UiStyleId] ?: Settings().uiStyleId,
        use24Hour = prefs[Keys.Use24Hour] ?: true,
        weekStart = prefs[Keys.WeekStart]?.let { DayOfWeek.of(it) } ?: DayOfWeek.MONDAY,
        homeZoneId = prefs[Keys.HomeZone],
        snoozeMinutes = prefs[Keys.SnoozeMinutes] ?: 10,
        silenceAfterMinutes = prefs[Keys.SilenceAfterMinutes] ?: 15,
        volumeCrescendo = prefs[Keys.VolumeCrescendo] ?: true,
        vibrateByDefault = prefs[Keys.VibrateByDefault] ?: true,
        showSeconds = prefs[Keys.ShowSeconds] ?: false,
        weatherEnabled = prefs[Keys.WeatherEnabled] ?: false,
        bedtime = BedtimeSchedule(
            enabled = prefs[Keys.BedtimeEnabled] ?: false,
            bedtime = prefs[Keys.BedtimeAt]?.let(LocalTime::ofSecondOfDay)
                ?: LocalTime.of(23, 0),
            wakeTime = prefs[Keys.WakeAt]?.let(LocalTime::ofSecondOfDay)
                ?: LocalTime.of(7, 0),
            windDownMinutes = prefs[Keys.WindDownMinutes] ?: 30,
            days = prefs[Keys.BedtimeDays] ?: 0b111_1111,
        ),
    )

    private fun writeSettings(prefs: androidx.datastore.preferences.core.MutablePreferences, s: Settings) {
        prefs[Keys.ThemeId] = s.themeId
        prefs[Keys.UiStyleId] = s.uiStyleId
        prefs[Keys.Use24Hour] = s.use24Hour
        prefs[Keys.WeekStart] = s.weekStart.value
        s.homeZoneId?.let { prefs[Keys.HomeZone] = it }
        prefs[Keys.SnoozeMinutes] = s.snoozeMinutes
        prefs[Keys.SilenceAfterMinutes] = s.silenceAfterMinutes
        prefs[Keys.VolumeCrescendo] = s.volumeCrescendo
        prefs[Keys.VibrateByDefault] = s.vibrateByDefault
        prefs[Keys.ShowSeconds] = s.showSeconds
        prefs[Keys.WeatherEnabled] = s.weatherEnabled
        prefs[Keys.BedtimeEnabled] = s.bedtime.enabled
        prefs[Keys.BedtimeAt] = s.bedtime.bedtime.toSecondOfDay().toLong()
        prefs[Keys.WakeAt] = s.bedtime.wakeTime.toSecondOfDay().toLong()
        prefs[Keys.WindDownMinutes] = s.bedtime.windDownMinutes
        prefs[Keys.BedtimeDays] = s.bedtime.days
    }

    private object Keys {
        val ThemeId = stringPreferencesKey("theme_id")
        val UiStyleId = stringPreferencesKey("ui_style_id")
        val Use24Hour = booleanPreferencesKey("use_24_hour")
        val WeekStart = intPreferencesKey("week_start")
        val HomeZone = stringPreferencesKey("home_zone")
        val SnoozeMinutes = intPreferencesKey("snooze_minutes")
        val SilenceAfterMinutes = intPreferencesKey("silence_after_minutes")
        val VolumeCrescendo = booleanPreferencesKey("volume_crescendo")
        val VibrateByDefault = booleanPreferencesKey("vibrate_by_default")
        val ShowSeconds = booleanPreferencesKey("show_seconds")
        val WeatherEnabled = booleanPreferencesKey("weather_enabled")

        val BedtimeEnabled = booleanPreferencesKey("bedtime_enabled")
        val BedtimeAt = longPreferencesKey("bedtime_at")
        val WakeAt = longPreferencesKey("wake_at")
        val WindDownMinutes = intPreferencesKey("wind_down_minutes")
        val BedtimeDays = intPreferencesKey("bedtime_days")

        val StopwatchState = stringPreferencesKey("stopwatch_state")
        val StopwatchAccumulated = longPreferencesKey("stopwatch_accumulated")
        val StopwatchStartedAtWall = longPreferencesKey("stopwatch_started_at_wall")

        val CityIds = stringSetPreferencesKey("city_ids")
        val CityOrder = stringPreferencesKey("city_order")
    }

    private companion object {
        const val MIRROR_FILE = "basalt_boot"
        const val MIRROR_THEME_ID = "theme_id"
        const val MIRROR_UI_STYLE_ID = "ui_style_id"
    }
}
