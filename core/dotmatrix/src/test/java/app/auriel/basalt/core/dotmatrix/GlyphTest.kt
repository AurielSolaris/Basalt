package app.auriel.basalt.core.dotmatrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ASCII-picture-to-bitmask fold.
 *
 * Glyphs are authored as pictures so the source stays readable, which puts
 * a small transformation between what an author writes and what the
 * renderer draws. This is that transformation.
 */
class GlyphTest {

    @Test
    fun `a hash lights a cell and a dot does not`() {
        val glyph = Glyph.of(
            "#.#",
            ".#.",
        )
        assertTrue(glyph.isLit(0, 0))
        assertFalse(glyph.isLit(1, 0))
        assertTrue(glyph.isLit(2, 0))
        assertFalse(glyph.isLit(0, 1))
        assertTrue(glyph.isLit(1, 1))
    }

    @Test
    fun `a space is unlit and anything else is lit`() {
        val glyph = Glyph.of("# .X")
        assertTrue(glyph.isLit(0, 0))
        assertFalse(glyph.isLit(1, 0))
        assertFalse(glyph.isLit(2, 0))
        assertTrue(glyph.isLit(3, 0))
    }

    @Test
    fun `the leftmost column is the most significant bit`() {
        // Getting this backwards mirrors every glyph in the font, which is
        // the kind of bug that reads as "the typeface looks odd".
        val glyph = Glyph.of("#....")
        assertTrue(glyph.isLit(0, 0))
        (1..4).forEach { assertFalse(glyph.isLit(it, 0)) }
    }

    @Test
    fun `dimensions come from the picture`() {
        val glyph = Glyph.of("....", "....", "....")
        assertEquals(4, glyph.width)
        assertEquals(3, glyph.height)
    }

    @Test
    fun `a ragged picture takes the widest row and pads the rest unlit`() {
        val glyph = Glyph.of("###", "#")
        assertEquals(3, glyph.width)
        assertTrue(glyph.isLit(0, 1))
        assertFalse(glyph.isLit(1, 1))
        assertFalse(glyph.isLit(2, 1))
    }

    @Test
    fun `out of bounds reads are unlit rather than an exception`() {
        // The renderer walks a fixed cell grid, so it asks about cells a
        // narrower glyph does not have. Answering "unlit" is the useful
        // answer; throwing would mean bounds checks at every draw site.
        val glyph = Glyph.of("#")
        assertFalse(glyph.isLit(-1, 0))
        assertFalse(glyph.isLit(0, -1))
        assertFalse(glyph.isLit(1, 0))
        assertFalse(glyph.isLit(0, 1))
        assertFalse(glyph.isLit(99, 99))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a row count that disagrees with the height is rejected`() {
        Glyph(width = 3, height = 2, rows = intArrayOf(0b111))
    }

    @Test
    fun `a fully lit glyph lights every cell`() {
        val glyph = Glyph.of("#####", "#####")
        for (y in 0 until glyph.height) {
            for (x in 0 until glyph.width) {
                assertTrue("($x,$y) should be lit", glyph.isLit(x, y))
            }
        }
    }
}
