package app.auriel.basalt.core.dotmatrix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    fill: Float = DotFill,
    isLit: (x: Int, y: Int) -> Boolean,
) {
    Canvas(modifier = modifier.dotMatrixSize(columns, rows, cellSize)) {
        drawDotGrid(
            columns = columns,
            rows = rows,
            cellPx = cellSize.toPx(),
            litColor = litColor,
            unlitColor = unlitColor,
            shape = shape,
            fill = fill,
            isLit = isLit,
        )
    }
}

internal fun Modifier.dotMatrixSize(columns: Int, rows: Int, cellSize: Dp): Modifier =
    this.size(
        width = cellSize * columns,
        height = cellSize * rows,
    )

/** Convenience default used by callers that have no opinion on density. */
val DefaultCellSize: Dp = 3.dp
