package app.auriel.basalt.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.auriel.basalt.core.design.BasaltIcon
import app.auriel.basalt.core.design.BasaltIconGlyph
import app.auriel.basalt.core.design.LocalBasaltColors
import app.auriel.basalt.core.dotmatrix.DotMatrixText
import app.auriel.basalt.core.dotmatrix.DotShape

/**
 * The top plate: wordmark on the left, settings cog on the right.
 */
@Composable
fun BasaltTopBar(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    settingsSelected: Boolean = false,
) {
    val colors = LocalBasaltColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DotMatrixText(
            text = "BASALT",
            cellSize = 2f,
            shape = DotShape.Chunky,
            litColor = colors.silver,
            unlitColor = colors.unlit,
        )
        BasaltIconGlyph(
            icon = BasaltIcon.Settings,
            color = if (settingsSelected) colors.copper else colors.pewter,
            size = 22.dp,
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Settings",
                    onClick = onOpenSettings,
                )
                .padding(6.dp),
        )
    }
}
