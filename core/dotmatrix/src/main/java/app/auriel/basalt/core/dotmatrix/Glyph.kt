package app.auriel.basalt.core.dotmatrix

/**
 * A single character rendered as a fixed grid of on/off cells.
 *
 * Rows are stored as bitmasks, most-significant bit = leftmost column.
 * A glyph is immutable and shared; the renderer never mutates it.
 */
class Glyph(
    val width: Int,
    val height: Int,
    private val rows: IntArray,
) {
    init {
        require(rows.size == height) { "expected $height rows, got ${rows.size}" }
    }

    /** True when the cell at [x],[y] is lit. Out-of-bounds reads are unlit. */
    fun isLit(x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height) return false
        val shift = width - 1 - x
        return (rows[y] ushr shift) and 1 == 1
    }

    companion object {
        /**
         * Builds a glyph from an ASCII picture, one string per row, where
         * '#' (or any non-space, non-'.') marks a lit cell. Authoring
         * glyphs this way keeps the source readable; the strings are
         * folded to bitmasks once, at class-init time.
         */
        fun of(vararg picture: String): Glyph {
            val height = picture.size
            val width = picture.maxOf { it.length }
            val rows = IntArray(height) { y ->
                val row = picture[y]
                var mask = 0
                for (x in 0 until width) {
                    val c = row.getOrElse(x) { ' ' }
                    mask = mask shl 1
                    if (c != ' ' && c != '.') mask = mask or 1
                }
                mask
            }
            return Glyph(width, height, rows)
        }
    }
}
