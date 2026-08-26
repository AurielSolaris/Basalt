package app.auriel.basalt.feature.alarm

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log

/** One choosable sound. */
data class RingtoneChoice(
    val uri: String?,
    val title: String,
    /** True for a file the user opened themselves. */
    val userSupplied: Boolean = false,
)

/**
 * Where alarm sounds come from.
 *
 * Two sources, deliberately kept side by side rather than behind separate
 * screens: the device's own alarm tones, and any audio file the user points
 * at. The second is the reason this exists — being restricted to a dozen
 * stock tones is the single most common complaint about stock clock apps.
 */
object RingtoneCatalog {

    private const val TAG = "BasaltRingtones"

    /**
     * The device's alarm tones, plus its ringtones.
     *
     * Ringtones are included because plenty of devices ship two usable
     * alarm sounds and thirty ringtones, and the user's opinion about what
     * will wake them beats the manufacturer's categorisation.
     */
    fun systemTones(context: Context): List<RingtoneChoice> {
        val tones = mutableListOf(RingtoneChoice(uri = null, title = "DEFAULT ALARM"))
        listOf(RingtoneManager.TYPE_ALARM, RingtoneManager.TYPE_RINGTONE).forEach { type ->
            runCatching {
                val manager = RingtoneManager(context).apply { setType(type) }
                val cursor = manager.cursor
                while (cursor.moveToNext()) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val uri = manager.getRingtoneUri(cursor.position)?.toString() ?: continue
                    if (tones.none { it.uri == uri }) {
                        tones += RingtoneChoice(uri = uri, title = title.uppercase())
                    }
                }
            }.onFailure { Log.w(TAG, "could not enumerate type $type", it) }
        }
        return tones
    }

    /**
     * Remembers a file the user opened, so it still plays tomorrow.
     *
     * A document-picker URI is granted to this process only, and evaporates
     * on restart. Taking the persistable grant is what turns "plays once"
     * into "is my alarm sound" — and it is the step whose absence produces
     * a silent alarm days later, which is the worst kind of bug.
     */
    fun persist(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        true
    }.getOrElse {
        Log.w(TAG, "could not persist permission for $uri", it)
        false
    }

    /** A readable name for a picked file, falling back to its last path part. */
    fun displayName(context: Context, uri: Uri): String {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) {
                        return cursor.getString(index).uppercase()
                    }
                }
            }
        }
        return (uri.lastPathSegment ?: "AUDIO FILE").uppercase()
    }

    /** Length in milliseconds, or zero when it cannot be determined. */
    fun durationMillis(context: Context, uri: Uri): Long = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        }
    }.getOrElse {
        Log.w(TAG, "could not read duration of $uri", it)
        0L
    }

    /** What the document picker will accept. */
    val AudioMimeTypes = arrayOf("audio/mpeg", "audio/wav", "audio/x-wav", "audio/*")
}
