package app.auriel.basalt.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalBasaltColors = staticCompositionLocalOf { BasaltThemes.Default.colors }

/** The selected theme, for the few places that need its name rather than its colours. */
val LocalBasaltTheme = staticCompositionLocalOf { BasaltThemes.Default }

/**
 * The selected UI style — the lettering, independent of the palette.
 *
 * Read by [BasaltText] and by nothing else in the common case: a screen
 * states what it wants to say and the style decides what the letters look
 * like. The few places that branch on it directly do so because a serif
 * needs different *spacing* than a grid, not different content.
 */
val LocalBasaltStyle = staticCompositionLocalOf { BasaltStyles.lastKnown }

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
    style: BasaltStyleId = BasaltStyles.lastKnown,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalBasaltColors provides theme.colors,
        LocalBasaltTheme provides theme,
        LocalBasaltStyle provides style,
        content = content,
    )
}
