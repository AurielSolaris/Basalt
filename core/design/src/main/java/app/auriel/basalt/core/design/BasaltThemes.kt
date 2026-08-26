package app.auriel.basalt.core.design

/**
 * The themes, and the identity each one is persisted under.
 *
 * [id] is storage, [displayName] is the chrome, and the two are separate on
 * purpose: renaming a theme in the picker must never silently move every
 * user who had it selected onto a different one.
 *
 * Declared in alphabetical order, which is also the order the picker lists
 * them in. The default is not first and does not need to be: a list of seven
 * is long enough that "where is the one I want" beats "which is the default",
 * and the selected row is marked anyway.
 */
enum class BasaltThemeId(
    val id: String,
    val displayName: String,
    val tagline: String,
    val colors: BasaltColors,
) {
    BASALT(
        id = "basalt",
        displayName = "BASALT",
        tagline = "COLD GREY STONE AND AN ARC LAMP",
        colors = BasaltColors.Basalt,
    ),
    BREM(
        id = "brem",
        displayName = "BREM",
        tagline = "BLOOD RED ELDRITCH MACHINERY",
        colors = BasaltColors.Brem,
    ),
    COPPER(
        id = "copper",
        displayName = "COPPER",
        tagline = "OXIDISED COPPER ON IRON",
        colors = BasaltColors.Copper,
    ),
    LATTE(
        id = "latte",
        displayName = "LATTE",
        tagline = "CATPPUCCIN LATTE. PAPER AND INK",
        colors = BasaltColors.Latte,
    ),
    MOCHA(
        id = "mocha",
        displayName = "MOCHA",
        tagline = "CATPPUCCIN MOCHA. INDIGO AND PASTEL",
        colors = BasaltColors.Mocha,
    ),
    QUARTZ(
        id = "quartz",
        displayName = "QUARTZ",
        tagline = "MILKY STONE IN DAYLIGHT",
        colors = BasaltColors.Quartz,
    ),
    SOLAR(
        id = "solar",
        displayName = "SOLAR",
        tagline = "NORDIC SLATE UNDER SOFT YELLOW",
        colors = BasaltColors.Solar,
    ),
    ;

    val isLight: Boolean get() = colors.isLight
}

object BasaltThemes {

    /** The one the design was drawn for. */
    val Default: BasaltThemeId = BasaltThemeId.COPPER

    val all: List<BasaltThemeId> = BasaltThemeId.entries.toList()

    /**
     * Resolves a persisted id, falling back rather than failing.
     *
     * An unknown id is not an error worth crashing over — it is what a
     * downgrade looks like, or a theme that was removed. Falling back to
     * [Default] leaves the app usable and the stored value untouched, so
     * upgrading again restores the user's choice.
     */
    fun byId(id: String?): BasaltThemeId =
        all.firstOrNull { it.id == id } ?: Default

    /**
     * The last theme this process saw, for the frames that happen before
     * anything has been read from disk.
     *
     * Settings live in DataStore, which is asynchronous by design, and two
     * surfaces cannot wait for it: the alarm screen, which is put in front
     * of someone who is asleep and must not spend a frame in the wrong
     * palette, and the activity window background, which is painted before
     * the first composition. [BasaltPreferences] keeps a synchronous mirror
     * on disk for a cold start; this is the in-process half of the same
     * idea, and both are caches of a value that is authoritative elsewhere.
     */
    @Volatile
    var lastKnown: BasaltThemeId = Default
        private set

    fun remember(theme: BasaltThemeId) {
        lastKnown = theme
    }
}
