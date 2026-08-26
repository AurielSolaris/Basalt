package app.auriel.basalt.feature.timer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import app.auriel.basalt.core.data.BasaltGraph
import app.auriel.basalt.core.data.model.Timer
import app.auriel.basalt.core.notify.BasaltChannels

/**
 * Keeps running timers alive without keeping the app alive.
 *
 * There is no service counting down. A timer is a deadline plus a
 * notification, and both are things the system already knows how to hold:
 * `AlarmManager` wakes us at the deadline, and the notification's own
 * chronometer renders the count without any process running at all.
 *
 * That is not a shortcut, it is the correct design. A foreground service
 * ticking once a second to update a number the system can draw itself is
 * pure battery cost, and it dies anyway the moment the OEM decides it has
 * been running too long.
 */
class TimerScheduler(private val context: Context) {

    private val graph = BasaltGraph.get(context)
    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    /** Re-arms every running timer and refreshes their notifications. */
    suspend fun syncAll() {
        BasaltChannels.ensure(context)
        graph.timers.getAll().forEach { timer ->
            if (timer.isRunning()) arm(timer) else cancel(timer.id)
        }
    }

    @SuppressLint("MissingPermission")
    fun arm(timer: Timer) {
        val manager = alarmManager ?: return
        BasaltChannels.ensure(context)

        val fire = PendingIntent.getBroadcast(
            context,
            requestCode(timer.id),
            TimerReceiver.expiredIntent(context, timer.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            manager.canScheduleExactAlarms()
        if (canBeExact) {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timer.deadlineWallMillis,
                fire,
            )
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.deadlineWallMillis, fire)
        }

        showRunning(timer)
    }

    fun cancel(timerId: Long) {
        alarmManager?.let { manager ->
            PendingIntent.getBroadcast(
                context,
                requestCode(timerId),
                TimerReceiver.expiredIntent(context, timerId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ).also {
                manager.cancel(it)
                it.cancel()
            }
        }
        notificationManager()?.cancel(notificationId(timerId))
    }

    /**
     * The running-timer notification.
     *
     * `setUsesChronometer` with `setChronometerCountDown` hands the counting
     * to the system: it draws a live countdown from the base time with no
     * further work from us, so the notification stays correct even when the
     * process has been dead for an hour.
     */
    private fun showRunning(timer: Timer) {
        val remaining = timer.deadlineWallMillis - System.currentTimeMillis()
        val builder = Notification.Builder(context, BasaltChannels.TIMERS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(timer.label.ifBlank { "Timer" })
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            // The chronometer counts against elapsed-realtime, so the
            // wall-clock deadline has to be converted into that frame.
            .setWhen(System.currentTimeMillis() + remaining)
            .setShowWhen(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    requestCode(timer.id) + 1,
                    TimerReceiver.showIntent(context),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Pause",
                    PendingIntent.getBroadcast(
                        context,
                        requestCode(timer.id) + 2,
                        TimerReceiver.pauseIntent(context, timer.id),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).build(),
            )
        notificationManager()?.notify(notificationId(timer.id), builder.build())
    }

    private fun notificationManager(): NotificationManager? =
        context.getSystemService(NotificationManager::class.java)

    companion object {
        fun notificationId(timerId: Long): Int = 100_000 + timerId.toInt()
        private fun requestCode(timerId: Long): Int = 200_000 + (timerId.toInt() * 4)
    }
}
