package app.auriel.basalt.widget

import androidx.compose.ui.graphics.Color
import app.auriel.basalt.core.design.BasaltColors
import app.auriel.basalt.core.design.BasaltThemeId
import app.auriel.basalt.core.design.BasaltThemes
import app.auriel.basalt.widget.face.FaceRole

/**
 * Widgets render through the same palette as the app.
 *
 * Glance's own theming is bypassed entirely. A widget that themed itself
 * from the launcher's wallpaper — which is what Material You widgets do, and
 * what all three of the OEM clocks this catalogue is drawn from do — would
 * stop matching the app the moment the user picked a theme, and matching the
 * app is the whole point of putting a dot-matrix chassis on a home screen.
 *
 * The theme is resolved from the stored id at render time, so a widget drawn
 * from a cold process gets the right palette without waiting for DataStore.
 * [lastKnown] remains the fallback for the same reason it exists for
 * activities: worst case is one frame, and a widget's frames last a minute.
 */
object WidgetPalette {

    /** The palette a widget should draw in, from the stored theme id. */
    fun of(themeId: String?): BasaltColors = theme(themeId).colors

    fun theme(themeId: String?): BasaltThemeId = BasaltThemes.byId(themeId)

    /** The palette in force in this process, for callers with no id to hand. */
    val colors: BasaltColors get() = BasaltThemes.lastKnown.colors

    val background: Color get() = colors.ink

    /**
     * Which token a [FaceRole] takes.
     *
     * The single place roles become colours. Faces name roles and never
     * colours, which is what lets all seven themes — two of them light —
     * answer the same face without any of them special-casing it.
     */
    fun color(role: FaceRole, colors: BasaltColors): Color = when (role) {
        FaceRole.Primary -> colors.copper
        FaceRole.Hot -> colors.copperHot
        FaceRole.Label -> colors.silver
        FaceRole.Caption -> colors.pewter
        FaceRole.Alert -> colors.emberAlarm
        FaceRole.Cool -> colors.patina
    }

    /**
     * The unlit grid behind a run of text.
     *
     * Suppressed below the legibility floor: at three pixels a cell the
     * unlit dots stop reading as the texture
     * behind the message and start reading as noise on top of it, and the
     * message is what the widget is for.
     */
    fun unlit(colors: BasaltColors, cellPx: Float): Color =
        if (cellPx >= 4f) colors.unlit else Color.Transparent
}
