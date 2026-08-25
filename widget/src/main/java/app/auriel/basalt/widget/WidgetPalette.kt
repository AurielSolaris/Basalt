package app.auriel.basalt.widget

import androidx.compose.ui.graphics.Color
import app.auriel.basalt.core.design.BasaltColors

/**
 * Widgets render through the same palette as the app.
 *
 * Glance's own theming is bypassed entirely: a widget that themed itself
 * from the launcher would stop matching the app, and matching the app is
 * the whole point of a dot-matrix chassis.
 *
 * v0.1.0 is scaffolding only — no providers are registered yet. The ten
 * widgets are built on a shared bitmap render path once the features they
 * display exist.
 */
object WidgetPalette {
    val colors: BasaltColors = BasaltColors.Forge
    val background: Color get() = colors.ink
}
