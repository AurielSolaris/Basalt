package app.auriel.basalt.widget.face

import app.auriel.basalt.core.data.model.Timer
import app.auriel.basalt.core.data.model.TimerState
import app.auriel.basalt.core.time.ClockFormat
import app.auriel.basalt.core.time.DurationFormat
import app.auriel.basalt.widget.catalog.BasaltWidget
import app.auriel.basalt.widget.data.NextAlarm
import app.auriel.basalt.widget.data.WidgetSnapshot
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/**
 * What each of the ten faces says.
 *
 * Every function here is pure: snapshot in, [WidgetFace] out, no clock read,
 * no context, no drawing. That is what lets the whole catalogue be tested on
 * the JVM — the interesting bugs in a widget are "it said the wrong thing",
 * not "it drew the dots slightly wrong", and this is the layer where the
 * wrong thing gets said.
 *
 * Everything is upper-cased before it leaves this file. The 5×7 glyph set
 * has no lowercase, so a city named "Kolkata" would render as seven question
 * marks; folding here rather than in the renderer means the content
 * description sees the same string the dots do.
 */
object WidgetFaces {

    fun build(widget: BasaltWidget, snapshot: WidgetSnapshot): WidgetFace = when (widget) {
        BasaltWidget.ANALOG -> analog(snapshot)
        BasaltWidget.DIGITAL -> digital(snapshot)
        BasaltWidget.DUAL -> dual(snapshot)
        BasaltWidget.NEXT_ALARM -> nextAlarm(snapshot)
        BasaltWidget.QUICK_LOOK -> quickLook(snapshot)
        BasaltWidget.SOLAR -> solar(snapshot)
        BasaltWidget.STACKED -> stacked(snapshot)
        BasaltWidget.STOPWATCH -> stopwatch(snapshot)
        BasaltWidget.TIMERS -> timers(snapshot)
        BasaltWidget.WORLD -> world(snapshot)
    }

    // -- the faces --------------------------------------------------------

    private fun analog(s: WidgetSnapshot): WidgetFace {
        val time = s.now.toLocalTime()
        return WidgetFace(
            widget = BasaltWidget.ANALOG,
            hero = FaceHero.Dial(hour = time.hour, minute = time.minute),
            contentDescription = spoken(time, s.use24Hour),
        )
    }

    private fun digital(s: WidgetSnapshot): WidgetFace {
        val time = s.now.toLocalTime()
        val alarm = s.nextAlarm
        return WidgetFace(
            widget = BasaltWidget.DIGITAL,
            hero = FaceHero.Text(clock(time, s.use24Hour)),
            lines = listOfNotNull(
                FaceLine(plain(ClockFormat.date(s.now.toLocalDate())), FaceRole.Label),
                alarm?.let { FaceLine(alarmLine(it, s), FaceRole.Alert) },
            ),
            contentDescription = buildString {
                append(spoken(time, s.use24Hour))
                append(", ").append(ClockFormat.date(s.now.toLocalDate()))
                if (alarm != null) append(", next alarm ").append(alarmSpoken(alarm, s))
            },
        )
    }

    /**
     * Samsung's dual clock: home and away at once, each tinted for its own
     * daylight, so which of the two is a reasonable hour to call is legible
     * without reading either number.
     */
    private fun dual(s: WidgetSnapshot): WidgetFace {
        val homeTime = s.now.toLocalTime()
        val away = s.cities.firstOrNull()
        val awayTime = away?.let { s.now.atZone(s.zone).withZoneSameInstant(it.zoneId).toLocalTime() }

        val rows = listOfNotNull(
            FaceRow(
                leading = "HOME",
                trailing = clock(homeTime, s.use24Hour),
                role = daylightRole(homeTime),
                emphasis = true,
            ),
            if (away != null && awayTime != null) {
                FaceRow(
                    leading = plain(away.name),
                    trailing = clock(awayTime, s.use24Hour),
                    role = daylightRole(awayTime),
                )
            } else {
                null
            },
        )

        val offset = if (away != null) offsetLine(s, away.zoneId) else "ADD A CITY TO PAIR"
        return WidgetFace(
            widget = BasaltWidget.DUAL,
            hero = FaceHero.Rows(rows),
            lines = listOf(FaceLine(offset, FaceRole.Caption)),
            contentDescription = buildString {
                append("Home ").append(spoken(homeTime, s.use24Hour))
                if (away != null && awayTime != null) {
                    append(", ").append(away.name).append(' ').append(spoken(awayTime, s.use24Hour))
                }
            },
        )
    }

    private fun nextAlarm(s: WidgetSnapshot): WidgetFace {
        val alarm = s.nextAlarm
            ?: return WidgetFace(
                widget = BasaltWidget.NEXT_ALARM,
                hero = FaceHero.Text("NO ALARM", FaceRole.Caption),
                contentDescription = "No alarm set",
            )
        return WidgetFace(
            widget = BasaltWidget.NEXT_ALARM,
            hero = FaceHero.Text(alarmLine(alarm, s), FaceRole.Alert),
            lines = listOfNotNull(
                alarm.label.takeIf { it.isNotBlank() }
                    ?.let { FaceLine(plain(it), FaceRole.Label) },
                FaceLine(untilLine(alarm, s.now), FaceRole.Caption),
            ),
            contentDescription = "Next alarm " + alarmSpoken(alarm, s),
        )
    }

    /**
     * Nothing's Quick Look: everything that might matter, on one line, at the
     * cost of none of it being large. The status line is assembled from
     * whatever is true right now and omitted entirely when nothing is.
     */
    private fun quickLook(s: WidgetSnapshot): WidgetFace {
        val time = s.now.toLocalTime()
        val status = buildList {
            s.nextAlarm?.let { add("ALARM " + alarmLine(it, s)) }
            s.runningTimers.firstOrNull()?.let {
                add("TIMER " + DurationFormat.coarse(it.remainingMillis(s.nowMillis)))
            }
            if (s.stopwatch.isRunning) {
                add("LAP " + DurationFormat.coarse(s.stopwatch.elapsedMillis(s.nowMillis)))
            }
        }
        return WidgetFace(
            widget = BasaltWidget.QUICK_LOOK,
            hero = FaceHero.Text(clock(time, s.use24Hour)),
            lines = listOfNotNull(
                FaceLine(plain(ClockFormat.date(s.now.toLocalDate())), FaceRole.Label),
                status.takeIf { it.isNotEmpty() }
                    ?.let { FaceLine(it.joinToString(" / "), if (s.hasMotion) FaceRole.Hot else FaceRole.Caption) },
            ),
            contentDescription = buildString {
                append(spoken(time, s.use24Hour))
                append(", ").append(ClockFormat.date(s.now.toLocalDate()))
                if (status.isNotEmpty()) append(", ").append(status.joinToString(", "))
            },
        )
    }

    /**
     * Nothing's solar clock: where the sun is, rather than what the hour is.
     *
     * The arc is the calendar day, not the daylight — real sunrise and sunset
     * need the astronomical maths that does not exist yet, and a gauge that
     * silently assumes 06:00 and 18:00 everywhere would be wrong by hours at
     * high latitude while looking exactly as confident. A day arc is a claim
     * the app can actually stand behind.
     */
    private fun solar(s: WidgetSnapshot): WidgetFace {
        val time = s.now.toLocalTime()
        val fraction = time.toSecondOfDay().toFloat() / SECONDS_PER_DAY
        return WidgetFace(
            widget = BasaltWidget.SOLAR,
            hero = FaceHero.Gauge(
                fraction = fraction,
                readout = clock(time, s.use24Hour),
                role = daylightRole(time),
            ),
            lines = listOf(
                FaceLine(plain(ClockFormat.date(s.now.toLocalDate())), FaceRole.Label),
                FaceLine(partOfDay(time), FaceRole.Caption),
            ),
            contentDescription = spoken(time, s.use24Hour) + ", " + partOfDay(time).lowercase(Locale.US),
        )
    }

    private fun stacked(s: WidgetSnapshot): WidgetFace {
        val time = s.now.toLocalTime()
        val hour = if (s.use24Hour) time.hour else ((time.hour % 12).takeIf { it != 0 } ?: 12)
        return WidgetFace(
            widget = BasaltWidget.STACKED,
            hero = FaceHero.Stack(
                top = String.format(Locale.US, "%02d", hour),
                bottom = String.format(Locale.US, "%02d", time.minute),
            ),
            lines = listOfNotNull(
                FaceLine(plain(ClockFormat.date(s.now.toLocalDate())), FaceRole.Caption),
                ClockFormat.meridiem(time, s.use24Hour).takeIf { it.isNotEmpty() }
                    ?.let { FaceLine(it, FaceRole.Label) },
            ),
            contentDescription = spoken(time, s.use24Hour),
        )
    }

    private fun stopwatch(s: WidgetSnapshot): WidgetFace {
        val elapsed = s.stopwatch.elapsedMillis(s.nowMillis)
        val running = s.stopwatch.isRunning
        return WidgetFace(
            widget = BasaltWidget.STOPWATCH,
            hero = FaceHero.Gauge(
                // The arc sweeps once a minute, which is the one unit a
                // stopwatch has that a viewer can follow without reading.
                fraction = ((elapsed % 60_000L).coerceAtLeast(0L)).toFloat() / 60_000f,
                readout = DurationFormat.coarse(elapsed),
                role = if (running) FaceRole.Hot else FaceRole.Primary,
            ),
            lines = listOf(
                FaceLine(
                    text = when {
                        running -> "RUNNING"
                        elapsed > 0L -> "PAUSED"
                        else -> "READY"
                    },
                    role = if (running) FaceRole.Hot else FaceRole.Caption,
                ),
            ),
            controls = listOfNotNull(
                FaceControl(WidgetControl.StopwatchToggle, if (running) "STOP" else "START"),
                // Lap is offered only while it is running: a lap button on a
                // stopped stopwatch is a control that does nothing, which is
                // worse than no control at all on a face this small.
                if (running) FaceControl(WidgetControl.StopwatchLap, "LAP") else null,
            ),
            contentDescription = "Stopwatch " + DurationFormat.coarse(elapsed) +
                if (running) ", running" else "",
        )
    }

    /**
     * Google's Timer Starter, turned into a bank.
     *
     * Sorted by what runs out first rather than by creation order: the only
     * question a glance at a timer widget asks is "how long have I got", and
     * the answer is the smallest number on it.
     */
    private fun timers(s: WidgetSnapshot): WidgetFace {
        if (s.timers.isEmpty()) {
            return WidgetFace(
                widget = BasaltWidget.TIMERS,
                hero = FaceHero.Text("NO TIMERS", FaceRole.Caption),
                lines = listOf(FaceLine("TAP TO START ONE", FaceRole.Caption)),
                contentDescription = "No timers running. Tap to start one.",
            )
        }
        val ordered = s.timers.sortedWith(
            compareBy({ !it.isRunning() }, { it.remainingMillis(s.nowMillis) }),
        )
        return WidgetFace(
            widget = BasaltWidget.TIMERS,
            hero = FaceHero.Rows(ordered.map { timerRow(it, s) }),
            contentDescription = ordered.joinToString(", ") { timer ->
                val name = timer.label.ifBlank { "Timer" }
                name + ' ' + DurationFormat.coarse(timer.remainingMillis(s.nowMillis))
            },
        )
    }

    private fun world(s: WidgetSnapshot): WidgetFace {
        if (s.cities.isEmpty()) {
            return WidgetFace(
                widget = BasaltWidget.WORLD,
                hero = FaceHero.Text("NO CITIES", FaceRole.Caption),
                lines = listOf(FaceLine("PICK SOME IN CLOCK", FaceRole.Caption)),
                contentDescription = "No cities on the world board.",
            )
        }
        val instant = s.now.atZone(s.zone).toInstant()
        val rows = s.cities.sortedBy { it.order }.map { city ->
            val there = instant.atZone(city.zoneId).toLocalTime()
            FaceRow(
                leading = plain(city.name),
                trailing = clock(there, s.use24Hour),
                role = daylightRole(there),
            )
        }
        return WidgetFace(
            widget = BasaltWidget.WORLD,
            hero = FaceHero.Rows(rows),
            contentDescription = s.cities.joinToString(", ") { city ->
                city.name + ' ' + spoken(instant.atZone(city.zoneId).toLocalTime(), s.use24Hour)
            },
        )
    }

    // -- shared pieces ----------------------------------------------------

    private fun timerRow(timer: Timer, s: WidgetSnapshot): FaceRow {
        val remaining = timer.remainingMillis(s.nowMillis)
        return FaceRow(
            leading = plain(timer.label.ifBlank { "TIMER" }),
            trailing = DurationFormat.coarse(remaining),
            role = when {
                timer.state == TimerState.Expired || remaining < 0L -> FaceRole.Alert
                timer.isRunning() -> FaceRole.Hot
                timer.state == TimerState.Paused -> FaceRole.Caption
                else -> FaceRole.Primary
            },
            emphasis = timer.isRunning(),
        )
    }

    /**
     * `07:00`, `TMR 07:00` or `TUE 07:00`.
     *
     * A weekday is only worth three cells when it is not obvious, so today
     * and tomorrow are named rather than dated. Beyond that the weekday is
     * the useful token — nobody reads "in 4 days" off a widget, but "SAT"
     * lands immediately.
     */
    fun alarmLine(alarm: NextAlarm, s: WidgetSnapshot): String {
        val time = clock(alarm.firesAt.toLocalTime(), s.use24Hour)
        val today = s.now.toLocalDate()
        return when (alarm.firesAt.toLocalDate()) {
            today -> time
            today.plusDays(1) -> "TMR $time"
            else -> plain(
                alarm.firesAt.dayOfWeek.getDisplayName(
                    java.time.format.TextStyle.SHORT,
                    Locale.US,
                ),
            ) + ' ' + time
        }
    }

    /** `IN 7H 20M`, for the face that has room to say it. */
    fun untilLine(alarm: NextAlarm, now: LocalDateTime): String {
        val minutes = Duration.between(now, alarm.firesAt).toMinutes().coerceAtLeast(0L)
        val hours = minutes / 60
        val rest = minutes % 60
        return if (hours > 0) "IN ${hours}H ${rest}M" else "IN ${rest}M"
    }

    private fun alarmSpoken(alarm: NextAlarm, s: WidgetSnapshot): String {
        val time = spoken(alarm.firesAt.toLocalTime(), s.use24Hour)
        val day = alarm.firesAt.dayOfWeek.getDisplayName(
            java.time.format.TextStyle.FULL,
            Locale.getDefault(),
        )
        return "$day $time" + alarm.label.takeIf { it.isNotBlank() }?.let { ", $it" }.orEmpty()
    }

    private fun offsetLine(s: WidgetSnapshot, zone: java.time.ZoneId): String {
        val instant = s.now.atZone(s.zone).toInstant()
        val here = s.homeZone.rules.getOffset(instant).totalSeconds
        val there = zone.rules.getOffset(instant).totalSeconds
        val delta = (there - here) / 60
        if (delta == 0) return "SAME HOUR AS HOME"
        val sign = if (delta > 0) '+' else '-'
        val absolute = kotlin.math.abs(delta)
        val hours = absolute / 60
        val minutes = absolute % 60
        val body = if (minutes == 0) "${hours}H" else String.format(Locale.US, "%dH %02dM", hours, minutes)
        return "$sign$body FROM HOME"
    }

    /**
     * Day or night, from the wall clock alone.
     *
     * Six to six, and honestly an approximation — the real answer needs
     * sunrise and sunset for a latitude the app does not know. It is the same
     * approximation Samsung's dual clock is doing something better than, and
     * it is worth having anyway: on a world board, "which of these is asleep"
     * is right often enough to be useful and never claims more than a tint.
     */
    private fun daylightRole(time: LocalTime): FaceRole =
        if (time.hour in DAY_START until DAY_END) FaceRole.Primary else FaceRole.Cool

    private fun partOfDay(time: LocalTime): String = when (time.hour) {
        in 0..4 -> "NIGHT"
        in 5..11 -> "MORNING"
        in 12..16 -> "AFTERNOON"
        in 17..20 -> "EVENING"
        else -> "NIGHT"
    }

    private fun clock(time: LocalTime, use24Hour: Boolean): String {
        val body = ClockFormat.time(time, use24Hour)
        val meridiem = ClockFormat.meridiem(time, use24Hour)
        return if (meridiem.isEmpty()) body else "$body $meridiem"
    }

    /** The same reading, for a screen reader rather than for dots. */
    private fun spoken(time: LocalTime, use24Hour: Boolean): String =
        clock(time, use24Hour)

    /**
     * Folds arbitrary text into something the 5×7 set can actually draw.
     *
     * Upper-cased against [Locale.US] rather than the default: a Turkish
     * locale turns a dotted i into one the glyph set does not have, and a
     * widget is not the place to discover that.
     */
    private fun plain(text: String): String = text.uppercase(Locale.US)

    private const val SECONDS_PER_DAY = 86_400f
    private const val DAY_START = 6
    private const val DAY_END = 18
}
