package app.auriel.basalt.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.auriel.basalt.core.dotmatrix.DotShape

/**
 * Picks a palette by showing it rather than naming it.
 *
 * Each row carries a swatch drawn in that theme's *own* colours — ground,
 * chrome and the energised accent, in the proportions the app uses them.
 * A list of seven words would tell the user nothing: "BREM" is not a colour
 * anyone can picture, and neither, really, is "Basalt".
 */
@Composable
fun ThemePicker(
    selected: BasaltThemeId,
    onSelect: (BasaltThemeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasaltThemes.all.forEach { theme ->
            ThemeRow(
                theme = theme,
                selected = theme == selected,
                onSelect = { onSelect(theme) },
            )
        }
    }
}

@Composable
private fun ThemeRow(
    theme: BasaltThemeId,
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
        Swatch(theme = theme, selected = selected)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            BasaltText(
                text = theme.displayName,
                cellSize = 1.6f,
                shape = DotShape.Chunky,
                litColor = if (selected) colors.copper else colors.silver,
                unlitColor = Color.Transparent,
            )
            BasaltText(
                text = theme.tagline,
                cellSize = 1.1f,
                shape = DotShape.Chunky,
                litColor = colors.pewter,
                unlitColor = Color.Transparent,
            )
        }
        SelectionMark(selected = selected)
    }
}

/**
 * The theme in miniature: its ground, a band of its chrome, and a lit
 * accent. Deliberately drawn from the *previewed* theme's tokens, not the
 * current one, so the row is a sample and not a label.
 */
@Composable
private fun Swatch(theme: BasaltThemeId, selected: Boolean) {
    val current = LocalBasaltColors.current
    val preview = theme.colors
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 34.dp)
            .background(preview.ink)
            .border(
                width = 1.dp,
                color = if (selected) current.copper else current.bronzeDeep,
            )
            .padding(4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(preview.copper),
            )
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(3.dp)
                    .background(preview.silver),
            )
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(3.dp)
                    .background(preview.pewter),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(modifier = Modifier.size(6.dp).background(preview.copperHot))
                Box(modifier = Modifier.size(6.dp).background(preview.emberAlarm))
                Box(modifier = Modifier.size(6.dp).background(preview.patina))
            }
        }
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
