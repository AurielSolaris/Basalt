package app.auriel.basalt.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalBasaltColors = staticCompositionLocalOf { BasaltThemes.Default.colors }

/** The selected theme, for the few places that need its name rather than its colours. */
val LocalBasaltTheme = staticCompositionLocalOf { BasaltThemes.Default }

/**
 * Installs the palette. There is no MaterialTheme underneath — Basalt is
 * built on foundation + ui only, so this is the whole theme surface.
 *
 * The locals are `static`, so changing the theme invalidates everything
 * that read one rather than tracking reads individually. That is the right
 * trade here: the palette changes when a user picks a different one, which
 * is roughly never, and reading a colour happens in every composable in the
 * app, which is constantly.
 */
@Composable
fun BasaltTheme(
    theme: BasaltThemeId = BasaltThemes.lastKnown,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalBasaltColors provides theme.colors,
        LocalBasaltTheme provides theme,
        content = content,
    )
}
