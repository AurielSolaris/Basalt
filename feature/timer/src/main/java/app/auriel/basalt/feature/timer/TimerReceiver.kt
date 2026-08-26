package app.auriel.basalt.feature.timer

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import app.auriel.basalt.core.data.BasaltGraph
import app.auriel.basalt.core.data.model.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Timer transitions that arrive from the system or from a notification. */
class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1L)
        val action = intent.action ?: return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                withTimeoutOrNull(10_000L) { handle(appContext, action, timerId) }
            } catch (t: Throwable) {
                Log.e(TAG, "failed handling $action", t)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context, action: String, timerId: Long) {
        val graph = BasaltGraph.get(context)
        val timer = graph.timers.get(timerId) ?: return
        when (action) {
            ACTION_EXPIRED -> {
                // The timer keeps its deadline and moves to Expired rather
                // than stopping, so the UI can go on counting upward.
                graph.timers.update(timer.copy(state = TimerState.Expired))
                TimerAlertService.start(context, timerId)
            }
            ACTION_PAUSE -> {
                graph.timers.update(
                    timer.copy(
                        state = TimerState.Paused,
                        pausedRemainingMillis = timer.remainingMillis(
                            graph.timeSource.now().toEpochMilli(),
                        ),
                    ),
                )
                TimerScheduler(context).cancel(timerId)
            }
            ACTION_STOP -> {
                graph.timers.update(
                    timer.copy(
                        state = TimerState.Reset,
                        pausedRemainingMillis = timer.totalMillis,
                        deadlineWallMillis = 0L,
                    ),
                )
                TimerScheduler(context).cancel(timerId)
                TimerAlertService.stop(context)
            }
        }
    }

    companion object {
        private const val TAG = "BasaltTimerReceiver"

        const val ACTION_EXPIRED = "app.auriel.basalt.action.TIMER_EXPIRED"
        const val ACTION_PAUSE = "app.auriel.basalt.action.TIMER_PAUSE"
        const val ACTION_STOP = "app.auriel.basalt.action.TIMER_STOP"
        const val EXTRA_TIMER_ID = "timer_id"

        fun intentFor(context: Context, action: String, timerId: Long): Intent =
            Intent(action).apply {
                component = ComponentName(context, TimerReceiver::class.java)
                putExtra(EXTRA_TIMER_ID, timerId)
            }

        fun expiredIntent(context: Context, timerId: Long): Intent =
            intentFor(context, ACTION_EXPIRED, timerId)

        fun pauseIntent(context: Context, timerId: Long): Intent =
            intentFor(context, ACTION_PAUSE, timerId)

        fun stopIntent(context: Context, timerId: Long): Intent =
            intentFor(context, ACTION_STOP, timerId)

        fun showIntent(context: Context): Intent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(Intent.ACTION_MAIN)
    }
}
