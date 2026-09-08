package app.auriel.basalt.widget.catalog

/**
 * The widget catalogue.
 *
 * This is the deliverable, more than any individual widget is. The premise
 * of the module is that a widget should be a *configuration* — a row in this
 * enum plus a face function — rather than a new provider, a new layout and a
 * new update path each time. Everything downstream is parameterised by a
 * value from here: one Glance widget class, one renderer, one update pass.
 *
 * The catalogue itself is the union of what three OEMs ship, deduplicated.
 * Google Clock offers Analog, Digital, Stacked, World, Stopwatch and Timer
 * Starter. Samsung Clock offers Analog, Digital, Dual clock and Alarm.
 * Nothing ships a dot-matrix clock, an analog clock and a Quick Look line on
 * the home screen, and a Solar clock and a stopwatch among the Glyph toys.
 * Strip the duplicates and ten distinct ideas remain, which is what this
 * enum holds. Each entry records where the idea came from in [origin] — not
 * for credit, but because "who else shipped this" is the most useful thing
 * to know when deciding what a face should show.
 *
 * Declared alphabetically by [id], and a test enforces it. The picker shows
 * them in declaration order, and a catalogue that grows by accretion ends up
 * in the order the features were written — an order that means something to
 * nobody but the author.
 */
enum class BasaltWidget(
    val id: String,
    val displayName: String,
    val description: String,
    /** Where the idea comes from, for anyone reading the catalogue later. */
    val origin: String,
    val minCells: WidgetCells,
    val defaultCells: WidgetCells,
    val maxCells: WidgetCells,
    /** What this face reads. Everything else is skipped at snapshot time. */
    val needs: Set<WidgetData>,
    val tick: WidgetTick,
    /** Which section of the app a tap opens. */
    val opens: WidgetTarget,
) {
    ANALOG(
        id = "analog",
        displayName = "ANALOG",
        description = "Etched dial and needle hands.",
        origin = "Google Clock, Samsung Clock, Nothing OS",
        minCells = WidgetCells(2, 2),
        defaultCells = WidgetCells(3, 3),
        maxCells = WidgetCells(5, 5),
        needs = setOf(WidgetData.Time),
        tick = WidgetTick.Minute,
        opens = WidgetTarget.Clock,
    ),

    DIGITAL(
        id = "digital",
        displayName = "DIGITAL",
        description = "Dot-matrix time, date beneath, next alarm below that.",
        origin = "Google Clock, Samsung Clock, Nothing OS",
        minCells = WidgetCells(2, 1),
        defaultCells = WidgetCells(4, 2),
        maxCells = WidgetCells(5, 3),
        needs = setOf(WidgetData.Time, WidgetData.Alarms),
        tick = WidgetTick.Minute,
        opens = WidgetTarget.Clock,
    ),

    DUAL(
        id = "dual",
        displayName = "DUAL",
        description = "Home and away side by side, each tinted for its own daylight.",
        origin = "Samsung Clock",
        minCells = WidgetCells(3, 2),
        defaultCells = WidgetCells(4, 2),
        maxCells = WidgetCells(5, 3),
        needs = setOf(WidgetData.Time, WidgetData.Cities),
        tick = WidgetTick.Minute,
        opens = WidgetTarget.Clock,
    ),

    NEXT_ALARM(
        id = "next-alarm",
        displayName = "NEXT ALARM",
        description = "One engraved line: when the next alarm rings, and its label.",
        origin = "Samsung Clock",
        minCells = WidgetCells(3, 1),
        defaultCells = WidgetCells(4, 1),
        maxCells = WidgetCells(5, 2),
        needs = setOf(WidgetData.Time, WidgetData.Alarms),
        tick = WidgetTick.Minute,
        opens = WidgetTarget.Alarm,
    ),

    QUICK_LOOK(
        id = "quick-look",
        displayName = "QUICK LOOK",
        description = "Time, date, next alarm and anything running, on one dense line.",
        origin = "Nothing OS",
        minCells = WidgetCells(4, 1),
        defaultCells = WidgetCells(4, 1),
        maxCells = WidgetCells(5, 2),
        needs = setOf(
            WidgetData.Time,
            WidgetData.Alarms,
            WidgetData.Timers,
            WidgetData.Stopwatch,
        ),
        tick = WidgetTick.SecondWhileRunning,
        opens = WidgetTarget.Clock,
    ),

    SOLAR(
        id = "solar",
        displayName = "SOLAR",
        description = "Where the sun is: a day arc with the current hour marked.",
        origin = "Nothing Glyph Toys",
        minCells = WidgetCells(3, 2),
        defaultCells = WidgetCells(3, 2),
        maxCells = WidgetCells(5, 3),
        needs = setOf(WidgetData.Time),
        tick = WidgetTick.Minute,
        opens = WidgetTarget.Clock,
    ),

    STACKED(
        id = "stacked",
        displayName = "STACKED",
        description = "Hours over minutes, filling a square.",
        origin = "Google Clock",
        minCells = WidgetCells(2, 2),
        defaultCells = WidgetCells(2, 2),
        maxCells = WidgetCells(4, 4),
        needs = setOf(WidgetData.Time),
        tick = WidgetTick.Minute,
        opens = WidgetTarget.Clock,
    ),

    STOPWATCH(
        id = "stopwatch",
        displayName = "STOPWATCH",
        description = "Arc gauge and elapsed time, with start and lap in the widget.",
        origin = "Google Clock, Nothing Glyph Toys",
        minCells = WidgetCells(2, 2),
        defaultCells = WidgetCells(2, 2),
        maxCells = WidgetCells(4, 4),
        needs = setOf(WidgetData.Time, WidgetData.Stopwatch),
        tick = WidgetTick.SecondWhileRunning,
        opens = WidgetTarget.Stopwatch,
    ),

    TIMERS(
        id = "timers",
        displayName = "TIMERS",
        description = "Every timer with its remaining time, soonest first.",
        origin = "Google Clock (Timer Starter)",
        minCells = WidgetCells(3, 2),
        defaultCells = WidgetCells(4, 3),
        maxCells = WidgetCells(5, 4),
        needs = setOf(WidgetData.Time, WidgetData.Timers),
        tick = WidgetTick.SecondWhileRunning,
        opens = WidgetTarget.Timer,
    ),

    WORLD(
        id = "world",
        displayName = "WORLD",
        description = "The chosen cities as a board, each tinted for its own daylight.",
        origin = "Google Clock",
        minCells = WidgetCells(3, 2),
        defaultCells = WidgetCells(4, 3),
        maxCells = WidgetCells(5, 5),
        needs = setOf(WidgetData.Time, WidgetData.Cities),
        tick = WidgetTick.Minute,
        opens = WidgetTarget.Clock,
    ),
    ;

    companion object {
        val all: List<BasaltWidget> = entries.toList()

        fun byId(id: String?): BasaltWidget? = all.firstOrNull { it.id == id }

        /**
         * Everything any installed face could need, unioned.
         *
         * The update pass reads the database once for all installed widgets,
         * so a home screen with only an analog clock on it never opens the
         * timer table.
         */
        fun needsOf(widgets: Collection<BasaltWidget>): Set<WidgetData> =
            widgets.flatMapTo(mutableSetOf()) { it.needs }
    }
}

/** What a face reads. Nothing else is fetched when no installed face wants it. */
enum class WidgetData { Time, Alarms, Timers, Stopwatch, Cities }

/** Which section of the app a widget opens when tapped. */
enum class WidgetTarget(val route: String) {
    Clock("clock"),
    Alarm("alarm"),
    Timer("timer"),
    Stopwatch("stopwatch"),
}

/**
 * How often a face needs redrawing.
 *
 * The budget is the one the plan set: nothing redraws more than once a
 * minute unless something the user can watch is actually moving. A stopwatch
 * widget with no running stopwatch is a static picture, and gets the same
 * cadence as a clock face.
 */
enum class WidgetTick {
    /** Redraw when the minute turns, and not otherwise. */
    Minute,

    /**
     * Once a second, but only while something is running *and* the screen is
     * on. A second-ticking widget behind a dark screen is a battery cost
     * nobody is looking at.
     */
    SecondWhileRunning,
}
