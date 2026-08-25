package app.auriel.basalt.core.dotmatrix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How an individual cell is painted. */
enum class DotShape {
    /** Round dot — the default; reads as an LED. */
    Round,

    /** Rounded square — the "chunky" variant; reads as an LCD segment. */
    Chunky,
}

/**
 * A grid of lit/unlit cells.
 *
 * Unlit cells are *drawn*, not skipped: the dark grid behind the message is
 * as much a part of the look as the message itself.
 */
@Composable
fun DotMatrix(
    columns: Int,
    rows: Int,
    cellSize: Dp,
    litColor: Color,
    unlitColor: Color,
    modifier: Modifier = Modifier,
    shape: DotShape = DotShape.Round,
    /** Fraction of the cell the dot occupies. */
    fill: Float = 0.78f,
    isLit: (x: Int, y: Int) -> Boolean,
) {
    Canvas(modifier = modifier.dotMatrixSize(columns, rows, cellSize)) {
        drawDotMatrix(columns, rows, cellSize.toPx(), litColor, unlitColor, shape, fill, isLit)
    }
}

internal fun Modifier.dotMatrixSize(columns: Int, rows: Int, cellSize: Dp): Modifier =
    this.size(
        width = cellSize * columns,
        height = cellSize * rows,
    )

internal fun DrawScope.drawDotMatrix(
    columns: Int,
    rows: Int,
    cellPx: Float,
    litColor: Color,
    unlitColor: Color,
    shape: DotShape,
    fill: Float,
    isLit: (x: Int, y: Int) -> Boolean,
) {
    val dot = cellPx * fill
    val inset = (cellPx - dot) / 2f
    for (y in 0 until rows) {
        for (x in 0 until columns) {
            val color = if (isLit(x, y)) litColor else unlitColor
            val left = x * cellPx + inset
            val top = y * cellPx + inset
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

/** Convenience default used by callers that have no opinion on density. */
val DefaultCellSize: Dp = 3.dp
