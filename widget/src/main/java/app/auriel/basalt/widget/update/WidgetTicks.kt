package app.auriel.basalt.widget.update

import app.auriel.basalt.widget.catalog.BasaltWidget
import app.auriel.basalt.widget.catalog.WidgetTick

/**
 * When the next update pass should run.
 *
 * Ten widget types on a home screen is a battery story, and the way it goes
 * wrong is obvious in hindsight: each provider schedules its own repeating
 * update, and a phone with six Basalt widgets wakes six times a minute to
 * redraw six pictures of the same clock. So there is exactly one pending
 * alarm for the whole module, and this object decides its cadence from what
 * is actually installed and what is actually moving.
 *
 * All of it is arithmetic, so all of it is testable without a device — which
 * matters more here than usual, because the failure mode of a scheduling bug
 * is a widget that is quietly wrong at 3am and a battery graph nobody can
 * explain.
 */
object WidgetTicks {

    /** How fast the single update pass should repeat. */
    enum class Cadence(val periodMillis: Long) {
        /** Nothing installed. Cancel the alarm rather than idle on it. */
        Idle(0L),

        /** The minute turns. The cadence a clock actually needs. */
        Minute(60_000L),

        /** Something is running and someone is looking at it. */
        Second(1_000L),
    }

    /**
     * The cadence for a home screen holding [installed], given whether
     * anything is running and whether the screen is on.
     *
     * The second-by-second lane needs all three of: a face that can show
     * motion, motion to show, and someone able to see it. Drop any one and
     * a per-second wake-up buys nothing — a stopwatch widget with a stopped
     * stopwatch is a static picture, and a running one behind a dark screen
     * is a picture nobody is looking at.
     */
    fun cadence(
        installed: Collection<BasaltWidget>,
        hasMotion: Boolean,
        screenOn: Boolean,
    ): Cadence {
        if (installed.isEmpty()) return Cadence.Idle
        val wantsSeconds = installed.any { it.tick == WidgetTick.SecondWhileRunning }
        return if (wantsSeconds && hasMotion && screenOn) Cadence.Second else Cadence.Minute
    }

    /**
     * The next boundary strictly after [nowMillis], for [cadence].
     *
     * Aligned to the boundary rather than scheduled as "now + a period", so
     * the readout changes when the minute changes rather than however many
     * seconds after it the last pass happened to land. Drift here is the
     * difference between a widget and the lockscreen disagreeing about what
     * time it is, which reads as a broken widget.
     *
     * Epoch milliseconds work for this because every real zone offset is a
     * whole number of minutes, so minute boundaries are the same instants
     * everywhere. Returns [nowMillis] unchanged for [Cadence.Idle], which
     * has no next tick.
     */
    fun nextTickMillis(nowMillis: Long, cadence: Cadence): Long {
        if (cadence == Cadence.Idle) return nowMillis
        val period = cadence.periodMillis
        return nowMillis - Math.floorMod(nowMillis, period) + period
    }

    /**
     * How long to wait for that boundary.
     *
     * Never negative and never zero: a zero-delay exact alarm rescheduled
     * from its own callback is a spin.
     */
    fun delayToNextTick(nowMillis: Long, cadence: Cadence): Long {
        if (cadence == Cadence.Idle) return 0L
        return (nextTickMillis(nowMillis, cadence) - nowMillis).coerceAtLeast(1L)
    }
}
