package app.auriel.basalt.widget.glance

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import app.auriel.basalt.core.design.BasaltStyles
import app.auriel.basalt.core.design.type.BasaltTypefaces
import app.auriel.basalt.widget.WidgetPalette
import app.auriel.basalt.widget.catalog.BasaltWidget
import app.auriel.basalt.widget.data.WidgetSnapshot
import app.auriel.basalt.widget.data.WidgetSnapshots
import app.auriel.basalt.widget.face.WidgetFace
import app.auriel.basalt.widget.face.WidgetFaces
import app.auriel.basalt.widget.render.FaceRenderer
import app.auriel.basalt.widget.render.FaceRenderer.drawFace
import app.auriel.basalt.widget.render.WidgetCanvas

/**
 * Every Basalt widget, once.
 *
 * There are ten entries in the catalogue and one implementation of a widget,
 * which is the whole claim the module makes. A `BasaltWidget` value decides
 * what is read, what is drawn, where a tap goes and how often it updates;
 * nothing about being an app widget is written down ten times.
 *
 * The content is a single [Image] of a bitmap rather than a tree of Glance
 * views. That is not a shortcut — it is the only way to keep the dot matrix.
 * `RemoteViews` has no drawing primitives, so a Glance-composed widget would
 * be platform text in a platform typeface, and a clock app whose widget does
 * not look like the app is a clock app with a stranger on the home screen.
 */
class BasaltGlanceWidget(private val widget: BasaltWidget) : GlanceAppWidget() {

    /**
     * Exact, not responsive.
     *
     * `SizeMode.Responsive` asks for a fixed set of breakpoints and picks the
     * nearest, which is right for a layout made of views and wrong for one
     * that measures itself: every face here already fits whatever box it is
     * given, so a breakpoint would only round the size off before the
     * renderer got to use it.
     */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read outside provideContent: this is the suspending part, and it
        // must not happen again on every recomposition triggered by a resize.
        val snapshot = WidgetSnapshots.take(context, widget.needs)
        val face = WidgetFaces.build(widget, snapshot)
        provideContent { Content(snapshot, face) }
    }

    @Composable
    private fun Content(snapshot: WidgetSnapshot, face: WidgetFace) {
        val context = LocalContext.current
        val size = LocalSize.current
        val density = context.resources.displayMetrics.density
        val colors = WidgetPalette.of(snapshot.themeId)
        // Resolved here rather than inside the renderer because loading a
        // font needs a Context and the renderer deliberately has none — it
        // takes a box, a face and the things that draw them, and nothing else.
        val type = BasaltTypefaces.of(BasaltStyles.byId(snapshot.uiStyleId), context)

        val bitmap = WidgetCanvas.render(
            widthPx = (size.width.value * density).toInt(),
            heightPx = (size.height.value * density).toInt(),
            density = density,
        ) { rendered ->
            drawFace(rendered, face, colors, type)
        }

        Box(modifier = GlanceModifier.fillMaxSize()) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = face.contentDescription,
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(openIntent(context))),
            )

            // The control strip is invisible here on purpose: the buttons are
            // already drawn into the bitmap, and these are only the touch
            // targets laid over them. The band is computed from the same
            // constant the renderer used, so what is pressed is what is seen.
            if (face.controls.isNotEmpty()) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height((size.height.value * FaceRenderer.CONTROL_STRIP_FRACTION).dp),
                    ) {
                        face.controls.forEach { control ->
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxHeight()
                                    .clickable(
                                        actionRunCallback<WidgetControlAction>(
                                            actionParametersOf(
                                                ControlKey to control.action.name,
                                            ),
                                        ),
                                    ),
                            ) {
                                Spacer(modifier = GlanceModifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Where a tap lands.
     *
     * Each widget opens the section it is a widget for, not the app's start
     * destination — tapping a stopwatch widget to be shown the clock is the
     * kind of small wrongness that makes a widget feel unfinished.
     */
    private fun openIntent(context: Context): Intent =
        Intent(Intent.ACTION_MAIN)
            .setClassName(context.packageName, LAUNCH_ACTIVITY)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .putExtra(EXTRA_ROUTE, widget.opens.route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    companion object {
        /**
         * Named rather than referenced, because `:widget` does not depend on
         * `:app` and must not: the app already depends on this module, and
         * the other direction would be a cycle. A string across that seam is
         * the usual price, and the manifest keeps the two honest.
         */
        const val LAUNCH_ACTIVITY = "app.auriel.basalt.BasaltActivity"

        /** Which section to show. Read by `BasaltActivity`. */
        const val EXTRA_ROUTE = "app.auriel.basalt.widget.ROUTE"

        val ControlKey = ActionParameters.Key<String>("basalt.control")
    }
}
