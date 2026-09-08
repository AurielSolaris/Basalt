package app.auriel.basalt.widget.data

import android.content.Context
import app.auriel.basalt.core.data.BasaltGraph
import app.auriel.basalt.core.data.model.Stopwatch
import app.auriel.basalt.widget.catalog.WidgetData
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Reads the world once, for everybody.
 *
 * The update pass unions what every installed widget needs and asks for
 * exactly that, so a home screen holding only an analog clock never opens
 * the timer table and never touches the alarm rows. The saving is real:
 * this runs in a broadcast receiver on a cold process, where opening Room
 * at all is most of the cost of the pass.
 *
 * Everything is read at one [nowMillis], not at whatever time each query
 * happens to complete. A widget where the clock says 10:45 and the countdown
 * beside it was computed at 10:46 is subtly, unfixably wrong, and it only
 * shows up on the pass that straddles a minute boundary — which is every
 * pass, because that is when they are scheduled.
 */
object WidgetSnapshots {

    suspend fun take(context: Context, needs: Set<WidgetData>): WidgetSnapshot {
        val graph = BasaltGraph.get(context)
        val nowMillis = graph.timeSource.now().toEpochMilli()
        val zone = graph.timeSource.zone()
        val now = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(nowMillis),
            zone,
        )
        val settings = graph.settings.get()

        val nextAlarm = if (WidgetData.Alarms in needs) {
            NextAlarms.resolve(graph.alarms.getAll(), now)
        } else {
            null
        }
        val timers = if (WidgetData.Timers in needs) graph.timers.getAll() else emptyList()
        val stopwatch = if (WidgetData.Stopwatch in needs) graph.stopwatch.get() else Stopwatch()
        val cities = if (WidgetData.Cities in needs) graph.cities.selected.first() else emptyList()

        return WidgetSnapshot(
            nowMillis = nowMillis,
            zone = zone,
            now = now,
            use24Hour = settings.use24Hour,
            themeId = settings.themeId,
            uiStyleId = settings.uiStyleId,
            nextAlarm = nextAlarm,
            timers = timers,
            stopwatch = stopwatch,
            cities = cities,
            homeZone = settings.homeZoneId?.let(::zoneOrNull) ?: zone,
        )
    }

    /**
     * A stored zone id that no longer resolves falls back rather than
     * throwing. Zone ids do get retired between tzdata releases, and a
     * widget is the worst place to discover it — the crash happens in a
     * broadcast receiver where nobody sees the dialog and the home screen
     * just stops updating.
     */
    private fun zoneOrNull(id: String): ZoneId? = runCatching { ZoneId.of(id) }.getOrNull()
}
