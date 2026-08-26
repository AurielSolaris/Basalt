package app.auriel.basalt.core.design

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The four palettes.
 *
 * A theme is data, so the things worth checking are the ones a person
 * reading hex codes cannot check: that no palette leaves a role unreadable
 * on its own ground, that the stored ids are stable, and that an id nobody
 * recognises falls back instead of failing.
 *
 * Contrast is measured with the WCAG relative-luminance formula, against
 * two different bars. Roles that carry running text — [BasaltColors.silver]
 * for labels and [BasaltColors.pewter] for captions — are held to 4.0:1.
 * Accent roles are held to 3.0:1, the large-text bar, because that is what
 * they are: chunky headings, section rules and the big dot-matrix readouts.
 * A fully saturated red cannot reach 4.5:1 on black, and BREM is not
 * negotiable on being red.
 */
class ThemeCatalogTest {

    private companion object {
        const val TEXT_CONTRAST = 4.0
        const val ACCENT_CONTRAST = 3.0
    }

    private fun channel(value: Float): Double {
        val c = value.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)

    /** Straight-line distance through sRGB. Crude, but hue-aware. */
    private fun distance(a: Color, b: Color): Double = sqrt(
        ((a.red - b.red).toDouble()).pow(2) +
            ((a.green - b.green).toDouble()).pow(2) +
            ((a.blue - b.blue).toDouble()).pow(2),
    )

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    // -- the catalog ------------------------------------------------------

    @Test
    fun `the catalog is the expected list, alphabetically`() {
        val ids = BasaltThemes.all.map { it.id }
        assertEquals(
            listOf("basalt", "brem", "copper", "latte", "mocha", "quartz", "solar"),
            ids,
        )
        assertEquals("the picker is not in order", ids.sorted(), ids)
    }

    @Test
    fun `copper is the default`() {
        assertEquals(BasaltThemeId.COPPER, BasaltThemes.Default)
        assertEquals(BasaltColors.Copper, BasaltThemes.Default.colors)
    }

    @Test
    fun `ids are unique, lower case and free of separators`() {
        // The id is what ends up in DataStore. Keeping it boring means it
        // never has to be escaped, migrated or case-folded.
        val ids = BasaltThemes.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach {
            assertEquals(it, it.lowercase())
            assertTrue("'$it' should be plain", it.all(Char::isLetterOrDigit))
        }
    }

    @Test
    fun `every id round-trips`() {
        BasaltThemes.all.forEach { assertSame(it, BasaltThemes.byId(it.id)) }
    }

    @Test
    fun `an unknown id falls back rather than failing`() {
        // This is what a downgrade looks like, and what a removed theme
        // looks like. Neither is worth a crash.
        assertSame(BasaltThemes.Default, BasaltThemes.byId(null))
        assertSame(BasaltThemes.Default, BasaltThemes.byId(""))
        assertSame(BasaltThemes.Default, BasaltThemes.byId("obsidian"))
        assertSame(BasaltThemes.Default, BasaltThemes.byId("COPPER"))
    }

    @Test
    fun `every theme is named and described`() {
        BasaltThemes.all.forEach {
            assertTrue(it.displayName.isNotBlank())
            assertTrue(it.tagline.isNotBlank())
            assertEquals(it.displayName, it.displayName.uppercase())
            assertEquals(it.tagline, it.tagline.uppercase())
        }
    }

    @Test
    fun `the taglines are renderable in the base set`() {
        // Every label in the app goes through the 5x7 font, and a character
        // it does not have silently becomes a question mark.
        val renderable = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 :.,-+/?!\"()<>=*#%&@_[];").toSet()
        BasaltThemes.all.forEach { theme ->
            (theme.displayName + theme.tagline).forEach {
                assertTrue("'$it' is not in the base glyph set", it in renderable)
            }
        }
    }

    // -- the palettes -----------------------------------------------------

    @Test
    fun `the light themes are the ones that say they are`() {
        assertEquals(
            listOf(BasaltThemeId.LATTE, BasaltThemeId.QUARTZ),
            BasaltThemes.all.filter { it.isLight },
        )
    }

    @Test
    fun `isLight agrees with the ground it describes`() {
        // The flag drives the system bar icons. Getting it out of step with
        // the actual ground is how an app ends up with white icons on white.
        BasaltThemes.all.forEach { theme ->
            val bright = luminance(theme.colors.ink) > 0.5
            assertEquals("${theme.id} disagrees about its ground", bright, theme.isLight)
        }
    }

    @Test
    fun `no two themes share a ground`() {
        val grounds = BasaltThemes.all.map { it.colors.ink }
        assertEquals(grounds.size, grounds.toSet().size)
    }

    @Test
    fun `text roles are readable on their own ground`() {
        BasaltThemes.all.forEach { theme ->
            val ink = theme.colors.ink
            listOf(
                "silver" to theme.colors.silver,
                "silverBright" to theme.colors.silverBright,
                "pewter" to theme.colors.pewter,
            ).forEach { (name, color) ->
                val ratio = contrast(ink, color)
                assertTrue(
                    "${theme.id}.$name is %.2f:1 against ink".format(ratio),
                    ratio >= TEXT_CONTRAST,
                )
            }
        }
    }

    @Test
    fun `accent roles clear the large-text bar on their own ground`() {
        BasaltThemes.all.forEach { theme ->
            val ink = theme.colors.ink
            listOf(
                "bronze" to theme.colors.bronze,
                "copper" to theme.colors.copper,
                "copperHot" to theme.colors.copperHot,
                "patina" to theme.colors.patina,
                "emberAlarm" to theme.colors.emberAlarm,
            ).forEach { (name, color) ->
                val ratio = contrast(ink, color)
                assertTrue(
                    "${theme.id}.$name is %.2f:1 against ink".format(ratio),
                    ratio >= ACCENT_CONTRAST,
                )
            }
        }
    }

    @Test
    fun `raised panels stay readable too`() {
        // BezelPanel fills with ironOxide, so anything drawn on a panel is
        // not sitting on ink. A palette that only works on the ground would
        // fail on every settings row.
        BasaltThemes.all.forEach { theme ->
            val panel = theme.colors.ironOxide
            listOf(theme.colors.silver, theme.colors.copper, theme.colors.pewter).forEach {
                assertTrue(
                    "${theme.id} loses contrast on a panel",
                    contrast(panel, it) >= ACCENT_CONTRAST,
                )
            }
        }
    }

    @Test
    fun `the panel is distinguishable from the ground but not loudly`() {
        BasaltThemes.all.forEach { theme ->
            val ratio = contrast(theme.colors.ink, theme.colors.ironOxide)
            assertNotEquals(theme.colors.ink, theme.colors.ironOxide)
            assertTrue("${theme.id}'s panel is a second background", ratio < 2.0)
        }
    }

    @Test
    fun `energised is distinct from structural`() {
        // The whole scheme rests on copper meaning "running" and silver
        // meaning "chrome". If a palette lets them converge, the app stops
        // being able to say anything.
        //
        // Distance, not contrast, for the same reason as the alarm colour:
        // Mocha's peach and its subtext sit at the same lightness and are
        // plainly a different colour. Luminance cannot see that.
        BasaltThemes.all.forEach { theme ->
            val apart = distance(theme.colors.copper, theme.colors.silver)
            assertTrue(
                "${theme.id} cannot tell energised from structural (%.3f apart)".format(apart),
                apart >= 0.25,
            )
        }
    }

    @Test
    fun `the alarm colour is not the accent colour`() {
        // Deliberately *not* a contrast ratio. Contrast is luminance only,
        // and these two are separated by hue: Copper's alarm red and its
        // accent orange sit at nearly the same lightness and are obviously
        // different colours. A plain distance through sRGB is a crude
        // measure but it is measuring the right thing.
        BasaltThemes.all.forEach { theme ->
            val distance = distance(theme.colors.emberAlarm, theme.colors.copper)
            assertTrue(
                "${theme.id}'s alarm reads as an accent (%.3f apart)".format(distance),
                distance >= 0.12,
            )
        }
    }

    @Test
    fun `hotter is further from the ground than warm`() {
        // "Hot" means more contrast, in both directions: brighter on a dark
        // ground, darker on a light one.
        BasaltThemes.all.forEach { theme ->
            val warm = contrast(theme.colors.ink, theme.colors.copper)
            val hot = contrast(theme.colors.ink, theme.colors.copperHot)
            assertTrue("${theme.id}'s copperHot is not hotter than its copper", hot > warm)
        }
    }

    @Test
    fun `every colour is fully opaque`() {
        // Alpha is applied at the draw site, deliberately, so that a token
        // composited twice does not quietly get darker.
        BasaltThemes.all.forEach { theme ->
            tokensOf(theme.colors).forEach { (name, color) ->
                assertEquals("${theme.id}.$name is translucent", 1f, color.alpha, 0.0001f)
            }
        }
    }

    @Test
    fun `no palette repeats a colour across roles`() {
        BasaltThemes.all.forEach { theme ->
            val tokens = tokensOf(theme.colors)
            assertEquals(
                "${theme.id} uses one colour for two roles",
                tokens.size,
                tokens.map { it.second }.toSet().size,
            )
        }
    }

    @Test
    fun `unlit cells are faint but present`() {
        BasaltThemes.all.forEach { theme ->
            val unlit = theme.colors.unlit
            assertTrue("${theme.id}'s unlit grid is invisible", unlit.alpha > 0f)
            assertTrue("${theme.id}'s unlit grid is too loud", unlit.alpha < 0.25f)
        }
    }

    @Test
    fun `no two themes are the same palette wearing a different name`() {
        val palettes = BasaltThemes.all.map { it.colors }
        assertEquals(palettes.size, palettes.toSet().size)
    }

    // -- the in-process cache ---------------------------------------------

    @Test
    fun `the remembered theme starts at the default and follows what it is told`() {
        try {
            assertFalse(BasaltThemes.lastKnown.isLight)
            BasaltThemes.remember(BasaltThemeId.QUARTZ)
            assertSame(BasaltThemeId.QUARTZ, BasaltThemes.lastKnown)
        } finally {
            BasaltThemes.remember(BasaltThemes.Default)
        }
    }

    private fun tokensOf(colors: BasaltColors): List<Pair<String, Color>> = listOf(
        "ink" to colors.ink,
        "ironOxide" to colors.ironOxide,
        "bronzeDeep" to colors.bronzeDeep,
        "bronze" to colors.bronze,
        "copper" to colors.copper,
        "copperHot" to colors.copperHot,
        "patina" to colors.patina,
        "silver" to colors.silver,
        "silverBright" to colors.silverBright,
        "pewter" to colors.pewter,
        "emberAlarm" to colors.emberAlarm,
    )
}
