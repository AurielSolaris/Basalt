package app.auriel.basalt.feature.alarm

import android.content.Context
import android.content.Intent
import android.util.Log
import app.auriel.basalt.core.data.BasaltGraph
import app.auriel.basalt.core.data.model.AlarmInstance
import app.auriel.basalt.core.data.model.AlarmInstanceState
import java.time.LocalDateTime

/**
 * The alarm state machine.
 *
 * Every transition an alarm occurrence can make lives here, and nothing
 * else writes [AlarmInstanceState]. Centralising it is the point: the
 * transitions are entangled — snoozing has to re-arm, dismissing has to
 * schedule the *next* occurrence, and being missed has to do both — and
 * spreading that across a service, an activity and two receivers is how
 * clock apps end up with alarms that ring twice or never again.
 *
 *     Scheduled ─┬─▶ Firing ─┬─▶ Dismissed ─▶ (next occurrence scheduled)
 *                │           ├─▶ Snoozed ───▶ Firing
 *                │           └─▶ Missed ────▶ (next occurrence scheduled)
 *                └─▶ Missed        (device was off, or nobody answered)
 */
object AlarmStateManager {

    private const val TAG = "BasaltAlarmState"

    /** The instance is due now: start ringing. */
    suspend fun fire(context: Context, instanceId: Long) {
        val graph = BasaltGraph.get(context)
        val instance = graph.alarmInstances.get(instanceId) ?: run {
            Log.w(TAG, "fire() for unknown instance $instanceId")
            return
        }
        if (instance.state == AlarmInstanceState.Dismissed) return

        graph.alarmInstances.setState(instanceId, AlarmInstanceState.Firing)
        AlarmNotifications.cancelUpcoming(context, instanceId)
        AlarmService.start(context, instanceId)
    }

    /** Put it off for the configured number of minutes. */
    suspend fun snooze(context: Context, instanceId: Long) {
        val graph = BasaltGraph.get(context)
        val instance = graph.alarmInstances.get(instanceId) ?: return
        val minutes = graph.settings.get().snoozeMinutes

        AlarmService.stop(context)

        val now = LocalDateTime.ofInstant(graph.timeSource.now(), graph.timeSource.zone())
        val snoozed = instance.copy(
            firesAt = now.plusMinutes(minutes.toLong()).withSecond(0).withNano(0),
            state = AlarmInstanceState.Snoozed,
        )
        graph.alarmInstances.update(snoozed)
        AlarmScheduler(context).arm(snoozed)
        AlarmNotifications.showSnoozed(context, snoozed, graph.alarms.get(snoozed.alarmId))
    }

    /** Done with this occurrence; line up the next one. */
    suspend fun dismiss(context: Context, instanceId: Long) {
        val graph = BasaltGraph.get(context)
        val instance = graph.alarmInstances.get(instanceId) ?: return

        AlarmService.stop(context)
        AlarmNotifications.cancelAllFor(context, instanceId)

        val scheduler = AlarmScheduler(context)
        scheduler.cancel(instance)
        graph.alarmInstances.delete(instanceId)

        finishOccurrence(context, instance)
    }

    /**
     * Nobody answered, or the device was off when it was due.
     *
     * A missed alarm is recorded rather than discarded: "your phone did not
     * wake you and did not tell you why" is the worst thing a clock app can
     * do, and a notification is the cheapest possible fix.
     */
    suspend fun markMissed(context: Context, instance: AlarmInstance) {
        val graph = BasaltGraph.get(context)

        AlarmService.stop(context)
        AlarmNotifications.cancelAllFor(context, instance.id)

        val scheduler = AlarmScheduler(context)
        scheduler.cancel(instance)
        graph.alarmInstances.delete(instance.id)

        val alarm = graph.alarms.get(instance.alarmId)
        AlarmNotifications.showMissed(context, instance, alarm)
        finishOccurrence(context, instance)
    }

    /** Skip the next occurrence of the owning alarm without disabling it. */
    suspend fun skipNext(context: Context, instanceId: Long) {
        val graph = BasaltGraph.get(context)
        val instance = graph.alarmInstances.get(instanceId) ?: return
        val alarm = graph.alarms.get(instance.alarmId) ?: return

        AlarmNotifications.cancelAllFor(context, instanceId)
        val scheduler = AlarmScheduler(context)
        scheduler.cancel(instance)
        graph.alarmInstances.delete(instanceId)

        if (alarm.repeatDays.isRepeating) {
            scheduler.scheduleNext(alarm, instance.firesAt)
        } else {
            graph.alarms.setEnabled(alarm.id, false)
        }
    }

    /**
     * What happens after an occurrence is over, however it ended.
     *
     * A repeating alarm gets its next occurrence scheduled from the one
     * that just passed — not from "now" — so a dismissal at 07:05 does not
     * accidentally re-arm for 07:00 the same morning. A one-shot switches
     * itself off, which is the whole of what "one-shot" means.
     */
    private suspend fun finishOccurrence(context: Context, instance: AlarmInstance) {
        val graph = BasaltGraph.get(context)
        val alarm = graph.alarms.get(instance.alarmId) ?: return
        if (alarm.repeatDays.isRepeating) {
            AlarmScheduler(context).scheduleNext(alarm, instance.firesAt)
        } else {
            graph.alarms.setEnabled(alarm.id, false)
        }
    }

    /** Broadcast intents that any surface can send to drive a transition. */
    fun snoozeIntent(context: Context, instanceId: Long): Intent =
        AlarmReceiver.actionIntent(context, AlarmReceiver.ACTION_SNOOZE, instanceId)

    fun dismissIntent(context: Context, instanceId: Long): Intent =
        AlarmReceiver.actionIntent(context, AlarmReceiver.ACTION_DISMISS, instanceId)

    fun skipIntent(context: Context, instanceId: Long): Intent =
        AlarmReceiver.actionIntent(context, AlarmReceiver.ACTION_SKIP, instanceId)
}
