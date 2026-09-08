package app.auriel.basalt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.auriel.basalt.core.design.BasaltIconGlyph
import app.auriel.basalt.core.design.BezelPanel
import app.auriel.basalt.core.design.LocalBasaltColors
import app.auriel.basalt.core.design.BasaltText
import app.auriel.basalt.core.dotmatrix.DotShape

/**
 * The bottom navigation strip: an engraved brass plate carrying one
 * instrument glyph per section.
 *
 * The icon does the identifying and the dot-matrix label confirms it. That
 * split is what lets the label stay small enough for five sections to fit a
 * phone without scrolling, and it is why these labels use the square cell
 * shape: round dots turn to mush below about 2dp, square ones hold an edge.
 *
 * The selected tab is marked three ways, none of them colour alone: a
 * copper glyph, a brighter label, and a lit bar beneath it.
 */
@Composable
fun BasaltTabBar(
    current: String?,
    onSelect: (BasaltDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalBasaltColors.current
    BezelPanel(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            BasaltDestination.Tabs.forEach { destination ->
                val selected = destination.route == current
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = selected,
                            onClick = { onSelect(destination) },
                        )
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BasaltIconGlyph(
                        icon = destination.icon,
                        color = if (selected) colors.copper else colors.pewter,
                        size = 22.dp,
                        strokeWidth = if (selected) 1.7.dp else 1.5.dp,
                    )
                    BasaltText(
                        text = destination.label,
                        cellSize = 1.35f,
                        shape = DotShape.Chunky,
                        litColor = if (selected) colors.silverBright else colors.pewter,
                        // The unlit grid is dropped here: at this cell size
                        // it competes with the letterforms instead of
                        // sitting behind them.
                        unlitColor = Color.Transparent,
                        contentDescription = "",
                    )
                    // The lit bar is present for every tab so the strip
                    // keeps its rhythm, and energised only for the selected
                    // one.
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .background(if (selected) colors.copper else colors.bronzeDeep),
                    )
                }
            }
        }
    }
}
