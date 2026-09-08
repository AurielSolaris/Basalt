package app.auriel.basalt.widget.face

import app.auriel.basalt.widget.catalog.BasaltWidget

/**
 * What a widget shows, before anything knows how big it is.
 *
 * This is the join between the two halves of the module. On one side, ten
 * faces each decide what they want to say — pure functions of a snapshot,
 * with no drawing and no Android in them. On the other, one renderer knows
 * how to draw a hero and some lines into whatever box the launcher provided.
 *
 * Keeping the split here is what makes "a widget is a configuration" true
 * rather than aspirational. Adding an eleventh widget means adding a row to
 * the catalogue and a `build` branch that returns one of these; it does not
 * mean touching the renderer, the update pass, the providers or the
 * manifest.
 *
 * [contentDescription] is not optional and not derived. A dots-only widget
 * is completely opaque to a screen reader, so every face states in plain
 * words what it is showing, and the renderer attaches it to the image.
 */
data class WidgetFace(
    val widget: BasaltWidget,
    val hero: FaceHero,
    /** Supporting lines, drawn under the hero, in order. */
    val lines: List<FaceLine> = emptyList(),
    /**
     * Buttons along the bottom edge, if this face has any.
     *
     * Google's stopwatch and timer widgets both act rather than only
     * report, and a clock widget you have to open the app to use is half a
     * widget. Controls are part of the face's *configuration* — the face
     * declares them, the renderer draws the strip, and the Glance layer
     * lays clickable regions over exactly that strip. No face contains a
     * button and no button contains a layout.
     */
    val controls: List<FaceControl> = emptyList(),
    val contentDescription: String,
)

/** One button in the strip along the bottom of a face. */
data class FaceControl(val action: WidgetControl, val label: String)

/**
 * What a control does.
 *
 * An enum rather than a lambda because the click arrives in a different
 * process, minutes later, in a receiver that has never seen this face — so
 * the only thing that can cross the gap is a name.
 */
enum class WidgetControl {
    /** Start the stopwatch, or stop it if it is running. */
    StopwatchToggle,

    /** Record a lap. Only offered while it is running. */
    StopwatchLap,
}

/**
 * The dominant element.
 *
 * Five shapes cover all ten widgets, which is the point: the ten OEM ideas
 * are ten *arrangements* of a much smaller set of primitives, and once the
 * primitives exist the eleventh idea is nearly free.
 */
sealed interface FaceHero {

    /** One run of text, as large as it will go. Digital, next alarm. */
    data class Text(val text: String, val role: FaceRole = FaceRole.Primary) : FaceHero

    /** Two runs, one above the other, each as large as it will go. Stacked. */
    data class Stack(
        val top: String,
        val bottom: String,
        val role: FaceRole = FaceRole.Primary,
    ) : FaceHero

    /** An etched dial with hands. Analog. */
    data class Dial(
        val hour: Int,
        val minute: Int,
        /** Drawn only when the face is big enough to make it readable. */
        val showTicks: Boolean = true,
    ) : FaceHero

    /**
     * An arc filled to [fraction], with [readout] written inside it.
     * Stopwatch and solar.
     */
    data class Gauge(
        val fraction: Float,
        val readout: String,
        val role: FaceRole = FaceRole.Primary,
    ) : FaceHero {
        init {
            require(fraction in 0f..1f) { "gauge fraction out of range: $fraction" }
        }
    }

    /** A list, leading text left and trailing text right. World, dual, timers. */
    data class Rows(val rows: List<FaceRow>) : FaceHero
}

/**
 * One row of a [FaceHero.Rows].
 *
 * [emphasis] rather than a second role, because "this row is the one that
 * matters" is a different question from "what colour is this row", and the
 * dual face needs both at once: home versus away is emphasis, day versus
 * night is role.
 */
data class FaceRow(
    val leading: String,
    val trailing: String,
    val role: FaceRole = FaceRole.Primary,
    val emphasis: Boolean = false,
)

/** A supporting line under the hero. */
data class FaceLine(val text: String, val role: FaceRole = FaceRole.Label)

/**
 * Which palette token a piece of text takes.
 *
 * Roles, not colours, for the same reason the app's palette is roles: seven
 * themes have to be able to answer these, and two of them are light themes
 * where "the bright one" is nearly black.
 */
enum class FaceRole {
    /** The reading itself. */
    Primary,

    /** The reading, energised — something is running. */
    Hot,

    /** A heading or a name beside a value. */
    Label,

    /** Secondary detail; the first thing dropped when space runs out. */
    Caption,

    /** An alarm, or a timer past zero. */
    Alert,

    /** Cool counterpart to [Hot]; night side of the dual face. */
    Cool,
}
