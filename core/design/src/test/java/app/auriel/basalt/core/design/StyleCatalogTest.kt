package app.auriel.basalt.core.design

import app.auriel.basalt.core.design.type.BasaltTypeface
import app.auriel.basalt.core.design.type.DotMatrixTypeface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two UI styles, and the contract they both have to answer.
 *
 * Only the dot matrix can be measured here — EB Garamond needs a real
 * `android.graphics.Typeface`, which a JVM unit test has no way to load, so
 * its metrics are exercised on device rather than pretended at with a stub
 * that would agree with whatever the code did.
 *
 * What *is* worth pinning down without a device is the part that made a
 * second style possible at all: that fitting is expressed in cells, that
 * width is linear in cell size, and that the fallbacks behave. Those are the
 * properties [app.auriel.basalt.widget.render.FaceRenderer] relies on to
 * lay out both styles with one set of rules.
 */
class StyleCatalogTest {

    private val retro: BasaltTypeface = DotMatrixTypeface()

    // -- the catalogue ----------------------------------------------------

    @Test
    fun `ids are stable`() {
        // These strings are in users' DataStore. Changing one silently moves
        // everybody who chose it onto the default.
        assertEquals("retro", BasaltStyleId.RETRO.id)
        assertEquals("vintage", BasaltStyleId.VINTAGE.id)
    }

    @Test
    fun `ids are unique`() {
        val ids = BasaltStyles.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `an unknown id falls back rather than failing`() {
        // What a downgrade looks like, or a style that was removed.
        assertSame(BasaltStyles.Default, BasaltStyles.byId("copperplate"))
        assertSame(BasaltStyles.Default, BasaltStyles.byId(null))
        assertSame(BasaltStyles.Default, BasaltStyles.byId(""))
    }

    @Test
    fun `a known id resolves to itself`() {
        BasaltStyles.all.forEach { style ->
            assertSame(style, BasaltStyles.byId(style.id))
        }
    }

    @Test
    fun `the default is the grid`() {
        // The app was drawn for it, and an upgrade must not silently
        // re-letter a home screen.
        assertSame(BasaltStyleId.RETRO, BasaltStyles.Default)
        assertTrue(BasaltStyleId.RETRO.isMatrix)
        assertTrue(!BasaltStyleId.VINTAGE.isMatrix)
    }

    @Test
    fun `every style is presentable`() {
        BasaltStyles.all.forEach { style ->
            assertTrue(style.displayName.isNotBlank())
            assertTrue(style.tagline.isNotBlank())
            // The picker sets both in the style's own lettering, so a name
            // the glyph set cannot render is a name nobody can read.
            assertEquals(style.displayName, style.displayName.uppercase())
        }
    }

    // -- the measurement contract -----------------------------------------

    @Test
    fun `width is linear in cell size`() {
        // The whole fitting path divides by the unit width instead of
        // searching for a size. That is only correct if width scales.
        val unit = retro.widthPx("21:45", 1f)
        assertEquals(unit * 4f, retro.widthPx("21:45", 4f), 0.001f)
        assertEquals(unit * 9.5f, retro.widthPx("21:45", 9.5f), 0.001f)
    }

    @Test
    fun `a line box is cellHeight cells tall`() {
        // The contract that lets one layout serve both styles.
        assertEquals(7, retro.cellHeight)
        assertEquals(7f * 3f, retro.heightPx(3f), 0.001f)
    }

    @Test
    fun `empty text has no width`() {
        assertEquals(0f, retro.widthPx("", 5f), 0.001f)
    }

    @Test
    fun `the grid fits whole pixels only`() {
        // A fractionally scaled matrix stops being a matrix: the dots land
        // on different subpixel offsets and the rows sample unevenly.
        val cell = retro.cellPxForText("12:00", availableWidthPx = 100f, availableHeightPx = 100f)
        assertEquals(cell, kotlin.math.floor(cell), 0.0f)
    }

    @Test
    fun `fitting honours whichever axis runs out first`() {
        // A wide short widget scales on its height and a tall narrow one on
        // its width, without either having to know which case it is in.
        val wide = retro.cellPxForText("12:00", availableWidthPx = 1000f, availableHeightPx = 21f)
        assertEquals(3f, wide, 0.001f)

        val tall = retro.cellPxForText("12:00", availableWidthPx = 29f, availableHeightPx = 1000f)
        assertTrue(retro.widthPx("12:00", tall) <= 29f)
    }

    @Test
    fun `a fitted run stays inside its box whenever a real size exists`() {
        val text = "SATURDAY 14 MARCH"
        for (width in listOf(240f, 501f, 1000f, 4000f)) {
            val cell = retro.cellPxForText(text, width, 4000f)
            assertTrue("expected room for a real size at width $width", cell > BasaltTypeface.MIN_CELL_PX)
            assertTrue(
                "run overflowed at width $width",
                retro.widthPx(text, cell) <= width + 0.001f,
            )
        }
    }

    /**
     * The floor wins over the fit, and that is the intended order.
     *
     * Squeezed hard enough there is no size that both fits the box and
     * renders anything at all, and [BasaltTypeface.MIN_CELL_PX] resolves it
     * in favour of drawing something. The overflow is not a bug being
     * documented — it is why callers ask [BasaltTypeface.isLegible] before
     * committing to a run, and why the renderer drops a line rather than
     * drawing it at whatever came back.
     */
    @Test
    fun `an unfittable run is clamped to the floor and reported illegible`() {
        val cell = retro.cellPxForText("SATURDAY 14 MARCH", availableWidthPx = 40f, availableHeightPx = 400f)
        assertEquals(BasaltTypeface.MIN_CELL_PX, cell, 0.001f)
        assertTrue(!retro.isLegible(cell))
    }

    @Test
    fun `fitting never returns a size below the floor`() {
        // A degenerate box still has to render something rather than divide
        // its way to zero.
        val cell = retro.cellPxForText("12:00", availableWidthPx = 0f, availableHeightPx = 0f)
        assertTrue(cell >= BasaltTypeface.MIN_CELL_PX)
    }

    @Test
    fun `legibility has a floor`() {
        assertTrue(retro.isLegible(3f))
        assertTrue(!retro.isLegible(2.9f))
    }

    @Test
    fun `truncation stops at what fits`() {
        val text = "REYKJAVIK"
        val room = retro.widthPx("REYK", 2f)
        val cut = retro.truncateToWidth(text, 2f, room)
        assertTrue(cut.isNotEmpty())
        assertTrue(text.startsWith(cut))
        assertTrue(retro.widthPx(cut, 2f) <= room + 0.001f)
    }

    @Test
    fun `truncation leaves text that already fits alone`() {
        val text = "OSLO"
        val room = retro.widthPx(text, 3f) + 10f
        assertSame(text, retro.truncateToWidth(text, 3f, room))
    }

    @Test
    fun `truncation to nothing is empty rather than negative`() {
        assertEquals("", retro.truncateToWidth("OSLO", 3f, 0f))
        assertEquals("", retro.truncateToWidth("OSLO", 3f, -50f))
    }

    @Test
    fun `a column gap is wide enough to read as a gap`() {
        // Rows put a name on the left and a time on the right; without this
        // the two collide the moment the name is truncated to fit.
        assertTrue(retro.columnGapCells >= 2f)
        assertNotEquals(0f, retro.columnGapCells)
    }
}
