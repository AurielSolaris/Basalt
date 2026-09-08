package app.auriel.basalt.widget

import app.auriel.basalt.widget.catalog.BasaltWidget
import app.auriel.basalt.widget.catalog.WidgetCells
import app.auriel.basalt.widget.catalog.WidgetData
import app.auriel.basalt.widget.catalog.WidgetTick
import app.auriel.basalt.widget.glance.WidgetReceivers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The catalogue's own invariants.
 *
 * The module's claim is that a widget is a configuration, which only holds
 * if the configuration is coherent. Everything checked here is something the
 * framework would otherwise discover on a user's home screen: a widget that
 * cannot be placed because its minimum exceeds its default, a face that
 * fetches nothing and so draws nothing, a provider with no class behind it.
 */
class WidgetCatalogTest {

    @Test
    fun `the catalog is the aggregate of the three, alphabetically`() {
        val ids = BasaltWidget.all.map { it.id }
        assertEquals(
            listOf(
                "analog", "digital", "dual", "next-alarm", "quick-look",
                "solar", "stacked", "stopwatch", "timers", "world",
            ),
            ids,
        )
        assertEquals("the picker is not in order", ids.sorted(), ids)
    }

    @Test
    fun `every widget names where it came from`() {
        // The catalogue is a deduplicated union of three OEM line-ups, and an
        // entry with no origin is an entry somebody invented — which may be a
        // fine idea, but is not what this list is.
        BasaltWidget.all.forEach { widget ->
            assertTrue(
                "${widget.id} has no origin",
                widget.origin.contains("Google") ||
                    widget.origin.contains("Samsung") ||
                    widget.origin.contains("Nothing"),
            )
        }
    }

    @Test
    fun `all three OEMs are actually represented`() {
        val origins = BasaltWidget.all.joinToString(" ") { it.origin }
        assertTrue(origins.contains("Google"))
        assertTrue(origins.contains("Samsung"))
        assertTrue(origins.contains("Nothing"))
    }

    @Test
    fun `ids and names are unique`() {
        assertEquals(BasaltWidget.all.size, BasaltWidget.all.map { it.id }.toSet().size)
        assertEquals(BasaltWidget.all.size, BasaltWidget.all.map { it.displayName }.toSet().size)
    }

    @Test
    fun `byId round trips, and an unknown id is null rather than a default`() {
        BasaltWidget.all.forEach { assertEquals(it, BasaltWidget.byId(it.id)) }
        // Unlike a theme, an unrecognised widget id has no sensible fallback:
        // silently drawing a clock where a stopwatch was asked for would be
        // worse than drawing nothing.
        assertNull(BasaltWidget.byId("no-such-widget"))
        assertNull(BasaltWidget.byId(null))
    }

    // -- sizing -----------------------------------------------------------

    @Test
    fun `every widget can be placed at its default and resized both ways`() {
        BasaltWidget.all.forEach { widget ->
            assertTrue(
                "${widget.id}: min ${widget.minCells} exceeds default ${widget.defaultCells}",
                widget.minCells.columns <= widget.defaultCells.columns &&
                    widget.minCells.rows <= widget.defaultCells.rows,
            )
            assertTrue(
                "${widget.id}: default ${widget.defaultCells} exceeds max ${widget.maxCells}",
                widget.defaultCells.columns <= widget.maxCells.columns &&
                    widget.defaultCells.rows <= widget.maxCells.rows,
            )
        }
    }

    @Test
    fun `nothing is declared larger than a phone home screen`() {
        // Five by five is the whole grid on most launchers. A widget that
        // cannot fit anywhere is one the picker offers and then refuses.
        BasaltWidget.all.forEach { widget ->
            assertTrue(widget.maxCells.columns <= 5)
            assertTrue(widget.maxCells.rows <= 5)
        }
    }

    @Test
    fun `cells convert to the launcher's dp convention`() {
        assertEquals(40, WidgetCells.cellsToDp(1))
        assertEquals(110, WidgetCells.cellsToDp(2))
        assertEquals(320, WidgetCells.cellsToDp(5))
    }

    @Test
    fun `a widget cannot span zero cells`() {
        listOf(0 to 1, 1 to 0, -1 to 2).forEach { (columns, rows) ->
            runCatching { WidgetCells(columns, rows) }
                .onSuccess { throw AssertionError("$columns×$rows was accepted") }
        }
    }

    // -- data and cadence -------------------------------------------------

    @Test
    fun `every face reads the clock, because every face is a clock face`() {
        BasaltWidget.all.forEach { widget ->
            assertTrue("${widget.id} reads nothing", widget.needs.isNotEmpty())
            assertTrue("${widget.id} does not read the time", WidgetData.Time in widget.needs)
        }
    }

    @Test
    fun `only the faces that can show motion ask for the fast lane`() {
        // A per-second cadence is only ever justified by something moving. A
        // clock face on the fast lane would be a wake-up a second for a
        // readout that changes once a minute.
        val fast = BasaltWidget.all.filter { it.tick == WidgetTick.SecondWhileRunning }
        assertEquals(
            listOf(BasaltWidget.QUICK_LOOK, BasaltWidget.STOPWATCH, BasaltWidget.TIMERS),
            fast,
        )
        fast.forEach { widget ->
            assertTrue(
                "${widget.id} wants seconds but reads nothing that moves",
                WidgetData.Timers in widget.needs || WidgetData.Stopwatch in widget.needs,
            )
        }
    }

    @Test
    fun `the union of needs is what the update pass fetches`() {
        assertEquals(
            setOf(WidgetData.Time),
            BasaltWidget.needsOf(listOf(BasaltWidget.ANALOG, BasaltWidget.STACKED)),
        )
        assertEquals(
            setOf(WidgetData.Time, WidgetData.Alarms, WidgetData.Cities),
            BasaltWidget.needsOf(listOf(BasaltWidget.DIGITAL, BasaltWidget.WORLD)),
        )
        assertEquals(emptySet<WidgetData>(), BasaltWidget.needsOf(emptyList()))
    }

    @Test
    fun `a home screen of clock faces never opens the timer table`() {
        val quiet = BasaltWidget.all.filter { it.tick == WidgetTick.Minute }
        val needs = BasaltWidget.needsOf(quiet)
        assertTrue(WidgetData.Timers !in needs)
        assertTrue(WidgetData.Stopwatch !in needs)
    }

    // -- providers --------------------------------------------------------

    @Test
    fun `every catalogue entry has a receiver and every receiver is distinct`() {
        assertEquals(BasaltWidget.all.size, WidgetReceivers.byWidget.size)
        BasaltWidget.all.forEach { widget ->
            assertTrue("${widget.id} has no receiver", widget in WidgetReceivers.byWidget)
        }
        assertEquals(
            BasaltWidget.all.size,
            WidgetReceivers.byWidget.values.map { it.name }.toSet().size,
        )
    }

    // -- text -------------------------------------------------------------

    @Test
    fun `display names are renderable in the base glyph set`() {
        // Widget labels reach the picker as system text, but the display name
        // is also what a face draws as a heading, and a character the 5x7 set
        // does not have silently becomes a question mark.
        val renderable = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 :.,-+/?!\"()<>=*#%&@_[];").toSet()
        BasaltWidget.all.forEach { widget ->
            widget.displayName.forEach {
                assertTrue("'$it' in ${widget.id} is not in the base glyph set", it in renderable)
            }
        }
    }

    @Test
    fun `descriptions are sentences, since the picker shows them verbatim`() {
        BasaltWidget.all.forEach { widget ->
            assertTrue("${widget.id} has no description", widget.description.length > 12)
            assertTrue("${widget.id} is not a sentence", widget.description.endsWith("."))
        }
    }
}
