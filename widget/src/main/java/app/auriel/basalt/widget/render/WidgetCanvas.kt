package app.auriel.basalt.widget.render

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.sqrt

/**
 * A Compose drawing surface that ends up as a bitmap.
 *
 * Glance renders to `RemoteViews`, and `RemoteViews` cannot host a Compose
 * `Canvas` — it is a description of a view hierarchy sent to another process,
 * not a place code runs. So a widget that wants the app's dot matrix has
 * exactly one route: draw into a bitmap here, and hand the launcher an
 * `Image` of it.
 *
 * The important part is that the drawing itself is a `DrawScope`, the same
 * type a composable's `Canvas` provides. Every dot in this module goes
 * through `:core:dotmatrix`'s draw functions, so a widget and the screen it
 * mirrors are the same code producing the same pixels, and cannot drift.
 */
object WidgetCanvas {

    /**
     * The most a widget's bitmap may weigh.
     *
     * `AppWidgetManager` ships updates over a Binder transaction, and an
     * oversized bitmap does not degrade — it throws, or worse, silently
     * leaves the last good frame on the home screen so the widget appears
     * frozen rather than broken. The framework's own guidance is to stay
     * well inside the transaction limit; a megabyte and a half is roughly
     * one full-screen 1080p widget at ARGB_8888 and comfortably more than
     * any 5×5 face needs.
     *
     * Exceeding it is not an error here. The bitmap is rendered smaller and
     * the launcher scales it up — a slightly soft grid beats a widget that
     * stopped updating, and the case only arises on a tablet-sized cell.
     */
    const val MAX_BYTES: Long = 1_500_000L

    private const val BYTES_PER_PIXEL = 4

    /**
     * Renders [widthPx] × [heightPx] of drawing into a bitmap, clamped to
     * [MAX_BYTES].
     *
     * The block receives the size actually rendered, which may be smaller
     * than what was asked for. Faces measure from that rather than from
     * their declared cell size, so a clamped widget lays itself out for the
     * space it really got instead of drawing off the edge.
     */
    fun render(
        widthPx: Int,
        heightPx: Int,
        density: Float,
        block: DrawScope.(Size) -> Unit,
    ): Bitmap {
        val (width, height) = clampToBudget(widthPx, heightPx)
        val image = ImageBitmap(width, height)
        val size = Size(width.toFloat(), height.toFloat())
        CanvasDrawScope().draw(
            density = Density(density),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(image),
            size = size,
        ) {
            block(size)
        }
        return image.asAndroidBitmap()
    }

    /**
     * The largest size within [MAX_BYTES] that keeps the requested aspect
     * ratio, never smaller than a single pixel each way.
     *
     * Scaling both axes by the same factor matters: clamping only the larger
     * one would distort a wide widget into a square, and a dot matrix with
     * non-square cells reads as a rendering fault rather than as a small
     * widget.
     */
    fun clampToBudget(widthPx: Int, heightPx: Int): Pair<Int, Int> {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val bytes = width.toLong() * height.toLong() * BYTES_PER_PIXEL
        if (bytes <= MAX_BYTES) return width to height
        val scale = sqrt(MAX_BYTES.toDouble() / bytes.toDouble())
        return (width * scale).toInt().coerceAtLeast(1) to
            (height * scale).toInt().coerceAtLeast(1)
    }
}
