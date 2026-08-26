package app.auriel.basalt.core.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Resolves the object graph from composition.
 *
 * Screens use this instead of taking repositories as parameters, so a
 * feature's public surface stays `@Composable fun XScreen()` and the app
 * module does not have to thread six dependencies through navigation.
 */
@Composable
fun rememberGraph(): BasaltGraph.Graph = BasaltGraph.get(LocalContext.current)
