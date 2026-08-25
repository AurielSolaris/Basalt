package app.auriel.basalt.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalBasaltColors = staticCompositionLocalOf { BasaltColors.Forge }

/**
 * Installs the palette. There is no MaterialTheme underneath — Basalt is
 * built on foundation + ui only, so this is the whole theme surface.
 */
@Composable
fun BasaltTheme(
    colors: BasaltColors = BasaltColors.Forge,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBasaltColors provides colors, content = content)
}
