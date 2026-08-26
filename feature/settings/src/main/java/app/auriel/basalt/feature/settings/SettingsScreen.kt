package app.auriel.basalt.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.auriel.basalt.core.data.rememberGraph
import app.auriel.basalt.core.design.BasaltButton
import app.auriel.basalt.core.design.BasaltToggle
import app.auriel.basalt.core.design.InlineStepper
import app.auriel.basalt.core.design.LocalBasaltColors
import app.auriel.basalt.core.design.SectionHeader
import app.auriel.basalt.core.design.SettingRow
import app.auriel.basalt.core.design.ThemePicker
import app.auriel.basalt.core.dotmatrix.DotMatrixText
import app.auriel.basalt.core.dotmatrix.DotShape

/**
 * Settings, including the part that decides whether alarms work at all.
 *
 * The SYSTEM section is not filler. On a modern Android device the gap
 * between "the alarm is scheduled" and "the alarm rings" is entirely made
 * of these three permissions, and the app cannot grant itself any of them.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val graph = rememberGraph()
    val viewModel: SettingsViewModel = viewModel { SettingsViewModel(graph) }
    val colors = LocalBasaltColors.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // These are granted in the *system* settings app, so the only reliable
    // moment to re-read them is when Basalt comes back to the foreground.
    var access by remember { mutableStateOf(readSystemAccess(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) access = readSystemAccess(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SectionHeader("THEME")

        ThemePicker(
            selected = theme,
            onSelect = viewModel::setTheme,
            modifier = Modifier.padding(vertical = 4.dp),
        )

        SectionHeader("DISPLAY")

        SettingRow(label = "24 HOUR CLOCK") {
            BasaltToggle(
                checked = settings.use24Hour,
                onCheckedChange = viewModel::set24Hour,
                contentDescription = "24 hour clock",
            )
        }
        SettingRow(label = "SHOW SECONDS", caption = "REDRAWS EVERY SECOND") {
            BasaltToggle(
                checked = settings.showSeconds,
                onCheckedChange = viewModel::setShowSeconds,
                contentDescription = "Show seconds",
            )
        }
        SettingRow(label = "WEEK STARTS") {
            InlineStepper(
                value = SettingsViewModel.weekStartLabel(settings.weekStart),
                onDown = { viewModel.cycleWeekStart(false) },
                onUp = { viewModel.cycleWeekStart(true) },
                label = "week start",
            )
        }

        SectionHeader("ALARM")

        SettingRow(label = "SNOOZE", caption = "MINUTES") {
            InlineStepper(
                value = settings.snoozeMinutes.toString(),
                onDown = { viewModel.adjustSnooze(-1) },
                onUp = { viewModel.adjustSnooze(1) },
                label = "snooze length",
            )
        }
        SettingRow(
            label = "SILENCE AFTER",
            caption = if (settings.silenceAfterMinutes == 0) "NEVER GIVE UP" else "MINUTES",
        ) {
            InlineStepper(
                value = if (settings.silenceAfterMinutes == 0) "OFF"
                else settings.silenceAfterMinutes.toString(),
                onDown = { viewModel.adjustSilenceAfter(-1) },
                onUp = { viewModel.adjustSilenceAfter(1) },
                label = "silence after",
            )
        }
        SettingRow(label = "VOLUME RAMP", caption = "START QUIET, GET LOUDER") {
            BasaltToggle(
                checked = settings.volumeCrescendo,
                onCheckedChange = viewModel::setVolumeCrescendo,
                contentDescription = "Volume ramp",
            )
        }
        SettingRow(label = "VIBRATE") {
            BasaltToggle(
                checked = settings.vibrateByDefault,
                onCheckedChange = viewModel::setVibrate,
                contentDescription = "Vibrate by default",
            )
        }

        SectionHeader("EXTRAS")

        SettingRow(label = "WEATHER", caption = "OFF BY DEFAULT. USES NETWORK") {
            BasaltToggle(
                checked = settings.weatherEnabled,
                onCheckedChange = viewModel::setWeather,
                contentDescription = "Weather",
            )
        }

        SectionHeader("SYSTEM")

        if (!access.allClear) {
            DotMatrixText(
                text = "ALARMS MAY NOT RING",
                cellSize = 1.5f,
                shape = DotShape.Chunky,
                litColor = colors.emberAlarm,
                unlitColor = Color.Transparent,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        AccessRow(
            label = "NOTIFICATIONS",
            caption = "REQUIRED TO SHOW A RINGING ALARM",
            granted = access.notificationsEnabled,
            onFix = { openNotificationSettings(context) },
        )
        AccessRow(
            label = "EXACT ALARMS",
            caption = "REQUIRED TO RING ON TIME",
            granted = access.canScheduleExactAlarms,
            onFix = { requestExactAlarmPermission(context) },
        )
        AccessRow(
            label = "BATTERY",
            caption = "UNRESTRICTED, SO ANDROID CANNOT SLEEP US",
            granted = access.ignoringBatteryOptimizations,
            onFix = { requestIgnoreBatteryOptimizations(context) },
        )

        SettingRow(
            label = "APP INFO",
            caption = "OEM SLEEP AND AUTOSTART SETTINGS LIVE HERE",
        ) {
            BasaltButton(label = "OPEN", onClick = { openAppSettings(context) }, cellSize = 1.4f)
        }

        DotMatrixText(
            text = "SOME MAKERS ADD THEIR OWN SLEEP RULES ON TOP OF",
            cellSize = 1.1f,
            shape = DotShape.Chunky,
            litColor = colors.pewter,
            unlitColor = Color.Transparent,
            modifier = Modifier.padding(top = 10.dp),
        )
        DotMatrixText(
            text = "ANDROIDS. BASALT CANNOT TURN THOSE OFF ITSELF.",
            cellSize = 1.1f,
            shape = DotShape.Chunky,
            litColor = colors.pewter,
            unlitColor = Color.Transparent,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )

        SectionHeader("ABOUT")

        SettingRow(label = "BASALT", caption = "VERSION " + versionName(context)) {}

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {}
    }
}

@Composable
private fun AccessRow(
    label: String,
    caption: String,
    granted: Boolean,
    onFix: () -> Unit,
) {
    val colors = LocalBasaltColors.current
    SettingRow(label = label, caption = caption) {
        if (granted) {
            DotMatrixText(
                text = "OK",
                cellSize = 1.8f,
                shape = DotShape.Chunky,
                litColor = colors.patina,
                unlitColor = Color.Transparent,
                contentDescription = "$label granted",
            )
        } else {
            BasaltButton(
                label = "GRANT",
                emphasised = true,
                onClick = onFix,
                cellSize = 1.4f,
            )
        }
    }
}

private fun versionName(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
}.getOrDefault("?")
