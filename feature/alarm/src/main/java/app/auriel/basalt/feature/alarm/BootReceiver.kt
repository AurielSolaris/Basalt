package app.auriel.basalt.feature.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.auriel.basalt.core.notify.BasaltChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Rebuilds the schedule whenever the world moves under it.
 *
 * Pending alarms live in the system's memory, not in the app's, and four
 * ordinary events throw them away or invalidate them:
 *
 * - **Reboot.** Every pending alarm is gone. Without this receiver, an
 *   alarm set on Sunday night simply does not ring on Monday if the phone
 *   restarted in between. This is the single most common way a clock app
 *   fails its user.
 * - **App upgrade.** `MY_PACKAGE_REPLACED` has the same effect as a reboot
 *   for this app's alarms.
 * - **Time zone change.** An alarm is stored as a local time, so flying
 *   somewhere changes which instant it means. Every instance has to be
 *   re-resolved against the new zone.
 * - **Manual clock change.** `TIME_SET` covers the user moving the clock,
 *   and also fires around daylight-saving transitions on many devices.
 *
 * The response to all four is identical and idempotent: rebuild everything.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "rebuilding schedule after ${intent.action}")
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                BasaltChannels.ensure(appContext)
                withTimeoutOrNull(WORK_TIMEOUT_MILLIS) {
                    AlarmScheduler(appContext).rescheduleAll()
                } ?: Log.e(TAG, "timed out rebuilding schedule")
            } catch (t: Throwable) {
                Log.e(TAG, "failed rebuilding schedule", t)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BasaltBootReceiver"
        const val WORK_TIMEOUT_MILLIS = 20_000L
    }
}
