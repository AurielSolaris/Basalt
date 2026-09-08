package app.auriel.basalt.widget.catalog

/**
 * A size in launcher grid cells.
 *
 * Widgets are declared in cells because that is the unit the user actually
 * drags in, but the framework wants dp, and the conversion is a convention
 * rather than an API: a widget spanning n cells is `70n - 30` dp, the extra
 * 30 being the gap the launcher puts between cells and does not give to the
 * widget. Getting this wrong by one cell is the difference between a widget
 * the user can place and one the picker silently refuses.
 *
 * Cells are also a lie on any launcher whose grid is not 70dp — which is
 * most OEM launchers, Samsung's included. That is fine and is the reason
 * every face is drawn from the *measured* size at render time rather than
 * from the declared one. The declaration only has to be close enough that
 * the picker offers a sensible default.
 */
data class WidgetCells(val columns: Int, val rows: Int) {
    init {
        require(columns >= 1 && rows >= 1) { "a widget cannot span $columns×$rows cells" }
    }

    val widthDp: Int get() = cellsToDp(columns)
    val heightDp: Int get() = cellsToDp(rows)

    override fun toString(): String = "${columns}×$rows"

    companion object {
        /** The launcher's nominal cell pitch, in dp. */
        const val CELL_DP = 70

        /** The gap the launcher keeps for itself, in dp. */
        const val GUTTER_DP = 30

        fun cellsToDp(cells: Int): Int = (CELL_DP * cells) - GUTTER_DP
    }
}
