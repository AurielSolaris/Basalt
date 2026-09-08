package app.auriel.basalt.core.design

/**
 * The UI styles, and the identity each one is persisted under.
 *
 * A style is the *lettering*, and nothing else. It is deliberately a second
 * axis rather than more themes: a theme answers "what colour is this app"
 * and a style answers "what shape are its letters", and the seven themes all
 * have to work under both styles. Folding the two together would have meant
 * fourteen themes and a picker nobody could read.
 *
 * The split exists because the dot matrix turned out to be load-bearing for
 * some people and a dealbreaker for others. A grid of LEDs is a strong
 * opinion, and a clock is something you look at more often than you look at
 * almost anything else on the device. Vintage is for the readers who wanted
 * the chrome without the pixels.
 *
 * [id] is storage, [displayName] is the chrome, and the two are separate for
 * the same reason they are separate on themes: renaming a style in the picker
 * must never silently move every user who had it selected onto a different
 * one.
 */
enum class BasaltStyleId(
    val id: String,
    val displayName: String,
    val tagline: String,
) {
    /**
     * The original: hand-authored 5x7 glyphs on a lit/unlit grid.
     *
     * Still the default, and still what the app was drawn for. Every
     * measurement in the chrome is a multiple of a cell.
     */
    RETRO(
        id = "retro",
        displayName = "RETRO",
        tagline = "DOT MATRIX. LIT AND UNLIT CELLS",
    ),

    /**
     * EB Garamond, an old-style serif cut from Claude Garamond's sixteenth
     * century romans.
     *
     * Chosen over the obvious alternatives because it is the one revival
     * that stays readable at a clock's sizes. A display serif set at
     * two hundred points for the hero readout and at twelve for a city
     * name has to hold both ends, and EB Garamond's larger optical sizes do.
     */
    VINTAGE(
        id = "vintage",
        displayName = "VINTAGE",
        tagline = "EB GARAMOND. INK ON A PRESSED PAGE",
    ),
    ;

    /** Whether this style draws through the dot grid. */
    val isMatrix: Boolean get() = this == RETRO
}

object BasaltStyles {

    /** The one the app was drawn for. */
    val Default: BasaltStyleId = BasaltStyleId.RETRO

    val all: List<BasaltStyleId> = BasaltStyleId.entries.toList()

    /**
     * Resolves a persisted id, falling back rather than failing.
     *
     * Same contract as [BasaltThemes.byId], and for the same reasons: an
     * unrecognised id is what a downgrade looks like, and falling back
     * leaves the app usable without overwriting the stored choice.
     */
    fun byId(id: String?): BasaltStyleId =
        all.firstOrNull { it.id == id } ?: Default

    /**
     * The last style this process saw, for the frames that happen before
     * anything has been read from disk.
     *
     * The counterpart to [BasaltThemes.lastKnown], and needed in exactly the
     * same places: the alarm screen and the window background are both
     * painted before DataStore can answer, and a readout that changes
     * typeface one frame in is as jarring as one that changes colour.
     */
    @Volatile
    var lastKnown: BasaltStyleId = Default
        private set

    fun remember(style: BasaltStyleId) {
        lastKnown = style
    }
}
