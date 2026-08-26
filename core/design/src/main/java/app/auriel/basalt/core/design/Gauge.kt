package app.auriel.basalt.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * An etched brass gauge: a ring of ticks, a lit arc, and a needle.
 *
 * Used wherever a proportion is more legible as a sweep than as a number —
 * the stopwatch's seconds hand, the bedtime window. The numeric readout
 * always sits beside it; the gauge is the glance, not the answer.
 */
@Composable
fun BasaltGauge(
    /** Where the lit arc begins, 0f..1f clockwise from twelve. */
    start: Float,
    /** How far it sweeps, 0f..1f. */
    sweep: Float,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    thickness: Dp = 10.dp,
    arcColor: Color? = null,
    /** Drawn at the leading edge of the arc. Null for no needle. */
    needleAt: Float? = null,
    ticks: Int = 12,
    content: @Composable () -> Unit = {},
) {
    val colors = LocalBasaltColors.current
    val lit = arcColor ?: colors.copper

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = thickness.toPx()
            val inset = stroke / 2f + 2.dp.toPx()
            val diameter = this.size.minDimension - inset * 2
            val topLeft = Offset(
                (this.size.width - diameter) / 2f,
                (this.size.height - diameter) / 2f,
            )
            val centre = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = diameter / 2f

            // The unlit track: always drawn, like the unlit dots.
            drawArc(
                color = colors.pewter.copy(alpha = 0.16f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )

            if (sweep > 0f) {
                drawArc(
                    color = lit,
                    startAngle = -90f + start * 360f,
                    sweepAngle = sweep.coerceIn(0f, 1f) * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
            }

            for (i in 0 until ticks) {
                val a = Math.toRadians((i * 360.0 / ticks) - 90.0)
                val quarter = ticks % 4 == 0 && i % (ticks / 4) == 0
                val outer = radius + stroke / 2f + 3.dp.toPx()
                val inner = outer - (if (quarter) 8.dp.toPx() else 5.dp.toPx())
                drawLine(
                    color = if (quarter) colors.silverBright else colors.pewter,
                    start = Offset(centre.x + cos(a).toFloat() * inner, centre.y + sin(a).toFloat() * inner),
                    end = Offset(centre.x + cos(a).toFloat() * outer, centre.y + sin(a).toFloat() * outer),
                    strokeWidth = if (quarter) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            needleAt?.let { position ->
                // Only the outer third of a hand, not a whole one. A gauge this
                // size carries its readout in the middle, and a needle pivoting
                // from the true centre draws a line straight through the digits
                // twice a revolution. Outboard, it still reads as a pointer and
                // the readout stays legible at every position.
                val a = Math.toRadians(position * 360.0 - 90.0)
                val inner = radius * 0.70f
                val outer = radius + stroke * 0.35f
                drawLine(
                    color = colors.copperHot,
                    start = Offset(
                        centre.x + cos(a).toFloat() * inner,
                        centre.y + sin(a).toFloat() * inner,
                    ),
                    end = Offset(
                        centre.x + cos(a).toFloat() * outer,
                        centre.y + sin(a).toFloat() * outer,
                    ),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        content()
    }
}
