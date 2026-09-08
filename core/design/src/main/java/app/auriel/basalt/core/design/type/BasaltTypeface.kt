package app.auriel.basalt.core.design.type

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.floor

/**
 * Lettering, as measurement plus drawing.
 *
 * This is the seam that makes a second UI style possible without a second
 * renderer. Everything that draws text in Basalt — the app's chrome and the
 * ten widget faces alike — needs exactly five things from a typeface: how
 * wide a string is, how tall a line is, the largest size that fits a box,
 * where to stop truncating, and how to paint it. Those five are this
 * interface, and both styles answer them.
 *
 * ### Why everything is in cells rather than points
 *
 * The unit throughout is [cellPx] — the height of one row of the dot grid —
 * and *not* a font size, even for the style that has no grid. That looks
 * backwards until you notice what it buys: every layout in the app and in
 * [FaceRenderer] is already written in cells, so switching styles changes
 * which glyphs come out and changes nothing about where they go. A line box
 * is [cellHeight] cells tall under both styles, so a face that fitted before
 * still fits, and a widget that dropped its third caption at 2x1 still drops
 * exactly that caption.
 *
 * The alternative — measuring Vintage in scaled points — would have meant
 * every call site carrying two sizes and every fitting rule being written
 * twice. This way the fitting rules are written once, here, as defaults.
 *
 * ### Why the drawing method takes a DrawScope receiver
 *
 * Same reason [app.auriel.basalt.core.dotmatrix.drawDotText] does: a widget
 * cannot host a Compose `Canvas`, so it rasterises into a bitmap, and a
 * `DrawScope` is the one thing common to both paths. Callers write
 * `with(typeface) { drawRun(...) }` from inside their own `DrawScope`.
 */
interface BasaltTypeface {

    /** Rows in a line box. Seven, matching the 5x7 glyph set. */
    val cellHeight: Int get() = 7

    /** Below this, a run is texture rather than text and is better dropped. */
    val minLegibleCellPx: Float get() = 3f

    /** How wide [text] is, in pixels, at [cellPx]. */
    fun widthPx(text: String, cellPx: Float): Float

    /** How tall any line is, in pixels, at [cellPx]. */
    fun heightPx(cellPx: Float): Float = cellHeight * cellPx

    /**
     * Snaps a fitted size to one the style can actually draw at.
     *
     * The dot matrix rounds down to a whole pixel per cell, because a grid
     * scaled by a fraction stops being a grid — the dots land on different
     * subpixel offsets and the rows sample unevenly. An outline font has no
     * such constraint and keeps the fractional size, which at small sizes is
     * the difference between fitting a city name and truncating it.
     */
    fun quantize(cellPx: Float): Float

    /**
     * Draws [text] with the top-left of its line box at [origin], and
     * returns the width it occupied.
     *
     * [unlitColor] paints the dark grid behind the message under Retro and
     * is ignored under Vintage, which has no grid to paint. Callers pass it
     * unconditionally rather than branching, so that the one place styles
     * differ stays inside the implementations.
     */
    fun DrawScope.drawRun(
        text: String,
        cellPx: Float,
        origin: Offset,
        litColor: Color,
        unlitColor: Color,
    ): Float

    // -- fitting, shared by both styles ------------------------------------

    /** Whether text drawn at [cellPx] is worth drawing at all. */
    fun isLegible(cellPx: Float): Boolean = cellPx >= minLegibleCellPx

    /**
     * The least space, in cells, to leave between two columns of a row —
     * a city name and its time, a timer's label and its countdown.
     *
     * A space character is the natural unit, since it is what the style
     * itself considers a gap. The floor matters for the serif: EB Garamond's
     * space is about a quarter of an em, which set between a truncated name
     * and a right-aligned time reads as a collision rather than as a column.
     */
    val columnGapCells: Float get() = widthPx(" ", 1f).coerceAtLeast(2f)

    /**
     * The largest size at which [text] fits the given box.
     *
     * Both axes are honoured, so a wide short widget scales on its height
     * and a tall narrow one on its width without either needing to know
     * which case it is in.
     *
     * Width is derived from a single measurement at unit size because width
     * is linear in [cellPx] under both styles — a dot glyph is a fixed
     * number of cells, and an outline glyph advances proportionally to its
     * point size. That keeps this a division rather than a search.
     */
    fun cellPxForText(
        text: String,
        availableWidthPx: Float,
        availableHeightPx: Float,
        maxCellPx: Float = Float.MAX_VALUE,
    ): Float {
        val perUnit = widthPx(text, 1f)
        val byWidth = if (perUnit > 0f) availableWidthPx / perUnit else Float.MAX_VALUE
        val byHeight = availableHeightPx / cellHeight
        val fits = quantize(minOf(byWidth, byHeight))
        return fits.coerceIn(MIN_CELL_PX, maxOf(MIN_CELL_PX, quantize(maxCellPx)))
    }

    /** The same, for a bare grid of [columns] x [rows] rather than a string. */
    fun cellPxForGrid(
        columns: Float,
        rows: Int,
        availableWidthPx: Float,
        availableHeightPx: Float,
    ): Float {
        if (columns <= 0f || rows <= 0) return MIN_CELL_PX
        val fits = quantize(minOf(availableWidthPx / columns, availableHeightPx / rows))
        return fits.coerceAtLeast(MIN_CELL_PX)
    }

    /**
     * Truncates [text] to what fits in [availableWidthPx] at [cellPx].
     *
     * Plain truncation with no ellipsis: at these sizes an ellipsis costs
     * three cells to say what the clipped edge already says, and city names
     * are the only place this fires.
     *
     * Walks characters from the end rather than dividing by a fixed advance,
     * because Vintage is proportional — an `I` and a `W` are not the same
     * width, and the fixed-stride arithmetic that is exactly right for the
     * grid would cut a serif line several characters short.
     */
    fun truncateToWidth(text: String, cellPx: Float, availableWidthPx: Float): String {
        if (text.isEmpty() || availableWidthPx <= 0f) return ""
        if (widthPx(text, cellPx) <= availableWidthPx) return text
        var end = text.length
        while (end > 0 && widthPx(text.take(end), cellPx) > availableWidthPx) end--
        return text.take(end)
    }

    companion object {
        /** Absolute floor, so a degenerate size still renders something. */
        const val MIN_CELL_PX: Float = 1f

        /** Rounds down to a whole pixel. Used by the grid styles. */
        fun wholePixels(cellPx: Float): Float = floor(cellPx)
    }
}
