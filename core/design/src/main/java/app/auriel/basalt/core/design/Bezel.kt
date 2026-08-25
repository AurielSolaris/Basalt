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
                drawLine(
                    color = colors.silverBright.copy(alpha = 0.16f),
                    start = Offset(0f, hairline / 2f),
                    end = Offset(size.width, hairline / 2f),
                    strokeWidth = hairline,
                )
                drawLine(
                    color = colors.bronzeDeep,
                    start = Offset(0f, size.height - hairline / 2f),
                    end = Offset(size.width, size.height - hairline / 2f),
                    strokeWidth = hairline,
                )
            }
            .padding(1.dp),
        content = content,
    )
}
