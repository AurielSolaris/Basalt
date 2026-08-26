package app.auriel.basalt.feature.alarm

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import app.auriel.basalt.core.data.BasaltGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Where every alarm transition enters the process.
 *
 * The work is suspending — it touches the database — but a broadcast
 * receiver is dead the moment [onReceive] returns. [goAsync] holds the
 * process open until the coroutine finishes, and the timeout is there
 * because a receiver that never calls `finish()` is an ANR waiting to
 * happen. Ten seconds is far longer than a handful of database writes and
 * far shorter than the system's patience.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val instanceId = intent.getLongExtra(EXTRA_INSTANCE_ID, -1L)
        val action = intent.action ?: return
        Log.i(TAG, "action=$action instance=$instanceId")

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                withTimeoutOrNull(WORK_TIMEOUT_MILLIS) {
                    handle(appContext, action, instanceId)
                } ?: Log.e(TAG, "timed out handling $action")
            } catch (t: Throwable) {
                Log.e(TAG, "failed handling $action", t)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context, action: String, instanceId: Long) {
        when (action) {
            ACTION_FIRE -> AlarmStateManager.fire(context, instanceId)
            ACTION_SNOOZE -> AlarmStateManager.snooze(context, instanceId)
            ACTION_DISMISS -> AlarmStateManager.dismiss(context, instanceId)
            ACTION_SKIP -> AlarmStateManager.skipNext(context, instanceId)
            ACTION_MISSED -> {
                BasaltGraph.get(context).alarmInstances.get(instanceId)?.let {
                    AlarmStateManager.markMissed(context, it)
                }
            }
            ACTION_UPCOMING -> {
                val graph = BasaltGraph.get(context)
                val instance = graph.alarmInstances.get(instanceId) ?: return
                AlarmNotifications.showUpcoming(
                    context,
                    instance,
                    graph.alarms.get(instance.alarmId),
                )
            }
            else -> Log.w(TAG, "unknown action $action")
        }
    }

    companion object {
        private const val TAG = "BasaltAlarmReceiver"
        private const val WORK_TIMEOUT_MILLIS = 10_000L

        const val ACTION_FIRE = "app.auriel.basalt.action.ALARM_FIRE"
        const val ACTION_SNOOZE = "app.auriel.basalt.action.ALARM_SNOOZE"
        const val ACTION_DISMISS = "app.auriel.basalt.action.ALARM_DISMISS"
        const val ACTION_SKIP = "app.auriel.basalt.action.ALARM_SKIP"
        const val ACTION_MISSED = "app.auriel.basalt.action.ALARM_MISSED"
        const val ACTION_UPCOMING = "app.auriel.basalt.action.ALARM_UPCOMING"

        const val EXTRA_INSTANCE_ID = "instance_id"

        fun actionIntent(context: Context, action: String, instanceId: Long): Intent =
            Intent(action).apply {
                component = ComponentName(context, AlarmReceiver::class.java)
                putExtra(EXTRA_INSTANCE_ID, instanceId)
            }

        fun fireIntent(context: Context, instanceId: Long): Intent =
            actionIntent(context, ACTION_FIRE, instanceId)

        fun upcomingIntent(context: Context, instanceId: Long): Intent =
            actionIntent(context, ACTION_UPCOMING, instanceId)

        /** Where the status-bar alarm icon leads. */
        fun showIntent(context: Context): Intent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(Intent.ACTION_MAIN)
    }
}
