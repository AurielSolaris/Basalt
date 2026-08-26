package app.auriel.basalt.widget

import androidx.compose.ui.graphics.Color
import app.auriel.basalt.core.design.BasaltColors
import app.auriel.basalt.core.design.BasaltThemes

/**
 * Widgets render through the same palette as the app.
 *
 * Glance's own theming is bypassed entirely: a widget that themed itself
 * from the launcher would stop matching the app, and matching the app is
 * the whole point of a dot-matrix chassis.
 *
 * Still scaffolding — no providers are registered yet. The ten widgets are
 * built on a shared bitmap render path once the features they display
 * exist.
 *
 * [BasaltThemes.lastKnown] rather than a fixed palette, so that once the
 * providers do exist a widget rendered from a live process already matches
 * the app. A widget rendered from a cold process gets the default, which is
 * the same problem the theme mirror solves for activities and which the
 * render path will solve the same way.
 */
object WidgetPalette {
    val colors: BasaltColors get() = BasaltThemes.lastKnown.colors
    val background: Color get() = colors.ink
}
