package app.auriel.basalt.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Basalt's palette.
 *
 * These are not Material roles and do not map onto one. The scheme has two
 * jobs and keeps them separate: [copper] reads as *energised* (a running
 * timer, an armed alarm, the selected tab) and [silver] reads as
 * *structural* (chrome, engraved labels, information at rest).
 *
 * The token names are the ones the default palette was drawn from, and they
 * stay that way in every theme — a name here is a **role**, not a hue.
 * Quartz's `ink` is a warm white and its `silverBright` is nearly black,
 * because `ink` means "the ground everything sits on" and `silverBright`
 * means "the strongest thing you can write on it". Renaming eleven tokens
 * to something hue-neutral would cost every call site and buy nothing; the
 * one rule that matters is that no drawing code may assume a token is dark.
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
    /**
     * Whether [ink] is a light ground.
     *
     * Two things genuinely need to know, rather than being able to work in
     * terms of the tokens alone: the system status and navigation bars,
     * whose icons are drawn by the OS and have to be told which way round to
     * go, and the bezel's specular edge, which has to move from the top to
     * the bottom of a panel for the panel to still read as raised.
     */
    val isLight: Boolean = false,
) {
    /** Unlit dot-matrix cells: the grid stays visible, barely. */
    val unlit: Color get() = pewter.copy(alpha = if (isLight) 0.14f else 0.08f)

    companion object {

        /**
         * **Copper.** The dark chassis, and the one the design was drawn
         * for. Oxidised copper on iron, lit from inside.
         */
        val Copper = BasaltColors(
            ink = Color(0xFF0B0A09),
            ironOxide = Color(0xFF171310),
            bronzeDeep = Color(0xFF3E2C1C),
            bronze = Color(0xFF8A5E36),
            copper = Color(0xFFB87333),
            copperHot = Color(0xFFE0925A),
            patina = Color(0xFF4E7A6A),
            silver = Color(0xFFC9CDD2),
            silverBright = Color(0xFFF2F4F6),
            pewter = Color(0xFF6E7479),
            emberAlarm = Color(0xFFD0472B),
        )

        /**
         * **Basalt.** The stone the app is named after: cold grey rock and
         * an arc lamp.
         *
         * The energised colour goes *colder* than the structural one rather
         * than warmer, which is the opposite of Copper. It has to: in a
         * scheme with no hue to spare, the only way for "this is running"
         * to stay distinct from "this is chrome" is for it to be brighter
         * and bluer, the way a struck arc is against grey steel.
         */
        val Basalt = BasaltColors(
            ink = Color(0xFF0A0B0C),
            ironOxide = Color(0xFF16191C),
            bronzeDeep = Color(0xFF2A3036),
            bronze = Color(0xFF63727D),
            copper = Color(0xFF9FC7E0),
            copperHot = Color(0xFFDCEEF9),
            patina = Color(0xFF5F8A7E),
            silver = Color(0xFF8A9199),
            silverBright = Color(0xFFC6CDD3),
            pewter = Color(0xFF767E86),
            emberAlarm = Color(0xFFC4553C),
        )

        /**
         * **Quartz.** Milky stone in daylight. The only light palette.
         *
         * Everything inverts, and "hotter" therefore means *darker*:
         * [copperHot] is a deeper burnt orange than [copper], because on a
         * pale ground contrast is bought by going down, not up. The same
         * goes for [silverBright], which is the darkest ink in the set.
         */
        val Quartz = BasaltColors(
            ink = Color(0xFFF1EFEA),
            ironOxide = Color(0xFFE3DFD7),
            bronzeDeep = Color(0xFFC0B4A0),
            bronze = Color(0xFF7A6340),
            copper = Color(0xFF96500F),
            copperHot = Color(0xFF7A4312),
            patina = Color(0xFF2F6B58),
            silver = Color(0xFF2A2724),
            silverBright = Color(0xFF100E0C),
            pewter = Color(0xFF6E675E),
            emberAlarm = Color(0xFF9E2A12),
            isLight = true,
        )

        /**
         * **BREM — Blood Red Eldritch Machinery.** Mechanisms built from
         * the blood and organs of things that should not have had either.
         *
         * The hard part of a red scheme is that the alert colour has
         * nowhere left to go, so the roles are separated by *temperature*
         * instead: arterial red carries the ordinary energised state,
         * [emberAlarm] is a searing near-white orange that only ever means
         * something is ringing, and the structural tokens are bone rather
         * than steel.
         */
        val Brem = BasaltColors(
            ink = Color(0xFF080404),
            ironOxide = Color(0xFF170A0A),
            bronzeDeep = Color(0xFF3A1214),
            bronze = Color(0xFFB03D38),
            copper = Color(0xFFCE2027),
            copperHot = Color(0xFFE23B33),
            patina = Color(0xFF6E7A2E),
            silver = Color(0xFFC8BBAE),
            silverBright = Color(0xFFEFE6DA),
            pewter = Color(0xFF8A776F),
            emberAlarm = Color(0xFFFF7A55),
        )

        /**
         * **Mocha.** Catppuccin's dark flavour, mapped onto Basalt's roles.
         *
         * Not a drop-in: Catppuccin is a named-colour set (base, surface0,
         * peach, mauve) and Basalt is a role set, so this is a reading of
         * one in terms of the other. The greys map cleanly — base, surface0
         * and surface1 are already a ground, a panel and a hairline — and
         * the accents were chosen for the roles rather than for fidelity:
         * peach carries the energised state because it is the warm one, and
         * mauve carries headings because it is the flavour's signature.
         */
        val Mocha = BasaltColors(
            ink = Color(0xFF1E1E2E),
            ironOxide = Color(0xFF313244),
            bronzeDeep = Color(0xFF45475A),
            bronze = Color(0xFFCBA6F7),
            copper = Color(0xFFFAB387),
            copperHot = Color(0xFFF9E2AF),
            patina = Color(0xFFA6E3A1),
            silver = Color(0xFFBAC2DE),
            silverBright = Color(0xFFCDD6F4),
            pewter = Color(0xFF7F849C),
            emberAlarm = Color(0xFFF38BA8),
        )

        /**
         * **Latte.** Catppuccin's light flavour, and the harder of the two
         * to map honestly.
         *
         * Latte's accents are tuned to sit *beside* its base, not to be read
         * on it — peach at 2.6:1 is a decoration, not a label. Basalt puts
         * the energised colour into digits, so two values are darker than
         * the flavour ships: `patina` is a deepened green, and
         * `silverBright` is a deepened text, because the role means "the
         * strongest thing you can write" and Latte's text is already its
         * darkest. Blue and mauve carry warm and hot, in that order, because
         * that is the pair whose contrast happens to run the right way.
         */
        val Latte = BasaltColors(
            ink = Color(0xFFEFF1F5),
            ironOxide = Color(0xFFCCD0DA),
            bronzeDeep = Color(0xFFACB0BE),
            bronze = Color(0xFF7C7F93),
            copper = Color(0xFF1E66F5),
            copperHot = Color(0xFF8839EF),
            patina = Color(0xFF2F7D1F),
            silver = Color(0xFF4C4F69),
            silverBright = Color(0xFF383B52),
            pewter = Color(0xFF6C6F85),
            emberAlarm = Color(0xFFD20F39),
            isLight = true,
        )

        /**
         * **Solar.** Nordic slate, lit from somewhere warm.
         *
         * The polar-night greys and the snow-storm whites are Nord's, and
         * they are the reason it works: a cold, low-chroma chassis is
         * exactly what Basalt's chrome wants. What is not Nord is the
         * accent, which is a soft yellow pastel rather than the frost blues
         * — a slate room with one warm lamp in it, instead of a slate room
         * lit blue. `pewter` had to be lifted well above Nord's comment
         * grey, which is a beautiful colour and completely unreadable as
         * caption text on the base.
         */
        val Solar = BasaltColors(
            ink = Color(0xFF2E3440),
            ironOxide = Color(0xFF3B4252),
            bronzeDeep = Color(0xFF434C5E),
            bronze = Color(0xFFA99268),
            copper = Color(0xFFEBCB8B),
            copperHot = Color(0xFFF7E3AE),
            patina = Color(0xFFA3BE8C),
            silver = Color(0xFFD8DEE9),
            silverBright = Color(0xFFECEFF4),
            pewter = Color(0xFF949FB2),
            emberAlarm = Color(0xFFD07178),
        )
    }
}
