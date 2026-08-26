package app.auriel.basalt.feature.stopwatch

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.auriel.basalt.core.data.model.Stopwatch
import app.auriel.basalt.core.notify.BasaltChannels

/**
 * The running-stopwatch notification.
 *
 * Like the timers, this runs no service. `setUsesChronometer` hands the
 * counting to the system, which draws a live figure from a base time on the
 * monotonic clock with no process of ours involved. A stopwatch is not
 * user-critical the way an alarm is, so paying for a foreground service to
 * animate a number the system can animate itself would be pure waste.
 *
 * The consequence to be honest about: because the notification counts from a
 * monotonic base, it is the *display* that stops making sense across a reboot,
 * not the stored value. The stored elapsed time is wall-clock based and stays
 * correct; the notification is simply reposted when the screen is next opened.
 */
object StopwatchNotifications {

    private const val NOTIFICATION_ID = 300_001

    fun show(context: Context, stopwatch: Stopwatch, nowWallMillis: Long) {
        if (!stopwatch.isRunning) {
            cancel(context)
            return
        }
        BasaltChannels.ensure(context)

        val elapsed = stopwatch.elapsedMillis(nowWallMillis)
        val notification = Notification.Builder(context, BasaltChannels.STOPWATCH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Stopwatch")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis() - elapsed)
            .setShowWhen(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent(context),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()

        manager(context)?.notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        manager(context)?.cancel(NOTIFICATION_ID)
    }

    private fun launchIntent(context: Context): Intent =
        context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN)

    private fun manager(context: Context): NotificationManager? =
        context.getSystemService(NotificationManager::class.java)
}
