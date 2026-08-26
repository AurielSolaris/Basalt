package app.auriel.basalt.feature.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plays the selected section, on repeat, exactly as the alarm will.
 *
 * Auditioning the trim is the whole point of setting one: a start and end
 * in seconds mean nothing until you hear where they land. This uses the
 * same seek-and-loop approach as [AlarmService], so what is heard here is
 * what rings in the morning.
 */
class RingtonePreview(private val context: Context, private val scope: CoroutineScope) {

    private var player: MediaPlayer? = null
    private var loop: Job? = null

    fun play(uri: Uri, startMillis: Long, endMillis: Long) {
        stop()
        runCatching {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            mp.setDataSource(context, uri)
            mp.prepare()

            val end = endMillis.takeIf { it > 0L } ?: mp.duration.toLong()
            mp.seekTo(startMillis.toInt())
            mp.start()
            player = mp

            loop = scope.launch {
                while (true) {
                    delay(80)
                    val position = runCatching { mp.currentPosition.toLong() }.getOrNull() ?: break
                    if (position >= end) {
                        runCatching {
                            mp.seekTo(startMillis.toInt())
                            if (!mp.isPlaying) mp.start()
                        }
                    }
                }
            }
        }.onFailure { Log.w(TAG, "preview failed for $uri", it) }
    }

    fun stop() {
        loop?.cancel()
        loop = null
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    val isPlaying: Boolean get() = runCatching { player?.isPlaying == true }.getOrDefault(false)

    private companion object {
        const val TAG = "BasaltPreview"
    }
}
