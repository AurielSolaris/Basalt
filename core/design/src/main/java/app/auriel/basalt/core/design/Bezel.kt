package app.auriel.basalt.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * A metal panel with an inset bezel.
 *
 * Depth comes from two hairlines — a bright specular edge along the top and
 * a dark edge along the bottom — rather than from a blurred drop shadow.
 * Blur reads as glass; Basalt is milled brass.
 *
 * On a light ground the two swap ends. "Bright on top" is not the rule; the
 * rule is that the lit edge faces the light, and on Quartz the ground is
 * already brighter than any highlight, so the panel only reads as raised if
 * the shadow is the one on top.
 */
@Composable
fun BezelPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalBasaltColors.current
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(colors.ironOxide, colors.ink),
                ),
            )
            .drawBehind {
                val hairline = 1.dp.toPx()
                val specular = colors.silverBright.copy(alpha = 0.16f)
                val shadow = colors.bronzeDeep
                val top = if (colors.isLight) shadow else specular
                val bottom = if (colors.isLight) specular else shadow
                drawLine(
                    color = top,
                    start = Offset(0f, hairline / 2f),
                    end = Offset(size.width, hairline / 2f),
                    strokeWidth = hairline,
                )
                drawLine(
                    color = bottom,
                    start = Offset(0f, size.height - hairline / 2f),
                    end = Offset(size.width, size.height - hairline / 2f),
                    strokeWidth = hairline,
                )
            }
            .padding(1.dp),
        content = content,
    )
}
