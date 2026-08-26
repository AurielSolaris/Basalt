package app.auriel.basalt.core.design

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.Flow

/**
 * Installs the selected theme and makes the window agree with it.
 *
 * Three things have to move together when a palette changes, and only one
 * of them is Compose:
 *
 *  - the composition, via [BasaltTheme];
 *  - the **window background**, which the system paints before the first
 *    frame and again behind every configuration change. Leave it dark under
 *    Quartz and the app flashes black on every launch and rotation;
 *  - the **status and navigation bar icons**, which the OS draws and which
 *    have to be told which way round to go. Basalt draws edge to edge, so
 *    on a light ground those icons are white on white unless asked not to
 *    be.
 *
 * The theme id arrives as a plain string and a [Flow] of strings rather
 * than as a [BasaltThemeId], so that this module stays unaware of where
 * settings are stored. Resolution — including falling back on an id it does
 * not recognise — happens here.
 *
 * @param initialThemeId a synchronously-available id, used for the frames
 *   before [themeIdFlow] has emitted. Passing null is safe and costs one
 *   frame in the default palette.
 */
@Composable
fun BasaltThemeHost(
    themeIdFlow: Flow<String?>,
    initialThemeId: String? = null,
    content: @Composable () -> Unit,
) {
    val initial = remember(initialThemeId) {
        BasaltThemes.byId(initialThemeId).also(BasaltThemes::remember)
    }
    val themeId by themeIdFlow.collectAsState(initial = initial.id)
    val theme = BasaltThemes.byId(themeId)

    val view = LocalView.current
    val inspecting = LocalInspectionMode.current

    SideEffect {
        BasaltThemes.remember(theme)
        if (inspecting) return@SideEffect
        val window = view.context.findActivity()?.window ?: return@SideEffect
        window.setBackgroundDrawable(ColorDrawable(theme.colors.ink.toArgb()))
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = theme.isLight
            isAppearanceLightNavigationBars = theme.isLight
        }
    }

    BasaltTheme(theme = theme, content = content)
}

/**
 * Walks out to the hosting activity.
 *
 * A composable's context is usually the activity, but not always — it is a
 * `ContextWrapper` inside a dialog, and something else entirely inside a
 * preview — so the walk is the reliable form, and a null result means
 * "there is no window here", which is a fine thing for the caller to skip.
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
