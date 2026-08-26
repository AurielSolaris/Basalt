package app.auriel.basalt

import android.app.Application
import app.auriel.basalt.core.data.BasaltGraph

/**
 * Warms the object graph and hands scheduling a chance to catch up.
 *
 * The graph is lazy, so this costs nothing but a few object allocations;
 * what it buys is a single, obvious place where process start is handled,
 * rather than each entry point discovering the graph for itself.
 */
class BasaltApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BasaltGraph.get(this)
    }
}
