package app.auriel.basalt.widget.data

import app.auriel.basalt.core.data.model.City
import app.auriel.basalt.core.data.model.Stopwatch
import app.auriel.basalt.core.data.model.Timer
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Everything any widget can draw, read once.
 *
 * A snapshot rather than a set of flows because a widget is not a screen: it
 * is rendered exactly once per update pass, from a broadcast receiver, in a
 * process that may have been started for the purpose and will be killed
 * shortly afterwards. There is nothing to observe and nothing to recompose.
 *
 * One snapshot serves every installed widget in the pass, which is what
 * makes the coalesced update affordable — ten widgets on the home screen
 * read the database once between them, not ten times.
 *
 * It is a plain data class with no Android in it on purpose: every face is
 * then a pure function of this plus a size, and can be tested as one.
 */
data class WidgetSnapshot(
    /** The instant the pass was taken, on the wall clock. */
    val nowMillis: Long,
    val zone: ZoneId,
    val now: LocalDateTime,
    val use24Hour: Boolean,
    /** Palette id, so the render path matches the app without asking it. */
    val themeId: String,
    /** Lettering id, for the same reason and read at the same time. */
    val uiStyleId: String = "retro",
    val nextAlarm: NextAlarm? = null,
    val timers: List<Timer> = emptyList(),
    val stopwatch: Stopwatch = Stopwatch(),
    val cities: List<City> = emptyList(),
    /** Where the user is, for the dual and world faces. */
    val homeZone: ZoneId = zone,
) {
    /** Timers that are counting, in either direction. */
    val runningTimers: List<Timer> get() = timers.filter { it.isRunning() }

    /**
     * Whether anything on screen is actually moving.
     *
     * This is the gate on the second-by-second cadence: a stopwatch widget
     * with a stopped stopwatch is a static picture, and paying a wake-up a
     * second for a static picture is the mistake the tick budget exists to
     * prevent.
     */
    val hasMotion: Boolean get() = stopwatch.isRunning || runningTimers.isNotEmpty()
}

/**
 * The alarm that rings next, already resolved.
 *
 * Resolved rather than "the alarm list plus a rule", because working out
 * which alarm is next is not trivial — it has to walk repeat masks, honour
 * skip-next and ignore disabled alarms — and doing it in the widget would
 * be a second implementation of something the alarm engine already decides.
 * Two implementations of "when does this ring" is how a widget comes to
 * disagree with the app it is a widget for.
 */
data class NextAlarm(
    val alarmId: Long,
    val firesAt: LocalDateTime,
    val label: String,
    /** True when this alarm repeats, which changes how it is described. */
    val repeating: Boolean,
)
