package app.auriel.basalt.widget.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.auriel.basalt.widget.catalog.BasaltWidget
import app.auriel.basalt.widget.data.WidgetSnapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Where every widget redraw happens.
 *
 * One receiver for the scheduled tick, for a button press, for a reboot and
 * for a time-zone change, because they all want the same thing: draw
 * everything that is installed, then decide when to do it again. Splitting
 * them would mean four places that could each get the rescheduling subtly
 * wrong, and a widget that stops updating is a bug nobody reports — they
 * just stop trusting the widget.
 *
 * The time-related system broadcasts matter more than they look. A time-zone
 * change moves every world and dual face at once, and none of them would
 * otherwise notice until the minute turned; a clock change moves everything.
 */
class WidgetTickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()

        scope.launch {
            try {
                // A broadcast receiver has a hard deadline — around ten
                // seconds — after which the process is killed regardless of
                // what it was doing. Timing out and still rescheduling leaves
                // a stale frame on screen for one pass; being killed mid-pass
                // leaves no next pass at all, and the widgets stop for good.
                withTimeoutOrNull(TIMEOUT_MILLIS) {
                    BasaltWidgets.refreshNow(app)
                }
                BasaltWidgets.scheduleNext(app, hasMotion = motion(app))
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * Whether anything is counting, for the next pass's cadence.
     *
     * Read through the same snapshot path the faces use, asking only for the
     * two things that can move. Failing closed — no motion — is the safe
     * direction: the worst case is a stopwatch widget that updates once a
     * minute for a minute, rather than a device that ticks every second
     * because a database read threw.
     */
    private suspend fun motion(context: Context): Boolean = runCatching {
        val installed = BasaltWidgets.installed(context)
        if (installed.isEmpty()) return false
        val needs = BasaltWidget.needsOf(installed)
        WidgetSnapshots.take(context, needs).hasMotion
    }.getOrDefault(false)

    private companion object {
        // Deliberately not the receiver's own scope: goAsync keeps the
        // process alive, and BasaltGraph is a process singleton, so there is
        // nothing here whose lifetime is shorter than the work.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        const val TIMEOUT_MILLIS = 8_000L
    }
}
