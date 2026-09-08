package app.auriel.basalt.core.dotmatrix

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * The dot grid, as plain drawing rather than as a composable.
 *
 * [DotMatrix] and [DotMatrixText] are the composable front doors, and they
 * are the right ones for a screen. Widgets cannot use them: a Glance widget
 * is `RemoteViews`, which cannot host a Compose `Canvas`, so a widget has to
 * rasterise into a bitmap and hand the launcher an `Image`.
 *
 * Rather than let the widget module reimplement the dots — which is how the
 * app and its widgets stop matching — both paths draw through the functions
 * here. A `DrawScope` is a `DrawScope` whether it came from a composable or
 * from a bitmap, so this is the whole of what has to be shared.
 *
 * Everything takes an [origin] because a widget composes several runs of
 * text and several grids onto one canvas; a composable draws exactly one and
 * lets layout place it.
 */

/** Default fraction of a cell the dot occupies. */
const val DotFill: Float = 0.78f

/**
 * Draws a [columns] × [rows] grid of cells, each [cellPx] on a side, with
 * its top-left corner at [origin].
 *
 * Unlit cells are painted, not skipped — the dark grid behind the message is
 * as much of the look as the message. Pass [Color.Transparent] as
 * [unlitColor] to suppress it, which is what the smallest widget sizes do
 * once the grid stops reading as texture and starts reading as noise.
 */
fun DrawScope.drawDotGrid(
    columns: Int,
    rows: Int,
    cellPx: Float,
    litColor: Color,
    unlitColor: Color,
    origin: Offset = Offset.Zero,
    shape: DotShape = DotShape.Round,
    fill: Float = DotFill,
    isLit: (x: Int, y: Int) -> Boolean,
) {
    val dot = cellPx * fill
    val inset = (cellPx - dot) / 2f
    for (y in 0 until rows) {
        for (x in 0 until columns) {
            val color = if (isLit(x, y)) litColor else unlitColor
            if (color.alpha == 0f) continue
            val left = origin.x + x * cellPx + inset
            val top = origin.y + y * cellPx + inset
            when (shape) {
                DotShape.Round -> drawCircle(
                    color = color,
                    radius = dot / 2f,
                    center = Offset(left + dot / 2f, top + dot / 2f),
                )

                DotShape.Chunky -> drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(dot, dot),
                )
            }
        }
    }
}

/**
 * Draws [text] in [font] with its top-left corner at [origin], and returns
 * the width it occupied in pixels.
 *
 * Returning the width rather than requiring the caller to call
 * [DotMatrixFont.measure] separately keeps the advance and the drawing from
 * being able to disagree, which is what produces a widget where the second
 * field overlaps the first only for certain strings.
 */
fun DrawScope.drawDotText(
    text: String,
    font: DotMatrixFont,
    cellPx: Float,
    litColor: Color,
    unlitColor: Color,
    origin: Offset = Offset.Zero,
    shape: DotShape = DotShape.Round,
    fill: Float = DotFill,
): Float {
    val columns = font.measure(text)
    if (columns <= 0) return 0f
    val row = GlyphRow(text, font)
    drawDotGrid(
        columns = columns,
        rows = font.cellHeight,
        cellPx = cellPx,
        litColor = litColor,
        unlitColor = unlitColor,
        origin = origin,
        shape = shape,
        fill = fill,
        isLit = row::isLit,
    )
    return columns * cellPx
}
