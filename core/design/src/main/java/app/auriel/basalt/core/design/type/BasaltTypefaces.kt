package app.auriel.basalt.core.design.type

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import app.auriel.basalt.core.design.BasaltStyleId
import app.auriel.basalt.core.design.R
import app.auriel.basalt.core.dotmatrix.DotMatrixFont
import app.auriel.basalt.core.dotmatrix.DotShape
import app.auriel.basalt.core.dotmatrix.drawDotText

/**
 * Retro: the hand-authored 5x7 grid.
 *
 * A thin adapter rather than a reimplementation — the measuring and the
 * drawing both go straight through [DotMatrixFont] and [drawDotText], so the
 * grid has exactly one implementation and this file cannot drift from it.
 */
class DotMatrixTypeface(
    private val font: DotMatrixFont = DotMatrixFont.Base5x7,
    private val shape: DotShape = DotShape.Round,
) : BasaltTypeface {

    override val cellHeight: Int get() = font.cellHeight

    override fun widthPx(text: String, cellPx: Float): Float =
        font.measure(text) * cellPx

    /** Whole pixels only. A fractionally scaled grid stops being a grid. */
    override fun quantize(cellPx: Float): Float = BasaltTypeface.wholePixels(cellPx)

    override fun DrawScope.drawRun(
        text: String,
        cellPx: Float,
        origin: Offset,
        litColor: Color,
        unlitColor: Color,
    ): Float = drawDotText(
        text = text,
        font = font,
        cellPx = cellPx,
        litColor = litColor,
        unlitColor = unlitColor,
        origin = origin,
        shape = shape,
    )
}

/**
 * Vintage: EB Garamond, drawn as outlines.
 *
 * ### Why this measures itself instead of trusting a point size
 *
 * The contract [BasaltTypeface] sets is that a line box is [cellHeight]
 * cells tall whatever the style, so that a layout fitted under one style
 * still fits under the other. A font's point size is not its line height —
 * the relationship depends on the face's own ascent and descent — so the
 * ratio between the two is measured once, from the loaded font, and every
 * size after that is derived from it. Hard-coding the usual 1.2 would have
 * left Vintage sitting a few percent proud of its box, which on a stacked
 * hero reads as a misalignment rather than as a font choice.
 *
 * ### Why the paint is thread-local
 *
 * Widgets rasterise off the main thread, and several faces can be drawn at
 * once during an update pass. A shared mutable `Paint` whose text size is
 * set per call would be a data race that shows up as one widget rendered at
 * another's size — rare, wrong, and nearly impossible to reproduce. One
 * paint per thread costs a handful of objects and removes the question.
 */
class GaramondTypeface(private val typeface: Typeface) : BasaltTypeface {

    private val paints = object : ThreadLocal<Paint>() {
        override fun initialValue(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = this@GaramondTypeface.typeface
            // The app's strings are authored in upper case for the grid, and
            // upper case set solid is too tight in a serif. A little tracking
            // is what makes it read as lettering rather than as a word jam.
            letterSpacing = TRACKING
        }
    }

    private fun paint(): Paint = paints.get()!!

    /**
     * Point size per cell, and baseline offset per cell, measured once from
     * the face itself.
     */
    private val metrics: Metrics by lazy {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = this@GaramondTypeface.typeface
            textSize = REFERENCE_SIZE
            letterSpacing = TRACKING
        }
        val fm = p.fontMetrics
        val lineHeight = fm.descent - fm.ascent
        if (lineHeight <= 0f) {
            Metrics(sizePerCell = 1f, baselinePerCell = 1f)
        } else {
            // textSize that makes one line box exactly cellHeight cells tall,
            // expressed per cell so callers only ever multiply.
            val sizePerCell = cellHeight * REFERENCE_SIZE / lineHeight
            val baselinePerCell = cellHeight * -fm.ascent / lineHeight
            Metrics(sizePerCell, baselinePerCell)
        }
    }

    private data class Metrics(val sizePerCell: Float, val baselinePerCell: Float)

    /** The point size this face is drawn at, for a given cell size. */
    private fun textSizeFor(cellPx: Float): Float = metrics.sizePerCell * cellPx

    override fun widthPx(text: String, cellPx: Float): Float {
        if (text.isEmpty()) return 0f
        val p = paint()
        p.textSize = textSizeFor(cellPx)
        return p.measureText(text)
    }

    /**
     * Outlines are resolution-independent, so unlike the grid there is
     * nothing to snap to. Keeping the fractional size is what lets a serif
     * line fit a city name where a rounded-down one would truncate it.
     */
    override fun quantize(cellPx: Float): Float = cellPx

    /**
     * Serifs survive being small better than a 5x7 grid does — there are no
     * cells to lose — so Vintage stays legible a little further down.
     */
    override val minLegibleCellPx: Float get() = 2f

    override fun DrawScope.drawRun(
        text: String,
        cellPx: Float,
        origin: Offset,
        litColor: Color,
        /** No grid under an outline face; accepted and ignored by contract. */
        unlitColor: Color,
    ): Float {
        if (text.isEmpty()) return 0f
        val p = paint()
        p.textSize = textSizeFor(cellPx)
        p.color = litColor.toArgb()
        p.alpha = (litColor.alpha * 255f).toInt().coerceIn(0, 255)
        drawContext.canvas.nativeCanvas.drawText(
            text,
            origin.x,
            origin.y + metrics.baselinePerCell * cellPx,
            p,
        )
        return p.measureText(text)
    }

    private companion object {
        /**
         * Arbitrary size the face's proportions are measured at. Only the
         * ratios derived from it are used, so the value itself is a
         * precision choice and nothing more.
         */
        const val REFERENCE_SIZE = 100f

        /** Ems of extra tracking, for upper-case setting. */
        const val TRACKING = 0.04f
    }
}

/**
 * Resolving a style to the thing that draws it.
 *
 * Vintage needs a font loaded from resources and therefore a [Context];
 * Retro needs nothing, because its glyphs are Kotlin. That asymmetry is why
 * this is an object with a cache rather than a property on
 * [BasaltStyleId]: an enum cannot hold a `Context`, and loading an 800KB
 * face on every widget redraw would be the most expensive thing a widget
 * does.
 *
 * The cache is keyed on nothing — there is one font — and holds the
 * application context's font only. Font loading can fail on a device with a
 * damaged resource table, and a clock that will not draw is worse than one
 * drawn in the wrong style, so failure falls back to the grid.
 */
object BasaltTypefaces {

    /**
     * Retro has no dependencies, so its instances are shared — one per dot
     * shape, since the shape is a property of the face rather than of the
     * call and there are only ever the two.
     */
    val Retro: BasaltTypeface = DotMatrixTypeface(shape = DotShape.Round)
    private val RetroChunky: BasaltTypeface = DotMatrixTypeface(shape = DotShape.Chunky)

    @Volatile
    private var vintage: BasaltTypeface? = null

    /**
     * The typeface for [style], loading the font on first use.
     *
     * [shape] applies to Retro only — an outline face has no cells to shape —
     * and is accepted unconditionally so that callers do not have to know
     * which style is installed before deciding what to pass.
     *
     * Falls back to Retro if EB Garamond cannot be loaded. A style the
     * device cannot draw is not an error worth crashing a clock over.
     */
    fun of(
        style: BasaltStyleId,
        context: Context,
        shape: DotShape = DotShape.Round,
    ): BasaltTypeface = when (style) {
        BasaltStyleId.RETRO -> if (shape == DotShape.Chunky) RetroChunky else Retro
        BasaltStyleId.VINTAGE -> vintage ?: load(context).also { vintage = it }
    }

    private fun load(context: Context): BasaltTypeface {
        val face = runCatching {
            ResourcesCompat.getFont(context.applicationContext, R.font.eb_garamond)
        }.getOrNull()
        return if (face == null) Retro else GaramondTypeface(face)
    }
}
