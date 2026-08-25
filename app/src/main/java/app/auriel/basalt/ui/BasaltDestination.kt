package app.auriel.basalt.ui

import app.auriel.basalt.core.design.BasaltIcon

/**
 * The top-level sections of the app.
 *
 * Order here is the order on screen, and Clock sits in the middle on
 * purpose: it is the section the app opens on and the one reached most
 * often, so it gets the position a thumb finds without looking.
 *
 * Settings is a destination but not a tab. It lives in the top bar, because
 * a screen entered rarely and deliberately should not take a thumb-sized
 * slot from the five that get opened daily.
 */
enum class BasaltDestination(
    val route: String,
    val label: String,
    val icon: BasaltIcon,
) {
    Alarm("alarm", "ALARM", BasaltIcon.Alarm),
    Timer("timer", "TIMER", BasaltIcon.Timer),
    Clock("clock", "CLOCK", BasaltIcon.Clock),
    Stopwatch("stopwatch", "STOPWATCH", BasaltIcon.Stopwatch),
    Bedtime("bedtime", "BEDTIME", BasaltIcon.Bedtime),
    Settings("settings", "SETTINGS", BasaltIcon.Settings),
    ;

    companion object {
        val Start = Clock

        /** What the bottom strip shows, left to right. */
        val Tabs = listOf(Alarm, Timer, Clock, Stopwatch, Bedtime)
    }
}
