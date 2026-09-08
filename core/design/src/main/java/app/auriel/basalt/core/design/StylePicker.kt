package app.auriel.basalt.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.auriel.basalt.core.dotmatrix.DotShape

/**
 * Picks the lettering by showing it.
 *
 * The same principle as [ThemePicker], applied to the other axis: each row
 * is set *in the style it offers* rather than in the style currently
 * installed. "EB GARAMOND" written in dots tells you nothing about what
 * choosing it would do, and a style picker is the one list in the app where
 * the label and the sample can be the same object.
 *
 * That is why every run here passes an explicit `style` instead of letting
 * it come from [LocalBasaltStyle] — these are the only call sites in the app
 * that deliberately ignore the installed style.
 */
@Composable
fun StylePicker(
    selected: BasaltStyleId,
    onSelect: (BasaltStyleId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasaltStyles.all.forEach { style ->
            StyleRow(
                style = style,
                selected = style == selected,
                onSelect = { onSelect(style) },
            )
        }
    }
}

@Composable
private fun StyleRow(
    style: BasaltStyleId,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = LocalBasaltColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .background(if (selected) colors.ironOxide else Color.Transparent)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Specimen(style = style, selected = selected)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            BasaltText(
                text = style.displayName,
                cellSize = 1.6f,
                shape = DotShape.Chunky,
                litColor = if (selected) colors.copper else colors.silver,
                unlitColor = Color.Transparent,
                style = style,
            )
            BasaltText(
                text = style.tagline,
                cellSize = 1.1f,
                shape = DotShape.Chunky,
                litColor = colors.pewter,
                unlitColor = Color.Transparent,
                style = style,
            )
        }
        SelectionMark(selected = selected)
    }
}

/**
 * The style in miniature: a time, set the way this style would set it.
 *
 * A fixed string rather than the current time, because the row is a
 * specimen and not a clock — a picker whose swatches tick is a picker that
 * costs a redraw a second to look at. `10:08` is the traditional watch
 * display time, which is a small joke and also happens to exercise both a
 * narrow glyph and a round one.
 */
@Composable
private fun Specimen(style: BasaltStyleId, selected: Boolean) {
    val colors = LocalBasaltColors.current
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 34.dp)
            .background(colors.ink)
            .border(
                width = 1.dp,
                color = if (selected) colors.copper else colors.bronzeDeep,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasaltText(
            text = "10:08",
            cellSize = 3.4f,
            litColor = colors.copper,
            unlitColor = Color.Transparent,
            style = style,
        )
    }
}

/** A filled bore for the selected row, an empty one for the rest. */
@Composable
private fun SelectionMark(selected: Boolean) {
    val colors = LocalBasaltColors.current
    Box(
        modifier = Modifier
            .size(14.dp)
            .border(width = 1.dp, color = if (selected) colors.copper else colors.pewter),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(modifier = Modifier.size(6.dp).background(colors.copperHot))
        }
    }
}
