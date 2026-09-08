package app.auriel.basalt.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.auriel.basalt.core.dotmatrix.DotShape

/**
 * Basalt's controls.
 *
 * All bespoke, because banning Material means banning its buttons and
 * switches too. Each one is a piece of the same chassis: a metal plate with
 * a bright top edge, carrying a dot-matrix label.
 */

/** A pressable brass plate. Copper-lit when [emphasised]. */
@Composable
fun BasaltButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
    enabled: Boolean = true,
    cellSize: Float = 1.8f,
) {
    val colors = LocalBasaltColors.current
    val edge = when {
        !enabled -> colors.bronzeDeep
        emphasised -> colors.copper
        else -> colors.bronze
    }
    val ink = when {
        !enabled -> colors.pewter.copy(alpha = 0.5f)
        emphasised -> colors.copperHot
        else -> colors.silver
    }
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(listOf(colors.ironOxide, colors.ink)),
            )
            .border(1.dp, edge)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasaltText(
            text = label,
            cellSize = cellSize,
            shape = DotShape.Chunky,
            litColor = ink,
            unlitColor = Color.Transparent,
        )
    }
}

/**
 * A two-position brass switch.
 *
 * Reads as a physical throw rather than a slider: the lit half is the
 * position the lever is in.
 */
@Composable
fun BasaltToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = LocalBasaltColors.current
    Canvas(
        modifier = modifier
            .size(width = 40.dp, height = 22.dp)
            .clickable(
                role = Role.Switch,
                onClickLabel = contentDescription,
                onClick = { onCheckedChange(!checked) },
            ),
    ) {
        val trackInset = size.height * 0.18f
        drawRect(
            color = if (checked) colors.bronze else colors.ironOxide,
            topLeft = androidx.compose.ui.geometry.Offset(0f, trackInset),
            size = androidx.compose.ui.geometry.Size(size.width, size.height - trackInset * 2),
        )
        val leverWidth = size.width * 0.46f
        val left = if (checked) size.width - leverWidth else 0f
        drawRect(
            color = if (checked) colors.copper else colors.pewter,
            topLeft = androidx.compose.ui.geometry.Offset(left, 0f),
            size = androidx.compose.ui.geometry.Size(leverWidth, size.height),
        )
        // Specular top edge, as everywhere else in the chassis.
        drawRect(
            color = colors.silverBright.copy(alpha = 0.22f),
            topLeft = androidx.compose.ui.geometry.Offset(left, 0f),
            size = androidx.compose.ui.geometry.Size(leverWidth, size.height * 0.10f),
        )
    }
}

/**
 * A brass up/down stepper.
 *
 * Basalt has no time picker yet — that is a multi-day bespoke component —
 * so stepping is how a duration or an alarm time gets entered for now.
 * Every field it drives wraps rather than clamps, which is what makes it
 * usable at all with one thumb.
 */
@Composable
fun BasaltStepper(
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier,
    arrowSize: Dp = 18.dp,
    label: String? = null,
) {
    val colors = LocalBasaltColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Arrow(
            pointingUp = true,
            color = colors.copper,
            size = arrowSize,
            onClick = onUp,
            clickLabel = label?.let { "Increase $it" },
        )
        Arrow(
            pointingUp = false,
            color = colors.copper,
            size = arrowSize,
            onClick = onDown,
            clickLabel = label?.let { "Decrease $it" },
        )
    }
}

@Composable
private fun Arrow(
    pointingUp: Boolean,
    color: Color,
    size: Dp,
    onClick: () -> Unit,
    clickLabel: String?,
) {
    Canvas(
        modifier = Modifier
            .size(size)
            .clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick),
    ) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            if (pointingUp) {
                moveTo(w / 2f, h * 0.15f)
                lineTo(w * 0.88f, h * 0.78f)
                lineTo(w * 0.12f, h * 0.78f)
            } else {
                moveTo(w / 2f, h * 0.85f)
                lineTo(w * 0.88f, h * 0.22f)
                lineTo(w * 0.12f, h * 0.22f)
            }
            close()
        }
        drawPath(path, color)
    }
}
