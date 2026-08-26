package app.auriel.basalt.core.dotmatrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The 5x7 base set.
 *
 * Layout is measured before it is drawn, so [DotMatrixFont.measure] has to
 * agree exactly with what the renderer walks — a disagreement of one cell
 * clips the last character of every label in the app.
 */
class DotMatrixFontTest {

    private val font = DotMatrixFont.Base5x7

    /** Every lit cell, flattened, so two glyphs can be compared. */
    private fun signature(glyph: Glyph): String = buildString {
        for (y in 0 until glyph.height) {
            for (x in 0 until glyph.width) append(if (glyph.isLit(x, y)) '#' else '.')
        }
    }

    @Test
    fun `the base set is five by seven with one column of tracking`() {
        assertEquals(5, font.cellWidth)
        assertEquals(7, font.cellHeight)
        assertEquals(1, font.tracking)
    }

    @Test
    fun `an empty string measures zero`() {
        assertEquals(0, font.measure(""))
    }

    @Test
    fun `a single character measures one cell with no trailing tracking`() {
        assertEquals(5, font.measure("A"))
    }

    @Test
    fun `tracking goes between characters, not after them`() {
        assertEquals(11, font.measure("AB"))
        assertEquals(17, font.measure("ABC"))
        assertEquals(5 * 8 + 7, font.measure("12:34:56"))
    }

    @Test
    fun `measure grows by one cell plus tracking per character`() {
        var previous = font.measure("A")
        for (length in 2..40) {
            val current = font.measure("A".repeat(length))
            assertEquals(font.cellWidth + font.tracking, current - previous)
            previous = current
        }
    }

    @Test
    fun `spaces are measured like any other character`() {
        // The space is a real glyph in the set, so a label does not close up
        // its own gaps.
        assertEquals(font.measure("AAA"), font.measure("A A"))
    }

    @Test
    fun `lookup is case-insensitive`() {
        for (c in 'A'..'Z') {
            assertSame("$c", font[c], font[c.lowercaseChar()])
        }
    }

    @Test
    fun `every digit and letter is present and distinct`() {
        // Any character the font is missing falls back to the question mark,
        // so a missing glyph shows up here as a duplicate signature rather
        // than as a crash.
        val characters = ('0'..'9') + ('A'..'Z')
        val signatures = characters.associateWith { signature(font[it]) }
        val question = signature(font['?'])

        characters.filter { it != '?' }.forEach {
            assertNotEquals("$it fell back to the question mark", question, signatures.getValue(it))
        }
        assertEquals(
            "two characters share a glyph",
            characters.size,
            signatures.values.toSet().size,
        )
    }

    @Test
    fun `the punctuation the app actually uses is present`() {
        val punctuation = ":.,-+/!()%°"
        val question = signature(font['?'])
        punctuation.forEach {
            assertNotEquals("$it is missing from the base set", question, signature(font[it]))
        }
    }

    @Test
    fun `an unknown character falls back rather than throwing`() {
        assertSame(font['?'], font['☃'])
    }

    @Test
    fun `every glyph fits the cell`() {
        val characters = (' '..'~').toList() + '°'
        characters.forEach { c ->
            val glyph = font[c]
            assertEquals("$c is the wrong width", font.cellWidth, glyph.width)
            assertEquals("$c is the wrong height", font.cellHeight, glyph.height)
        }
    }

    @Test
    fun `the space glyph is entirely unlit`() {
        assertTrue(signature(font[' ']).none { it == '#' })
    }
}
