package app.auriel.basalt.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Basalt's palette.
 *
 * These are not Material roles and do not map onto one. The scheme has two
 * jobs and keeps them separate: copper reads as *energised* (a running
 * timer, an armed alarm, the selected tab) and silver reads as
 * *structural* (chrome, engraved labels, information at rest).
 */
@Immutable
data class BasaltColors(
    val ink: Color,
    val ironOxide: Color,
    val bronzeDeep: Color,
    val bronze: Color,
    val copper: Color,
    val copperHot: Color,
    val patina: Color,
    val silver: Color,
    val silverBright: Color,
    val pewter: Color,
    val emberAlarm: Color,
) {
    /** Unlit dot-matrix cells: the grid stays visible, barely. */
    val unlit: Color get() = pewter.copy(alpha = 0.08f)

    companion object {
        /** The dark chassis. Default, and the one the design is drawn for. */
        val Forge = BasaltColors(
            ink = Color(0xFF0B0A09),
            ironOxide = Color(0xFF171310),
            bronzeDeep = Color(0xFF3E2C1C),
            bronze = Color(0xFF7A5230),
            copper = Color(0xFFB87333),
            copperHot = Color(0xFFE0925A),
            patina = Color(0xFF4E7A6A),
            silver = Color(0xFFC9CDD2),
            silverBright = Color(0xFFF2F4F6),
            pewter = Color(0xFF6E7479),
            emberAlarm = Color(0xFFD0472B),
        )
    }
}
