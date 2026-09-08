package app.auriel.basalt.widget.glance

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import app.auriel.basalt.core.data.BasaltGraph
import app.auriel.basalt.core.data.model.Lap
import app.auriel.basalt.core.data.model.StopwatchState
import app.auriel.basalt.widget.face.WidgetControl
import app.auriel.basalt.widget.update.BasaltWidgets
import kotlinx.coroutines.flow.first

/**
 * A press on a widget's own button.
 *
 * The click arrives here in a process that may have been started for it,
 * with nothing but a string to say which button it was — which is why
 * [WidgetControl] is an enum and not a lambda. Everything else is looked up
 * again from the repositories, because whatever the widget was showing when
 * it was drawn is up to a minute old by the time anyone presses it.
 *
 * The transitions deliberately mirror `StopwatchViewModel`'s exactly. A
 * stopwatch that counts differently depending on whether it was started from
 * the app or from the home screen is not a stopwatch.
 */
class WidgetControlAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val control = parameters[BasaltGlanceWidget.ControlKey]
            ?.let { name -> WidgetControl.entries.firstOrNull { it.name == name } }
            ?: return

        val graph = BasaltGraph.get(context)
        val now = graph.timeSource.now().toEpochMilli()
        val stopwatch = graph.stopwatch.get()

        when (control) {
            WidgetControl.StopwatchToggle -> {
                graph.stopwatch.update(
                    if (stopwatch.isRunning) {
                        // Fold the current run into the accumulated total and
                        // drop the start mark, rather than leaving both set:
                        // a paused stopwatch with a live start mark keeps
                        // counting the moment anything reads it.
                        stopwatch.copy(
                            state = StopwatchState.Paused,
                            accumulatedMillis = stopwatch.elapsedMillis(now),
                            startedAtWallMillis = 0L,
                        )
                    } else {
                        stopwatch.copy(
                            state = StopwatchState.Running,
                            startedAtWallMillis = now,
                        )
                    },
                )
            }

            WidgetControl.StopwatchLap -> {
                if (!stopwatch.isRunning) return
                val total = stopwatch.elapsedMillis(now)
                val previous = graph.stopwatch.laps.first().firstOrNull()?.totalMillis ?: 0L
                graph.stopwatch.addLap(
                    Lap(
                        number = graph.stopwatch.lapCount() + 1,
                        lapMillis = total - previous,
                        totalMillis = total,
                    ),
                )
            }
        }

        // Redraw immediately rather than waiting for the next scheduled pass:
        // a button that visibly does nothing for up to a minute reads as a
        // button that did not work, and gets pressed again.
        BasaltWidgets.refresh(context)
    }
}
