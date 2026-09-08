package app.auriel.basalt.widget.glance

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import app.auriel.basalt.widget.catalog.BasaltWidget
import app.auriel.basalt.widget.update.BasaltWidgets

/**
 * The ten providers.
 *
 * This is the one place the catalogue cannot stay a list. A widget provider
 * is a `BroadcastReceiver` named in the manifest, and the manifest is parsed
 * by the package installer long before any Basalt code runs, so there has to
 * be a real class per entry — the system has no way to be handed an enum.
 *
 * They are therefore as thin as the framework permits: a class name, the
 * catalogue value it stands for, and nothing else. Every one of them shares
 * [BasaltGlanceWidget], and none of them contains a decision.
 *
 * The base class also hooks enable and disable, because "is anything
 * installed" is what the update pass schedules against. Placing the first
 * Basalt widget on a home screen is what starts the clock; removing the last
 * one is what stops it.
 */
abstract class BasaltWidgetReceiver(private val widget: BasaltWidget) : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget get() = BasaltGlanceWidget(widget)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // A newly placed widget has to get on the schedule, and the cadence
        // may have to change: the first stopwatch widget on the home screen
        // is what makes a per-second pass possible at all.
        BasaltWidgets.reschedule(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        BasaltWidgets.reschedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        BasaltWidgets.reschedule(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        BasaltWidgets.reschedule(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }
}

class AnalogWidgetReceiver : BasaltWidgetReceiver(BasaltWidget.ANALOG)
class DigitalWidgetReceiver : BasaltWidgetReceiver(BasaltWidget.DIGITAL)
class DualWidgetReceiver : BasaltWidgetReceiver(BasaltWidget.DUAL)
class NextAlarmWidgetReceiver : BasaltWidgetReceiver(BasaltWidget.NEXT_ALARM)
class QuickLookWidgetReceiver : BasaltWidgetReceiver(BasaltWidget.QUICK_LOOK)
class SolarWidgetReceiver : BasaltWidgetReceiver(BasaltWidget.SOLAR)
class StackedWidgetReceiver : BasaltWidgetReceiver(BasaltWidget.STACKED)
class StopwatchWidgetReceiver : BasaltWidgetReceiver(BasaltWidget.STOPWATCH)
class TimersWidgetReceiver : BasaltWidgetReceiver(BasaltWidget.TIMERS)
class WorldWidgetReceiver : BasaltWidgetReceiver(BasaltWidget.WORLD)

/**
 * Which receiver stands for which catalogue entry.
 *
 * The update pass needs the mapping in the other direction — given a
 * catalogue value, is anything of that kind on a home screen — and this is
 * the only table that knows it. Keep it in step with the classes above and
 * with the manifest; a test checks that all three agree in count.
 */
object WidgetReceivers {
    val byWidget: Map<BasaltWidget, Class<out BasaltWidgetReceiver>> = mapOf(
        BasaltWidget.ANALOG to AnalogWidgetReceiver::class.java,
        BasaltWidget.DIGITAL to DigitalWidgetReceiver::class.java,
        BasaltWidget.DUAL to DualWidgetReceiver::class.java,
        BasaltWidget.NEXT_ALARM to NextAlarmWidgetReceiver::class.java,
        BasaltWidget.QUICK_LOOK to QuickLookWidgetReceiver::class.java,
        BasaltWidget.SOLAR to SolarWidgetReceiver::class.java,
        BasaltWidget.STACKED to StackedWidgetReceiver::class.java,
        BasaltWidget.STOPWATCH to StopwatchWidgetReceiver::class.java,
        BasaltWidget.TIMERS to TimersWidgetReceiver::class.java,
        BasaltWidget.WORLD to WorldWidgetReceiver::class.java,
    )
}
