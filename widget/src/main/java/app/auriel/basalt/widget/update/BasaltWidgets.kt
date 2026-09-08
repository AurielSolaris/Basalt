package app.auriel.basalt.widget.update

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.glance.appwidget.updateAll
import app.auriel.basalt.widget.catalog.BasaltWidget
import app.auriel.basalt.widget.glance.BasaltGlanceWidget
import app.auriel.basalt.widget.glance.WidgetReceivers

/**
 * The single update pass.
 *
 * Ten widget types, one pending alarm. The alternative — each provider
 * scheduling its own repeat — is how a home screen with six Basalt widgets
 * on it wakes the device six times a minute to draw six pictures of the same
 * clock, and it is the failure the plan named before any of this was
 * written.
 *
 * Every pass re-decides its own successor. That is what removes the need for
 * a screen-state receiver: rather than listening for the screen going dark
 * and switching cadence, each tick asks whether the screen is on *now* and
 * schedules accordingly, so the per-second lane collapses back to per-minute
 * within a second of the phone being pocketed, with nothing to register,
 * unregister or keep alive.
 */
object BasaltWidgets {

    /** Which catalogue entries actually have an instance on a home screen. */
    fun installed(context: Context): List<BasaltWidget> {
        val manager = AppWidgetManager.getInstance(context) ?: return emptyList()
        return WidgetReceivers.byWidget.entries
            .filter { (_, receiver) ->
                val component = ComponentName(context.applicationContext, receiver)
                runCatching { manager.getAppWidgetIds(component) }
                    .getOrNull()
                    ?.isNotEmpty() == true
            }
            .map { it.key }
    }

    /** Redraws every installed widget. The suspending half of a pass. */
    suspend fun refreshNow(context: Context) {
        for (widget in installed(context)) {
            // One failing face must not take the other nine with it. A widget
            // that throws while updating is left showing its last good frame,
            // which is a far better outcome than a receiver crash the user
            // sees as the whole app misbehaving.
            runCatching { BasaltGlanceWidget(widget).updateAll(context) }
        }
    }

    /**
     * Asks for a redraw without waiting for one.
     *
     * The fire-and-forget form, for callers that are not suspending — a
     * button on a widget, or the app noticing a setting changed. It goes
     * through the same receiver as a scheduled tick, so there is exactly one
     * path that draws widgets no matter what prompted it.
     */
    fun refresh(context: Context) {
        val app = context.applicationContext
        app.sendBroadcast(
            Intent(app, WidgetTickReceiver::class.java).setAction(ACTION_TICK),
        )
    }

    /**
     * Puts the next pass on the calendar at the ordinary cadence, or takes it
     * off entirely when nothing is installed.
     *
     * Called when the answer might have changed but nothing is known about
     * what is running: a widget placed or removed, the app's settings
     * edited, a reboot. Deliberately does not try to work out whether a
     * timer is going — that needs a suspending read, and this is called from
     * `onUpdate`, which has to return. The pass that fires a moment later has
     * the snapshot in hand and calls [scheduleNext] with the real answer, so
     * the cost of not knowing here is at most one ordinary tick.
     */
    fun reschedule(context: Context) {
        val app = context.applicationContext
        val cadence = if (installed(app).isEmpty()) {
            WidgetTicks.Cadence.Idle
        } else {
            WidgetTicks.Cadence.Minute
        }
        schedule(app, cadence)
    }

    /**
     * Schedules the pass after this one, knowing whether anything is moving.
     *
     * [hasMotion] comes from the snapshot the pass just took, rather than
     * from a second read: the two would be a fraction of a second apart, and
     * the whole point of a snapshot is that everything in a pass agrees.
     */
    fun scheduleNext(context: Context, hasMotion: Boolean) {
        val app = context.applicationContext
        val power = app.getSystemService(PowerManager::class.java)
        schedule(
            app,
            WidgetTicks.cadence(
                installed = installed(app),
                hasMotion = hasMotion,
                screenOn = power?.isInteractive ?: true,
            ),
        )
    }

    private fun schedule(app: Context, cadence: WidgetTicks.Cadence) {
        val manager = app.getSystemService(AlarmManager::class.java) ?: return
        val pending = tickIntent(app)

        if (cadence == WidgetTicks.Cadence.Idle) {
            manager.cancel(pending)
            return
        }

        val at = WidgetTicks.nextTickMillis(System.currentTimeMillis(), cadence)

        // setExact rather than setExactAndAllowWhileIdle, and deliberately.
        // The allow-while-idle form is rate-limited to roughly one firing
        // every nine minutes in Doze, so asking for it would not buy a
        // per-minute clock anyway — and a widget nobody can see, because the
        // screen has been off for an hour, does not need redrawing. Being
        // deferred by Doze is the right behaviour here rather than a
        // limitation to work around: the pass runs again as the device wakes,
        // which is also when someone first looks at the widget.
        runCatching { manager.setExact(AlarmManager.RTC, at, pending) }
            .onFailure {
                // Exact alarms are user-revocable. Losing them costs accuracy
                // at the minute boundary, not correctness, so the widget
                // degrades to an inexact tick instead of stopping.
                runCatching { manager.set(AlarmManager.RTC, at, pending) }
            }
    }

    private fun tickIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_TICK,
            Intent(context, WidgetTickReceiver::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    const val ACTION_TICK = "app.auriel.basalt.widget.TICK"

    /**
     * Well clear of the alarm engine's block, which allocates eight request
     * codes per alarm instance counting up from zero.
     */
    private const val REQUEST_TICK = 0x7A5A17
}
