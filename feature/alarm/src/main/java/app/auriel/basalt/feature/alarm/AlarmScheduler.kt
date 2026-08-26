package app.auriel.basalt.feature.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import app.auriel.basalt.core.data.BasaltGraph
import app.auriel.basalt.core.data.model.Alarm
import app.auriel.basalt.core.data.model.AlarmInstance
import app.auriel.basalt.core.data.model.AlarmInstanceState
import app.auriel.basalt.core.time.AlarmSchedule
import java.time.LocalDateTime

/**
 * Turns alarm *rules* into alarms the system will actually deliver.
 *
 * The design is deliberately boring and idempotent: [rescheduleAll] can be
 * called at any time, from any entry point, and the result is the same
 * correct set of pending alarms. Boot, timezone change, a manual clock
 * change, an app upgrade, a locale change and every edit of an alarm all
 * funnel into it. Nothing here tries to work out a minimal delta — a clock
 * app that is occasionally wasteful is fine; one that is occasionally wrong
 * is not.
 *
 * Instances are scheduled individually rather than one-at-a-time, so two
 * alarms set for the same minute both ring.
 */
class AlarmScheduler(private val context: Context) {

    private val graph = BasaltGraph.get(context)
    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    /**
     * Rebuilds the whole schedule from the alarm rules.
     *
     * Instances that are mid-flight — firing or snoozed — are left alone.
     * Rebuilding those would cancel a ringing alarm or forget a snooze,
     * and both are worse than a stale row.
     */
    suspend fun rescheduleAll() {
        val now = localNow()
        val alarms = graph.alarms.getAll()
        val existing = graph.alarmInstances.getAll()

        val protectedStates = setOf(AlarmInstanceState.Firing, AlarmInstanceState.Snoozed)
        val liveByAlarm = existing.filter { it.state in protectedStates }.associateBy { it.alarmId }

        // Drop everything that is neither live nor still relevant.
        existing.filter { it.state !in protectedStates }.forEach { instance ->
            cancel(instance)
            val owner = alarms.firstOrNull { it.id == instance.alarmId }
            val stillWanted = owner != null &&
                owner.enabled &&
                instance.firesAt.isAfter(now) &&
                instance.state == AlarmInstanceState.Scheduled
            if (!stillWanted) graph.alarmInstances.delete(instance.id)
        }

        // Anything that was due while the process was dead, and never rang,
        // is a missed alarm rather than something to silently drop.
        existing
            .filter { it.state == AlarmInstanceState.Scheduled && !it.firesAt.isAfter(now) }
            .forEach { AlarmStateManager.markMissed(context, it) }

        alarms.filter { it.enabled }.forEach { alarm ->
            if (liveByAlarm.containsKey(alarm.id)) return@forEach
            scheduleNext(alarm, now)
        }

        // Re-arm whatever survived, in case the pending intents were lost —
        // which is exactly what a reboot does.
        liveByAlarm.values.forEach { instance ->
            if (instance.state == AlarmInstanceState.Snoozed) arm(instance)
        }
    }

    /** Materialises and arms the next occurrence of one alarm. */
    suspend fun scheduleNext(alarm: Alarm, from: LocalDateTime = localNow()) {
        if (!alarm.enabled) return

        var firesAt = AlarmSchedule.nextOccurrence(alarm.time, alarm.repeatDays, from)

        if (alarm.skipNext) {
            if (alarm.repeatDays.isRepeating) {
                // Skip this one and take the following occurrence. The flag
                // is cleared here rather than when the skipped time passes,
                // so a phone that was off over the skipped morning still
                // ends up in the right state.
                firesAt = AlarmSchedule.nextOccurrence(alarm.time, alarm.repeatDays, firesAt)
                graph.alarms.setSkipNext(alarm.id, false)
            } else {
                // A one-shot that is skipped is simply off.
                graph.alarms.setSkipNext(alarm.id, false)
                graph.alarms.setEnabled(alarm.id, false)
                return
            }
        }

        val existing = graph.alarmInstances.getForAlarm(alarm.id)
            .firstOrNull { it.state == AlarmInstanceState.Scheduled }
        if (existing != null && existing.firesAt == firesAt) {
            arm(existing)
            return
        }
        existing?.let {
            cancel(it)
            graph.alarmInstances.delete(it.id)
        }

        val id = graph.alarmInstances.insert(
            AlarmInstance(alarmId = alarm.id, firesAt = firesAt, state = AlarmInstanceState.Scheduled),
        )
        graph.alarmInstances.get(id)?.let(::arm)
    }

    /** Places one instance with the system. */
    @SuppressLint("MissingPermission")
    fun arm(instance: AlarmInstance) {
        val manager = alarmManager ?: return
        val triggerAt = AlarmSchedule.toEpochMillis(instance.firesAt, graph.timeSource.zone())

        val fire = PendingIntent.getBroadcast(
            context,
            requestCode(instance.id, FIRE),
            AlarmReceiver.fireIntent(context, instance.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // setAlarmClock, not setExact.
        //
        // It is the only scheduling call the platform treats as a real
        // alarm clock: it is exempt from Doze deferral, survives App
        // Standby, and puts the alarm icon in the status bar. The show
        // intent is what the user reaches when they tap that icon.
        val show = PendingIntent.getActivity(
            context,
            requestCode(instance.id, SHOW),
            AlarmReceiver.showIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            manager.canScheduleExactAlarms()

        if (canBeExact) {
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), fire)
        } else {
            // Degraded, but not silent: without the exact-alarm permission
            // the best available call still fires in Doze, just with the
            // system free to shift it by a few minutes. Settings tells the
            // user this is happening.
            Log.w(TAG, "No exact alarm permission; falling back to inexact delivery")
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, fire)
        }

        // The quiet heads-up beforehand is inexact on purpose: it is a
        // convenience, and waking the device early for it would be rude.
        val upcomingAt = triggerAt - UPCOMING_LEAD_MILLIS
        if (upcomingAt > System.currentTimeMillis()) {
            manager.set(
                AlarmManager.RTC_WAKEUP,
                upcomingAt,
                PendingIntent.getBroadcast(
                    context,
                    requestCode(instance.id, UPCOMING),
                    AlarmReceiver.upcomingIntent(context, instance.id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
    }

    fun cancel(instance: AlarmInstance) {
        val manager = alarmManager ?: return
        listOf(
            FIRE to AlarmReceiver.fireIntent(context, instance.id),
            UPCOMING to AlarmReceiver.upcomingIntent(context, instance.id),
        ).forEach { (slot, intent) ->
            PendingIntent.getBroadcast(
                context,
                requestCode(instance.id, slot),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ).also {
                manager.cancel(it)
                it.cancel()
            }
        }
    }

    private fun localNow(): LocalDateTime =
        LocalDateTime.ofInstant(graph.timeSource.now(), graph.timeSource.zone())

    private companion object {
        const val TAG = "BasaltScheduler"

        /** How far ahead the "alarm coming up" notice appears. */
        const val UPCOMING_LEAD_MILLIS = 2 * 60 * 60 * 1000L

        // Instance ids are multiplied out so each instance owns a small
        // block of request codes and they cannot collide with each other.
        const val FIRE = 0
        const val UPCOMING = 1
        const val SHOW = 2

        fun requestCode(instanceId: Long, slot: Int): Int =
            (instanceId.toInt() * 8) + slot
    }
}
