package app.auriel.basalt.feature.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import app.auriel.basalt.core.data.model.Alarm
import app.auriel.basalt.core.data.model.AlarmInstance
import app.auriel.basalt.core.notify.BasaltChannels

/**
 * The notifications an alarm produces.
 *
 * Ids are derived from the instance id so a given occurrence can only ever
 * own one notification of each kind, and cancelling is exact.
 */
object AlarmNotifications {

    fun firing(context: Context, instance: AlarmInstance, alarm: Alarm?): Notification {
        BasaltChannels.ensure(context)
        val label = alarm?.label?.takeIf(String::isNotBlank) ?: "Alarm"

        val fullScreen = PendingIntent.getActivity(
            context,
            code(instance.id, FULLSCREEN),
            AlarmActivity.intent(context, instance.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(context, BasaltChannels.ALARM_FIRING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label)
            .setContentText(instance.firesAt.toLocalTime().hhmm())
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            // The whole point of the firing notification: on a locked or
            // sleeping device this is what actually brings the alarm screen
            // up. The notification itself is the fallback for when the
            // system declines to launch it.
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Snooze",
                    broadcast(context, instance.id, SNOOZE, AlarmStateManager.snoozeIntent(context, instance.id)),
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Dismiss",
                    broadcast(context, instance.id, DISMISS, AlarmStateManager.dismissIntent(context, instance.id)),
                ).build(),
            )
            .build()
    }

    fun showUpcoming(context: Context, instance: AlarmInstance, alarm: Alarm?) {
        BasaltChannels.ensure(context)
        val label = alarm?.label?.takeIf(String::isNotBlank) ?: "Alarm"
        val notification = Notification.Builder(context, BasaltChannels.ALARM_UPCOMING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$label at " + instance.firesAt.toLocalTime().hhmm())
            .setContentText("Upcoming")
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(false)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Skip",
                    broadcast(context, instance.id, SKIP, AlarmStateManager.skipIntent(context, instance.id)),
                ).build(),
            )
            .build()
        manager(context)?.notify(id(instance.id, UPCOMING), notification)
    }

    fun showSnoozed(context: Context, instance: AlarmInstance, alarm: Alarm?) {
        BasaltChannels.ensure(context)
        val label = alarm?.label?.takeIf(String::isNotBlank) ?: "Alarm"
        val notification = Notification.Builder(context, BasaltChannels.ALARM_SNOOZED)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$label snoozed")
            .setContentText("Rings again at " + instance.firesAt.toLocalTime().hhmm())
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Dismiss",
                    broadcast(context, instance.id, DISMISS, AlarmStateManager.dismissIntent(context, instance.id)),
                ).build(),
            )
            .build()
        manager(context)?.notify(id(instance.id, SNOOZED), notification)
    }

    fun showMissed(context: Context, instance: AlarmInstance, alarm: Alarm?) {
        BasaltChannels.ensure(context)
        val label = alarm?.label?.takeIf(String::isNotBlank) ?: "Alarm"
        val notification = Notification.Builder(context, BasaltChannels.ALARM_MISSED)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Missed alarm")
            .setContentText("$label at " + instance.firesAt.toLocalTime().hhmm())
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        manager(context)?.notify(id(instance.id, MISSED), notification)
    }

    fun cancelUpcoming(context: Context, instanceId: Long) {
        manager(context)?.cancel(id(instanceId, UPCOMING))
    }

    fun cancelAllFor(context: Context, instanceId: Long) {
        val manager = manager(context) ?: return
        listOf(UPCOMING, SNOOZED, FIRING).forEach { manager.cancel(id(instanceId, it)) }
    }

    fun firingId(instanceId: Long): Int = id(instanceId, FIRING)

    private fun manager(context: Context): NotificationManager? =
        context.getSystemService(NotificationManager::class.java)

    private fun broadcast(
        context: Context,
        instanceId: Long,
        slot: Int,
        intent: android.content.Intent,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        code(instanceId, slot),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun java.time.LocalTime.hhmm(): String = "%02d:%02d".format(hour, minute)

    // Notification ids and pending-intent request codes are both derived
    // from the instance, in separate blocks, so neither can collide.
    private fun id(instanceId: Long, slot: Int): Int = (instanceId.toInt() * 16) + slot
    private fun code(instanceId: Long, slot: Int): Int = (instanceId.toInt() * 16) + 8 + slot

    private const val UPCOMING = 0
    private const val SNOOZED = 1
    private const val MISSED = 2
    private const val FIRING = 3

    private const val FULLSCREEN = 0
    private const val SNOOZE = 1
    private const val DISMISS = 2
    private const val SKIP = 3
}
