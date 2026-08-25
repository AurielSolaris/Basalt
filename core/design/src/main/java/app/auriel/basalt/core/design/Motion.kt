package app.auriel.basalt.core.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Basalt's two easing curves. Material's curves are deliberately absent.
 *
 * Anything discrete — a digit advancing, a tab committing — uses
 * [MechanicalStep]: it arrives fast and stops hard, the way a solenoid
 * does. Anything continuous — a panel opening, a reveal — uses [SteamEase],
 * which starts slowly and trails off.
 */
object BasaltMotion {
    val MechanicalStep: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val SteamEase: Easing = CubicBezierEasing(0.6f, 0f, 0.2f, 1f)

    const val StepDurationMillis: Int = 120
    const val PanelDurationMillis: Int = 320
}
