package app.auriel.basalt.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Basalt's icon set.
 *
 * Drawn, not imported: there is no Material here to take icons from, and a
 * stroked instrument-panel line weight is part of the look. Every icon is
 * authored in a 24x24 box and scaled at draw time, so one definition serves
 * the tab strip, the widgets and anything later that needs it.
 *
 * The vocabulary is deliberately mechanical — a bell with a striker, a
 * cog with real teeth, a sand-glass — rather than the rounded glyph shapes a
 * modern set would use.
 */
enum class BasaltIcon {
    Alarm,
    Clock,
    Timer,
    Stopwatch,
    Bedtime,
    Settings,
    ;

    internal fun DrawScope.render(color: Color, stroke: Stroke) {
        when (this@BasaltIcon) {
            Alarm -> drawAlarm(color, stroke)
            Clock -> drawClock(color, stroke)
            Timer -> drawTimer(color, stroke)
            Stopwatch -> drawStopwatch(color, stroke)
            Bedtime -> drawBedtime(color, stroke)
            Settings -> drawSettings(color, stroke)
        }
    }
}

/** Draws [icon] at [size], scaled from its 24x24 authoring box. */
@Composable
fun BasaltIconGlyph(
    icon: BasaltIcon,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    strokeWidth: Dp = 1.6.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val factor = this.size.minDimension / UNIT
        val stroke = Stroke(
            width = strokeWidth.toPx() / factor,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        scale(factor, pivot = Offset.Zero) {
            with(icon) { render(color, stroke) }
        }
    }
}

private const val UNIT = 24f
private const val C = UNIT / 2

// ------------------------------------------------------------------- glyphs

private fun DrawScope.drawClock(color: Color, stroke: Stroke) {
    drawCircle(color, radius = 9f, center = Offset(C, C), style = stroke)
    // Ticks at the quarters, so it reads as an instrument rather than a badge.
    for (i in 0 until 4) {
        val a = Math.toRadians(i * 90.0)
        drawLine(
            color = color,
            start = Offset(C + sin(a).toFloat() * 7.4f, C - cos(a).toFloat() * 7.4f),
            end = Offset(C + sin(a).toFloat() * 8.6f, C - cos(a).toFloat() * 8.6f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
    hands(color, stroke, hourLength = 4f, minuteLength = 6.2f, hourAngle = -60f, minuteAngle = 60f)
}

private fun DrawScope.drawAlarm(color: Color, stroke: Stroke) {
    drawCircle(color, radius = 7.6f, center = Offset(C, C + 1.4f), style = stroke)
    // Bells, drawn as arcs sitting on the shoulders of the case.
    for (side in listOf(-1f, 1f)) {
        drawArc(
            color = color,
            startAngle = if (side < 0) 150f else 250f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(C + side * 7.4f - 3.2f, 1.2f),
            size = Size(6.4f, 6.4f),
            style = stroke,
        )
    }
    // Feet.
    for (side in listOf(-1f, 1f)) {
        drawLine(
            color = color,
            start = Offset(C + side * 5.2f, C + 7.4f),
            end = Offset(C + side * 7.2f, C + 9.6f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
    translate(top = 1.4f) {
        hands(color, stroke, hourLength = 3.2f, minuteLength = 4.6f, hourAngle = -45f, minuteAngle = 75f)
    }
}

private fun DrawScope.drawTimer(color: Color, stroke: Stroke) {
    // A sand-glass: two triangles meeting at a waist, in a frame.
    val path = Path().apply {
        moveTo(C - 6f, 3.5f)
        lineTo(C + 6f, 3.5f)
        lineTo(C + 0.9f, C)
        lineTo(C + 6f, UNIT - 3.5f)
        lineTo(C - 6f, UNIT - 3.5f)
        lineTo(C - 0.9f, C)
        close()
    }
    drawPath(path, color, style = stroke)
    for (y in listOf(3.5f, UNIT - 3.5f)) {
        drawLine(
            color = color,
            start = Offset(C - 7.2f, y),
            end = Offset(C + 7.2f, y),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
    // The sand that has already fallen.
    val sand = Path().apply {
        moveTo(C - 4.4f, UNIT - 4.4f)
        lineTo(C + 4.4f, UNIT - 4.4f)
        lineTo(C, C + 3.4f)
        close()
    }
    drawPath(sand, color)
}

private fun DrawScope.drawStopwatch(color: Color, stroke: Stroke) {
    drawCircle(color, radius = 7.6f, center = Offset(C, C + 1.6f), style = stroke)
    // Crown and shoulder button — the parts that make it a stopwatch and not a
    // clock.
    drawLine(color, Offset(C, 3.4f), Offset(C, 5.6f), strokeWidth = stroke.width * 1.6f, cap = StrokeCap.Round)
    drawLine(color, Offset(C - 2.6f, 3.4f), Offset(C + 2.6f, 3.4f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(
        color = color,
        start = Offset(C + 6.2f, 5.4f),
        end = Offset(C + 7.6f, 4f),
        strokeWidth = stroke.width * 1.4f,
        cap = StrokeCap.Round,
    )
    // A single hand, part-way round: a stopwatch at rest reads as stopped.
    drawLine(
        color = color,
        start = Offset(C, C + 1.6f),
        end = Offset(C + 3.9f, C - 2.3f),
        strokeWidth = stroke.width * 1.2f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawBedtime(color: Color, stroke: Stroke) {
    // Crescent by subtraction, so the inner edge stays a true arc.
    val outer = Path().apply { addOval(Rect(Offset(C - 8f, C - 8f), Size(16f, 16f))) }
    val bite = Path().apply { addOval(Rect(Offset(C - 3.4f, C - 10.4f), Size(15f, 15f))) }
    drawPath(Path().apply { op(outer, bite, PathOperation.Difference) }, color, style = stroke)
    // Two stars, small enough to read as punctuation.
    drawCircle(color, radius = 0.9f, center = Offset(C + 5.6f, C - 5.4f))
    drawCircle(color, radius = 0.6f, center = Offset(C + 7.4f, C - 1.8f))
}

private fun DrawScope.drawSettings(color: Color, stroke: Stroke) {
    // A cog with real teeth rather than a sliders glyph: this is the most
    // steam-age shape in the set and it earns its place on the settings tab.
    val teeth = 8
    for (i in 0 until teeth) {
        rotate(degrees = i * (360f / teeth), pivot = Offset(C, C)) {
            drawRoundRect(
                color = color,
                topLeft = Offset(C - 1.5f, C - 10.2f),
                size = Size(3f, 3.6f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.6f, 0.6f),
            )
        }
    }
    drawCircle(color, radius = 6.6f, center = Offset(C, C), style = stroke)
    drawCircle(color, radius = 2.6f, center = Offset(C, C), style = stroke)
}

private fun DrawScope.hands(
    color: Color,
    stroke: Stroke,
    hourLength: Float,
    minuteLength: Float,
    hourAngle: Float,
    minuteAngle: Float,
) {
    for ((angle, length, weight) in listOf(
        Triple(hourAngle, hourLength, 1.3f),
        Triple(minuteAngle, minuteLength, 1f),
    )) {
        val a = Math.toRadians(angle.toDouble())
        drawLine(
            color = color,
            start = Offset(C, C),
            end = Offset(C + sin(a).toFloat() * length, C - cos(a).toFloat() * length),
            strokeWidth = stroke.width * weight,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(color, radius = stroke.width * 0.9f, center = Offset(C, C))
}
