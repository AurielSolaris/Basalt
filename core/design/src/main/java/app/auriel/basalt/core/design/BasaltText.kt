package app.auriel.basalt.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.auriel.basalt.core.design.type.BasaltTypeface
import app.auriel.basalt.core.design.type.BasaltTypefaces
import app.auriel.basalt.core.dotmatrix.DotShape

/**
 * Text, in whichever style is installed.
 *
 * The front door for every string the app draws. It takes the same arguments
 * [app.auriel.basalt.core.dotmatrix.DotMatrixText] took and means the same
 * things by them — `cellSize` is still the height of one row of the grid, and
 * still the only size anyone passes — so a screen written for the dot matrix
 * renders under Vintage without being re-measured. That was the point of
 * putting the style behind a typeface rather than behind a text style: the
 * app's fifty-odd readouts did not have to learn about fonts.
 *
 * ### Why this draws rather than using BasicText
 *
 * Vintage could have been a `BasicText` with a `FontFamily`, and it would
 * have been fewer lines. It would also have been a *second* implementation
 * of Vintage, because the widgets cannot use `BasicText` — a widget is
 * `RemoteViews` and rasterises into a bitmap — so the home screen and the
 * app would have measured and placed the same string through different code.
 * The two would have agreed right up until they didn't. Drawing both styles
 * through [BasaltTypeface] means a Vintage line in the app and the same line
 * on a widget are the same pixels by construction.
 *
 * Because it is a canvas rather than a text node, the string is not
 * automatically available to accessibility services — so, exactly as the dot
 * matrix did, every run carries a plain-text [contentDescription]. The
 * lettering is a look, not a barrier.
 */
@Composable
fun BasaltText(
    text: String,
    litColor: Color,
    unlitColor: Color = Color.Transparent,
    modifier: Modifier = Modifier,
    cellSize: Float = 3f,
    shape: DotShape = DotShape.Round,
    contentDescription: String = text,
    style: BasaltStyleId = LocalBasaltStyle.current,
) {
    val context = LocalContext.current
    val typeface = remember(style, shape) {
        BasaltTypefaces.of(style, context, shape)
    }

    // Width is measured in the same units the caller passed, which works
    // because width is linear in cell size under both styles: a dp of cell
    // buys a fixed number of dp of advance. Doing it here rather than in the
    // draw pass keeps the composable's size available at layout time.
    val widthCells = remember(text, typeface) { typeface.widthPx(text, 1f) }

    Canvas(
        modifier = modifier
            .size(
                width = (cellSize * widthCells).dp,
                height = (cellSize * typeface.cellHeight).dp,
            )
            .semantics { this.contentDescription = contentDescription },
    ) {
        val cellPx = cellSize.dp.toPx()
        with(typeface) {
            drawRun(
                text = text,
                cellPx = cellPx,
                origin = Offset.Zero,
                litColor = litColor,
                unlitColor = unlitColor,
            )
        }
    }
}
