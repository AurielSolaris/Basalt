package app.auriel.basalt.core.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * Notification channels, created once per process.
 *
 * Channel *settings* belong to the user from the moment a channel first
 * exists — importance, sound and vibration cannot be changed by the app
 * afterwards. So the values here are the only chance to get them right, and
 * getting the firing channel wrong is indistinguishable from the alarm
 * being broken.
 */
object BasaltChannels {

    /** A ringing alarm. Highest importance; the service supplies the sound. */
    const val ALARM_FIRING = "alarm_firing"

    /** The quiet heads-up that an alarm is coming, and can be skipped. */
    const val ALARM_UPCOMING = "alarm_upcoming"

    /** An alarm that has been snoozed, with the time it will return. */
    const val ALARM_SNOOZED = "alarm_snoozed"

    /** An alarm nobody answered. */
    const val ALARM_MISSED = "alarm_missed"

    /** Running and expired timers. */
    const val TIMERS = "timers"

    /** The stopwatch, while it is running. */
    const val STOPWATCH = "stopwatch"

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                ALARM_FIRING,
                "Ringing alarms",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "The full-screen notification for an alarm that is going off."
                // Silent on purpose: the foreground service owns the audio
                // so it can ramp the volume and use the alarm stream. A
                // channel sound here would play a second, uncontrollable
                // one over the top.
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },
        )

        manager.createNotificationChannel(
            NotificationChannel(ALARM_UPCOMING, "Upcoming alarms", NotificationManager.IMPORTANCE_LOW)
                .apply {
                    description = "A reminder shortly before an alarm, with a way to skip it."
                    setShowBadge(false)
                },
        )

        manager.createNotificationChannel(
            NotificationChannel(ALARM_SNOOZED, "Snoozed alarms", NotificationManager.IMPORTANCE_LOW)
                .apply {
                    description = "Shown while an alarm is snoozed."
                    setShowBadge(false)
                },
        )

        manager.createNotificationChannel(
            NotificationChannel(ALARM_MISSED, "Missed alarms", NotificationManager.IMPORTANCE_DEFAULT)
                .apply {
                    description = "Shown when an alarm rang unanswered, or could not ring at all."
                },
        )

        manager.createNotificationChannel(
            NotificationChannel(TIMERS, "Timers", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Running timers, and timers that have finished."
                setSound(null, null)
                setShowBadge(false)
            },
        )

        manager.createNotificationChannel(
            NotificationChannel(STOPWATCH, "Stopwatch", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while the stopwatch is running."
                setSound(null, null)
                setShowBadge(false)
            },
        )
    }
}
