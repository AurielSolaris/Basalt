package app.auriel.basalt.widget.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.auriel.basalt.core.design.BasaltColors
import app.auriel.basalt.core.design.type.BasaltTypeface
import app.auriel.basalt.core.dotmatrix.drawDotGrid
import app.auriel.basalt.widget.WidgetPalette
import app.auriel.basalt.widget.face.FaceControl
import app.auriel.basalt.widget.face.FaceHero
import app.auriel.basalt.widget.face.FaceLine
import app.auriel.basalt.widget.face.FaceRow
import app.auriel.basalt.widget.face.WidgetFace
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The one renderer.
 *
 * Ten widgets, one draw path. Everything a face can be is a hero plus some
 * lines, and everything a hero can be is one of five primitives, so this
 * file is the entire answer to "how does a Basalt widget get drawn" — there
 * is no per-widget layout code anywhere else in the module, and adding an
 * eleventh widget does not add any.
 *
 * Nothing here is authored at a size. The launcher hands over a box and the
 * face is measured into it, which is the only thing that survives contact
 * with a Samsung grid, a Nothing grid, a tablet and a hand-resized widget.
 *
 * Nothing here is authored in a *typeface* either. Every measurement goes
 * through the [BasaltTypeface] handed in, so the same layout code draws the
 * dot matrix and EB Garamond, and a widget matches whichever style the app
 * is set to. The layout arithmetic is in cells under both styles — a line
 * box is [BasaltTypeface.cellHeight] cells tall whatever is drawing it —
 * which is what lets the fitting rules below stay written once.
 */
object FaceRenderer {

    /** Gap between the hero and the lines, and between lines, as a fraction. */
    private const val GAP_FRACTION = 0.06f

    /** The lines never take more of the face than this. */
    private const val MAX_LINE_BLOCK = 0.45f

    /**
     * How much of a face's full height the control strip takes.
     *
     * Shared with the Glance layer, which lays its clickable regions over
     * exactly this band. The two have to agree to the pixel or the button a
     * user presses is not the button they can see, so the number lives here
     * and the overlay is computed from it rather than tuned to match.
     */
    const val CONTROL_STRIP_FRACTION = 0.26f

    fun DrawScope.drawFace(
        size: Size,
        face: WidgetFace,
        colors: BasaltColors,
        type: BasaltTypeface,
    ) {
        drawChassis(size, colors)

        val padding = (min(size.width, size.height) * 0.07f).coerceAtLeast(3f)
        val strip = if (face.controls.isEmpty()) 0f else size.height * CONTROL_STRIP_FRACTION
        val content = Rect(
            left = padding,
            top = padding,
            right = size.width - padding,
            bottom = size.height - padding - strip,
        )
        if (content.width <= 0f || content.height <= 0f) return
        if (strip > 0f) {
            drawControls(
                face.controls,
                Rect(padding, size.height - strip, size.width - padding, size.height - padding),
                colors,
                type,
            )
        }

        val gap = (content.height * GAP_FRACTION).coerceAtLeast(1f)
        val lines = fitLines(face.lines, content, gap, type)
        val linesHeight = lines.sumOf { (type.heightPx(it.cellPx) + gap).toDouble() }.toFloat()

        val heroBox = Rect(
            left = content.left,
            top = content.top,
            right = content.right,
            bottom = content.bottom - linesHeight,
        )
        if (heroBox.height > 0f) drawHero(heroBox, face.hero, colors, type)

        var y = content.bottom - linesHeight + gap / 2f
        for (line in lines) {
            drawLineRun(line, content, y, colors, type)
            y += type.heightPx(line.cellPx) + gap
        }
    }

    /**
     * One run of text, in the installed style.
     *
     * A named helper rather than `with(type) { drawRun(...) }` at each of the
     * eight call sites: the receiver-scoped form nests a block inside every
     * draw and buys nothing here, since none of these sites needs more than
     * one call from inside the scope.
     */
    private fun DrawScope.drawText(
        type: BasaltTypeface,
        text: String,
        cellPx: Float,
        origin: Offset,
        litColor: Color,
        unlitColor: Color,
    ) {
        with(type) { drawRun(text, cellPx, origin, litColor, unlitColor) }
    }

    // -- controls ---------------------------------------------------------

    /**
     * The button strip.
     *
     * Drawn rather than composed out of Glance views, for the same reason
     * everything else here is drawn: a `Button` would arrive with the
     * platform's typeface and the platform's corners, and a Basalt widget
     * with one system-styled button on it looks like a bug. The clickable
     * region is a transparent Glance box laid over the top, so the look is
     * ours and the touch handling is the framework's.
     */
    private fun DrawScope.drawControls(
        controls: List<FaceControl>,
        strip: Rect,
        colors: BasaltColors,
        type: BasaltTypeface,
    ) {
        if (controls.isEmpty() || strip.height <= 0f) return
        val gap = strip.width * 0.03f
        val slot = (strip.width - gap * (controls.size - 1)) / controls.size

        val cell = controls.minOf { control ->
            type.cellPxForText(
                text = control.label,
                availableWidthPx = slot * 0.82f,
                availableHeightPx = strip.height * 0.55f,
            )
        }
        val textHeight = type.heightPx(cell)

        controls.forEachIndexed { index, control ->
            val left = strip.left + index * (slot + gap)
            drawRoundRect(
                color = colors.ironOxide,
                topLeft = Offset(left, strip.top),
                size = Size(slot, strip.height),
                cornerRadius = CornerRadius(strip.height * 0.28f),
            )
            if (!type.isLegible(cell)) return@forEachIndexed
            val width = type.widthPx(control.label, cell)
            drawText(
                type = type,
                text = control.label,
                cellPx = cell,
                litColor = colors.silverBright,
                unlitColor = Color.Transparent,
                origin = Offset(
                    x = left + (slot - width) / 2f,
                    y = strip.top + (strip.height - textHeight) / 2f,
                ),
            )
        }
    }

    // -- chassis ----------------------------------------------------------

    /**
     * The plate the face sits on.
     *
     * A widget floats over a wallpaper rather than over the app's own
     * background, so unlike a screen it has to draw its own ground or the
     * face sits on whatever photo the user picked. The bezel follows the same
     * rule the app's panels do: the lit edge faces the light, which puts the
     * specular on top in a dark theme and underneath in a light one, because
     * on a pale ground the ground is already brighter than any highlight.
     */
    private fun DrawScope.drawChassis(size: Size, colors: BasaltColors) {
        val radius = CornerRadius(min(size.width, size.height) * 0.14f)
        drawRoundRect(color = colors.ink, size = size, cornerRadius = radius)

        val edge = (min(size.width, size.height) * 0.012f).coerceAtLeast(1f)
        drawRoundRect(
            color = colors.bronzeDeep,
            size = size,
            cornerRadius = radius,
            style = Stroke(width = edge),
        )

        val specular = colors.silverBright.copy(alpha = 0.16f)
        val shadow = colors.bronzeDeep
        val top = if (colors.isLight) shadow else specular
        val inset = edge * 1.5f
        drawLine(
            color = top,
            start = Offset(size.width * 0.18f, inset),
            end = Offset(size.width * 0.82f, inset),
            strokeWidth = edge,
        )
    }

    // -- heroes -----------------------------------------------------------

    private fun DrawScope.drawHero(
        box: Rect,
        hero: FaceHero,
        colors: BasaltColors,
        type: BasaltTypeface,
    ) {
        when (hero) {
            is FaceHero.Text -> drawHeroText(box, hero, colors, type)
            is FaceHero.Stack -> drawHeroStack(box, hero, colors, type)
            is FaceHero.Dial -> drawHeroDial(box, hero, colors)
            is FaceHero.Gauge -> drawHeroGauge(box, hero, colors, type)
            is FaceHero.Rows -> drawHeroRows(box, hero, colors, type)
        }
    }

    private fun DrawScope.drawHeroText(
        box: Rect,
        hero: FaceHero.Text,
        colors: BasaltColors,
        type: BasaltTypeface,
    ) {
        val cell = type.cellPxForText(hero.text, box.width, box.height)
        val width = type.widthPx(hero.text, cell)
        val height = type.heightPx(cell)
        drawText(
            type = type,
            text = hero.text,
            cellPx = cell,
            litColor = WidgetPalette.color(hero.role, colors),
            unlitColor = WidgetPalette.unlit(colors, cell),
            origin = Offset(
                x = box.left + (box.width - width) / 2f,
                y = box.top + (box.height - height) / 2f,
            ),
        )
    }

    /**
     * Hours over minutes.
     *
     * Both runs take the *smaller* of the two fitted cell sizes even though
     * they are always the same length, because the box they share is split
     * evenly and rounding down independently can leave one row a pixel
     * larger — which, on a two-line readout at this size, is immediately
     * visible as a typo rather than as a rounding artefact.
     */
    private fun DrawScope.drawHeroStack(
        box: Rect,
        hero: FaceHero.Stack,
        colors: BasaltColors,
        type: BasaltTypeface,
    ) {
        val gap = box.height * 0.04f
        val half = (box.height - gap) / 2f
        val cell = min(
            type.cellPxForText(hero.top, box.width, half),
            type.cellPxForText(hero.bottom, box.width, half),
        )
        val height = type.heightPx(cell)
        val color = WidgetPalette.color(hero.role, colors)
        val unlit = WidgetPalette.unlit(colors, cell)
        val blockTop = box.top + (box.height - (height * 2 + gap)) / 2f

        listOf(hero.top, hero.bottom).forEachIndexed { index, text ->
            val width = type.widthPx(text, cell)
            drawText(
                type = type,
                text = text,
                cellPx = cell,
                litColor = color,
                unlitColor = unlit,
                origin = Offset(
                    x = box.left + (box.width - width) / 2f,
                    y = blockTop + index * (height + gap),
                ),
            )
        }
    }

    /**
     * The analog dial.
     *
     * Hour marks are dots rather than strokes so the dial belongs to the same
     * family as everything else — a dial with engraved ticks and a
     * dot-matrix date under it looks like two apps. Ticks are dropped
     * entirely below the size where they merge into a ring.
     *
     * The dial is the one hero with no text in it, and so the one that looks
     * identical under both styles. That is correct rather than an oversight:
     * a clock face is a clock face, and the style is the *lettering*.
     */
    private fun DrawScope.drawHeroDial(box: Rect, hero: FaceHero.Dial, colors: BasaltColors) {
        val radius = min(box.width, box.height) / 2f
        if (radius <= 2f) return
        val centre = Offset(box.left + box.width / 2f, box.top + box.height / 2f)

        drawCircle(
            color = colors.bronzeDeep,
            radius = radius * 0.98f,
            center = centre,
            style = Stroke(width = (radius * 0.05f).coerceAtLeast(1f)),
        )

        val dot = radius * 0.06f
        if (hero.showTicks && dot >= 1.2f) {
            for (mark in 0 until 12) {
                val angle = mark * (PI / 6.0) - PI / 2.0
                val quarter = mark % 3 == 0
                drawCircle(
                    color = if (quarter) colors.silver else colors.pewter,
                    radius = if (quarter) dot else dot * 0.62f,
                    center = Offset(
                        x = centre.x + (radius * 0.84f * cos(angle)).toFloat(),
                        y = centre.y + (radius * 0.84f * sin(angle)).toFloat(),
                    ),
                )
            }
        }

        // Hours advance with the minutes; a dial whose hour hand jumps on the
        // hour is the single most common tell of a hand-drawn clock face.
        val minuteTurn = hero.minute / 60.0
        hand(centre, radius * 0.50f, (hero.hour % 12 + minuteTurn) / 12.0, colors.copper, radius * 0.09f)
        hand(centre, radius * 0.74f, minuteTurn, colors.silverBright, radius * 0.06f)

        drawCircle(color = colors.copperHot, radius = (radius * 0.07f).coerceAtLeast(1f), center = centre)
    }

    private fun DrawScope.hand(
        centre: Offset,
        length: Float,
        turns: Double,
        color: Color,
        width: Float,
    ) {
        val angle = turns * 2.0 * PI - PI / 2.0
        drawLine(
            color = color,
            start = centre,
            end = Offset(
                x = centre.x + (length * cos(angle)).toFloat(),
                y = centre.y + (length * sin(angle)).toFloat(),
            ),
            strokeWidth = width.coerceAtLeast(1f),
        )
    }

    /** An arc filled to a fraction, with the reading written inside it. */
    private fun DrawScope.drawHeroGauge(
        box: Rect,
        hero: FaceHero.Gauge,
        colors: BasaltColors,
        type: BasaltTypeface,
    ) {
        val diameter = min(box.width, box.height)
        if (diameter <= 4f) return
        val stroke = (diameter * 0.09f).coerceAtLeast(1.5f)
        val arcSize = Size(diameter - stroke, diameter - stroke)
        val topLeft = Offset(
            x = box.left + (box.width - arcSize.width) / 2f,
            y = box.top + (box.height - arcSize.height) / 2f,
        )

        drawArc(
            color = colors.unlit.copy(alpha = 0.5f).compositeOver(colors.ironOxide),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = WidgetPalette.color(hero.role, colors),
            startAngle = -90f,
            sweepAngle = 360f * hero.fraction,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke),
        )

        // The readout lives inside the arc, so its box is the inscribed
        // square rather than the circle: text laid out to the diameter would
        // clip its own corners against the stroke.
        val inner = (diameter - stroke * 3f) * 0.70f
        if (inner <= 0f) return
        val cell = type.cellPxForText(hero.readout, inner, inner)
        if (!type.isLegible(cell)) return
        val width = type.widthPx(hero.readout, cell)
        val height = type.heightPx(cell)
        drawText(
            type = type,
            text = hero.readout,
            cellPx = cell,
            litColor = colors.silverBright,
            unlitColor = Color.Transparent,
            origin = Offset(
                x = box.left + (box.width - width) / 2f,
                y = box.top + (box.height - height) / 2f,
            ),
        )
    }

    /**
     * A list: name on the left, value on the right.
     *
     * Every row takes the same cell size — the one that fits the *widest*
     * row — rather than each fitting itself. A board whose rows are
     * different sizes reads as a list of unrelated things; the point of the
     * world and timer faces is that the rows are comparable.
     */
    private fun DrawScope.drawHeroRows(
        box: Rect,
        hero: FaceHero.Rows,
        colors: BasaltColors,
        type: BasaltTypeface,
    ) {
        val rows = hero.rows
        if (rows.isEmpty()) return
        val gap = box.height * 0.10f / rows.size
        val rowHeight = (box.height - gap * (rows.size - 1)) / rows.size
        if (rowHeight <= 0f) return

        val cell = rows.minOf { row ->
            type.cellPxForGrid(
                columns = type.widthPx(row.leading, 1f) +
                    type.widthPx(row.trailing, 1f) +
                    type.columnGapCells,
                rows = type.cellHeight,
                availableWidthPx = box.width,
                availableHeightPx = rowHeight,
            )
        }
        val textHeight = type.heightPx(cell)

        rows.forEachIndexed { index, row ->
            val top = box.top + index * (rowHeight + gap) + (rowHeight - textHeight) / 2f
            drawRow(row, box, top, cell, colors, type)
        }
    }

    private fun DrawScope.drawRow(
        row: FaceRow,
        box: Rect,
        top: Float,
        cell: Float,
        colors: BasaltColors,
        type: BasaltTypeface,
    ) {
        val color = WidgetPalette.color(row.role, colors)
        val unlit = WidgetPalette.unlit(colors, cell)
        val trailingWidth = type.widthPx(row.trailing, cell)

        // The value is placed first and the name is truncated to what is
        // left. A city whose name does not fit is still worth showing; a
        // time that does not fit is not.
        drawText(
            type = type,
            text = row.trailing,
            cellPx = cell,
            litColor = color,
            unlitColor = unlit,
            origin = Offset(box.right - trailingWidth, top),
        )

        val room = box.width - trailingWidth - cell * type.columnGapCells
        val leading = type.truncateToWidth(row.leading, cell, room)
        if (leading.isEmpty()) return
        drawText(
            type = type,
            text = leading,
            cellPx = cell,
            litColor = if (row.emphasis) colors.silverBright else colors.silver,
            unlitColor = unlit,
            origin = Offset(box.left, top),
        )
    }

    // -- lines ------------------------------------------------------------

    /** A line that survived fitting, with the size it will be drawn at. */
    private data class FittedLine(val line: FaceLine, val text: String, val cellPx: Float)

    /**
     * Chooses a size for the supporting lines, and drops the ones that do
     * not earn their space.
     *
     * Two rules, in order. A line that would be drawn below legibility is
     * removed rather than drawn as mush — an unreadable line costs the hero
     * height and returns nothing. And the block as a whole is capped at
     * [MAX_LINE_BLOCK] of the face, because on the smallest sizes the time
     * is the widget and three captions under a two-pixel clock is a worse
     * widget than a clock alone.
     *
     * Captions go first because they are declared last, which is the
     * convention the faces follow: the further down a line is, the less it
     * matters.
     */
    private fun fitLines(
        lines: List<FaceLine>,
        content: Rect,
        gap: Float,
        type: BasaltTypeface,
    ): List<FittedLine> {
        if (lines.isEmpty()) return emptyList()
        var candidates = lines
        while (candidates.isNotEmpty()) {
            val cell = candidates.minOf { line ->
                type.cellPxForText(
                    text = line.text,
                    availableWidthPx = content.width,
                    // A line may never take more than its equal share of the
                    // block, whatever its own text would allow.
                    availableHeightPx = content.height * MAX_LINE_BLOCK / candidates.size - gap,
                )
            }
            val blockHeight = candidates.size * (type.heightPx(cell) + gap)
            if (type.isLegible(cell) && blockHeight <= content.height * MAX_LINE_BLOCK) {
                return candidates.map { line ->
                    FittedLine(
                        line = line,
                        text = type.truncateToWidth(line.text, cell, content.width),
                        cellPx = cell,
                    )
                }
            }
            candidates = candidates.dropLast(1)
        }
        return emptyList()
    }

    private fun DrawScope.drawLineRun(
        fitted: FittedLine,
        content: Rect,
        y: Float,
        colors: BasaltColors,
        type: BasaltTypeface,
    ) {
        val width = type.widthPx(fitted.text, fitted.cellPx)
        drawText(
            type = type,
            text = fitted.text,
            cellPx = fitted.cellPx,
            litColor = WidgetPalette.color(fitted.line.role, colors),
            unlitColor = Color.Transparent,
            origin = Offset(content.left + (content.width - width) / 2f, y),
        )
    }

    /**
     * The picker preview, drawn without any data.
     *
     * Every widget needs a preview image and the framework wants one before
     * the app has run. Rather than ship ten hand-drawn PNGs that go stale
     * the first time a face changes, previews are the real renderer fed a
     * fixed snapshot — so a preview is wrong only when the widget is.
     *
     * The placeholder stays a dot grid under both styles: it is standing in
     * for a widget, not for a word, and the grid is what reads as "Basalt"
     * at thumbnail size.
     */
    fun DrawScope.drawPlaceholderGrid(size: Size, colors: BasaltColors) {
        val cell = (min(size.width, size.height) / 12f).coerceAtLeast(1f)
        drawDotGrid(
            columns = (size.width / cell).toInt(),
            rows = (size.height / cell).toInt(),
            cellPx = cell,
            litColor = colors.copper,
            unlitColor = colors.unlit,
        ) { x, y -> (x + y) % 3 == 0 }
    }
}

/** Compose has this on `Color` but not in a form the arc call above wants. */
private fun Color.compositeOver(background: Color): Color {
    val alpha = this.alpha
    return Color(
        red = red * alpha + background.red * (1 - alpha),
        green = green * alpha + background.green * (1 - alpha),
        blue = blue * alpha + background.blue * (1 - alpha),
        alpha = 1f,
    )
}
