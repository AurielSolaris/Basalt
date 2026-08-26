package app.auriel.basalt.feature.timer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import app.auriel.basalt.core.data.BasaltGraph
import app.auriel.basalt.core.notify.BasaltChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Rings when a timer reaches zero.
 *
 * Separate from the alarm service on purpose: an expired timer is not an
 * alarm, it does not take over the screen, and it should not inherit the
 * alarm's auto-silence or snooze behaviour.
 */
class TimerAlertService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> begin(intent.getLongExtra(EXTRA_TIMER_ID, -1L))
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun begin(timerId: Long) {
        if (timerId < 0) {
            stopSelf(); return
        }
        BasaltChannels.ensure(this)
        scope.launch {
            val graph = BasaltGraph.get(this@TimerAlertService)
            val timer = graph.timers.get(timerId)
            val label = timer?.label?.takeIf(String::isNotBlank) ?: "Timer"

            val notification = Notification.Builder(this@TimerAlertService, BasaltChannels.TIMERS)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("$label finished")
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        this@TimerAlertService,
                        0,
                        TimerReceiver.showIntent(this@TimerAlertService),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .addAction(
                    Notification.Action.Builder(
                        null,
                        "Stop",
                        PendingIntent.getBroadcast(
                            this@TimerAlertService,
                            1,
                            TimerReceiver.stopIntent(this@TimerAlertService, timerId),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    ).build(),
                )
                .build()

            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        TimerScheduler.notificationId(timerId),
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                    )
                } else {
                    startForeground(TimerScheduler.notificationId(timerId), notification)
                }
            }.onFailure { Log.e(TAG, "could not enter foreground", it) }

            startAudio()
            startVibration()
        }
    }

    private fun startAudio() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return
        runCatching {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@TimerAlertService, uri)
                isLooping = true
                prepare()
                start()
            }
        }.onFailure { Log.e(TAG, "could not play timer tone", it) }
    }

    private fun startVibration() {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        } ?: return
        vibrator = vib
        runCatching {
            vib.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 300, 300), 0),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
    }

    override fun onDestroy() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BasaltTimerAlert"
        private const val ACTION_START = "app.auriel.basalt.action.TIMER_ALERT_START"
        private const val ACTION_STOP = "app.auriel.basalt.action.TIMER_ALERT_STOP"
        private const val EXTRA_TIMER_ID = "timer_id"

        fun start(context: Context, timerId: Long) {
            runCatching {
                context.startForegroundService(
                    Intent(context, TimerAlertService::class.java).apply {
                        action = ACTION_START
                        putExtra(EXTRA_TIMER_ID, timerId)
                    },
                )
            }.onFailure { Log.e(TAG, "could not start timer alert", it) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, TimerAlertService::class.java)) }
        }
    }
}
