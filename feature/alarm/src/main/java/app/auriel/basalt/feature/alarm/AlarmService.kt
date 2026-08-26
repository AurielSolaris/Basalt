package app.auriel.basalt.feature.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import app.auriel.basalt.core.data.BasaltGraph
import app.auriel.basalt.core.data.model.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Makes the noise.
 *
 * A foreground service rather than a plain one: an alarm has to keep
 * playing while the screen is off and the app is nowhere near the
 * foreground, and only a foreground service is allowed to. The type is
 * `mediaPlayback` because that is exactly what it is doing.
 *
 * The service owns the audio, not the notification channel, so that volume
 * can be ramped, the alarm stream can be used explicitly, and a trimmed
 * section of a longer track can be looped.
 */
class AlarmService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var autoSilence: Job? = null
    private var trimLoop: Job? = null
    private var instanceId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val id = intent.getLongExtra(EXTRA_INSTANCE_ID, -1L)
                if (id >= 0) begin(id)
            }
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        // Not sticky: if the system kills this service the alarm state is
        // still in the database, and restarting a bare service with no
        // intent would leave it running silently forever.
        return START_NOT_STICKY
    }

    private fun begin(id: Long) {
        instanceId = id
        scope.launch {
            val graph = BasaltGraph.get(this@AlarmService)
            val instance = graph.alarmInstances.get(id) ?: run {
                stopSelf(); return@launch
            }
            val alarm = graph.alarms.get(instance.alarmId)
            val settings = graph.settings.get()

            startForegroundSafely(
                AlarmNotifications.firingId(id),
                AlarmNotifications.firing(this@AlarmService, instance, alarm),
            )
            showAlarmScreen(id)

            startAudio(alarm, settings.volumeCrescendo)
            if (alarm?.vibrate ?: settings.vibrateByDefault) startVibration()

            val silenceAfter = settings.silenceAfterMinutes
            if (silenceAfter > 0) {
                autoSilence = scope.launch {
                    delay(silenceAfter * 60_000L)
                    // Nobody came. Record it rather than just going quiet.
                    AlarmStateManager.markMissed(this@AlarmService, instance)
                }
            }
        }
    }

    /**
     * Brings the alarm screen up.
     *
     * The full-screen intent on the notification is the primary route, and it
     * is the one that works from a locked or sleeping device. It is also the
     * one Android will quietly refuse: a full-screen intent is suppressed
     * whenever the device is unlocked and no heads-up is shown — which is
     * exactly what happens when the posting app is already in the foreground.
     * The system logs `FSI suppressed: no HUN or keyguard` and moves on, and
     * the result is an alarm that rings with nothing on screen to stop it.
     *
     * So the activity is started directly as well. The two routes cover
     * opposite conditions and neither covers both: the direct start works
     * precisely when the app is visible, and therefore not subject to
     * background-activity-start limits; the full-screen intent works precisely
     * when it is not. Starting an activity that is already showing is harmless
     * — it is `singleInstance`, so the second start arrives as `onNewIntent`.
     */
    private fun showAlarmScreen(instanceId: Long) {
        runCatching { startActivity(AlarmActivity.intent(this, instanceId)) }
            .onFailure {
                Log.i(TAG, "direct activity start refused; relying on the full-screen intent", it)
            }
    }

    /**
     * Enters the foreground, tolerating the system refusing.
     *
     * From API 34 a foreground service start can be rejected outright if
     * the app is considered to be in the background without an exemption.
     * An alarm normally qualifies, but "normally" is not "always", and
     * taking the process down at the exact moment the user needed to be
     * woken is the worst possible response.
     */
    private fun startForegroundSafely(id: Int, notification: android.app.Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(id, notification)
            }
        }.onFailure { Log.e(TAG, "could not enter foreground", it) }
    }

    private fun startAudio(alarm: Alarm?, crescendo: Boolean) {
        val uri = alarm?.ringtoneUri?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: return

        runCatching {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            mp.setDataSource(this, uri)
            mp.isLooping = alarm?.hasTrim != true
            mp.prepare()

            val start = alarm?.ringtoneStartMillis ?: 0L
            val end = alarm?.ringtoneEndMillis?.takeIf { it > 0L } ?: mp.duration.toLong()

            if (start > 0L) mp.seekTo(start.toInt())
            mp.setVolume(if (crescendo) CRESCENDO_FLOOR else 1f, if (crescendo) CRESCENDO_FLOOR else 1f)
            mp.start()
            player = mp

            if (alarm?.hasTrim == true) {
                // MediaPlayer can loop a whole file but not a section of
                // one, so the section is looped by hand: watch the position
                // and seek back when it runs past the chosen end.
                trimLoop = scope.launch {
                    while (true) {
                        delay(TRIM_POLL_MILLIS)
                        val current = runCatching { mp.currentPosition.toLong() }.getOrNull() ?: break
                        if (current >= end || !mp.isPlaying) {
                            runCatching {
                                mp.seekTo(start.toInt())
                                if (!mp.isPlaying) mp.start()
                            }
                        }
                    }
                }
            }

            if (crescendo) {
                scope.launch {
                    var volume = CRESCENDO_FLOOR
                    while (volume < 1f) {
                        delay(CRESCENDO_STEP_MILLIS)
                        volume = (volume + CRESCENDO_STEP).coerceAtMost(1f)
                        runCatching { mp.setVolume(volume, volume) }
                    }
                }
            }
        }.onFailure {
            Log.e(TAG, "could not play $uri; falling back to the default alarm", it)
            fallbackToDefaultTone()
        }
    }

    /**
     * Last resort when the chosen sound will not play.
     *
     * A user-picked file can disappear, lose its permission grant, or be a
     * format the device cannot decode. Silence would look identical to a
     * broken alarm, so anything is better than nothing.
     */
    private fun fallbackToDefaultTone() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return
            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        }
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
                VibrationEffect.createWaveform(VIBRATION_PATTERN, 0),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build(),
            )
        }
    }

    override fun onDestroy() {
        autoSilence?.cancel()
        trimLoop?.cancel()
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BasaltAlarmService"
        private const val ACTION_START = "app.auriel.basalt.action.SERVICE_ALARM_START"
        private const val ACTION_STOP = "app.auriel.basalt.action.SERVICE_ALARM_STOP"
        private const val EXTRA_INSTANCE_ID = "instance_id"

        private const val CRESCENDO_FLOOR = 0.08f
        private const val CRESCENDO_STEP = 0.04f
        private const val CRESCENDO_STEP_MILLIS = 700L
        private const val TRIM_POLL_MILLIS = 100L
        private val VIBRATION_PATTERN = longArrayOf(0, 500, 500)

        fun start(context: Context, instanceId: Long) {
            val intent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INSTANCE_ID, instanceId)
            }
            runCatching { context.startForegroundService(intent) }
                .onFailure { Log.e(TAG, "could not start alarm service", it) }
        }

        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, AlarmService::class.java).apply { action = ACTION_STOP },
                )
            }
            runCatching { context.stopService(Intent(context, AlarmService::class.java)) }
        }
    }
}
