package app.auriel.basalt.core.dotmatrix

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Text rendered through a [DotMatrixFont].
 *
 * Every matrix carries a plain-text content description: the dots are a
 * look, not a barrier, and TalkBack must always read the real string.
 */
@Composable
fun DotMatrixText(
    text: String,
    litColor: Color,
    unlitColor: Color,
    modifier: Modifier = Modifier,
    font: DotMatrixFont = DotMatrixFont.Base5x7,
    cellSize: Float = 2f,
    shape: DotShape = DotShape.Round,
    contentDescription: String = text,
) {
    val columns = remember(text, font) { font.measure(text) }
    val rows = font.cellHeight
    val cell: Dp = cellSize.dp

    val lookup = remember(text, font) { GlyphRow(text, font) }

    Canvas(
        modifier = modifier
            .dotMatrixSize(columns, rows, cell)
            .semantics { this.contentDescription = contentDescription },
    ) {
        drawDotGrid(
            columns = columns,
            rows = rows,
            cellPx = cell.toPx(),
            litColor = litColor,
            unlitColor = unlitColor,
            shape = shape,
            fill = DotFill,
            isLit = lookup::isLit,
        )
    }
}

/**
 * Flattens a string into a single addressable cell grid so the renderer can
 * stay a plain (x, y) lookup instead of walking glyphs per frame.
 */
internal class GlyphRow(text: String, private val font: DotMatrixFont) {
    private val glyphs = text.map(font::get)
    private val stride = font.cellWidth + font.tracking

    fun isLit(x: Int, y: Int): Boolean {
        if (x < 0) return false
        val index = x / stride
        if (index >= glyphs.size) return false
        val local = x % stride
        if (local >= font.cellWidth) return false
        return glyphs[index].isLit(local, y)
    }
}
