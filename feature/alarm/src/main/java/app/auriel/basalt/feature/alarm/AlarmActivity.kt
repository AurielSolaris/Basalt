package app.auriel.basalt.feature.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.auriel.basalt.core.data.BasaltGraph
import app.auriel.basalt.core.design.BasaltButton
import app.auriel.basalt.core.design.BasaltThemeHost
import app.auriel.basalt.core.design.LocalBasaltColors
import app.auriel.basalt.core.dotmatrix.DotMatrixText
import app.auriel.basalt.core.dotmatrix.DotShape

/**
 * The screen an alarm brings up.
 *
 * Everything here is about being reachable from a phone that is asleep,
 * face down and locked: the activity shows over the keyguard, turns the
 * screen on itself, and keeps it on while it is in front. The two actions
 * are deliberately large and far apart — this is the one screen in Android
 * that gets used by someone who is not yet awake.
 */
class AlarmActivity : ComponentActivity() {

    private var instanceId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instanceId = intent.getLongExtra(EXTRA_INSTANCE_ID, -1L)
        showOverLockscreen()

        val graph = BasaltGraph.get(this)
        setContent {
            // The alarm screen is put in front of someone who is asleep, so
            // it starts from the synchronously-cached theme id rather than
            // spending a frame in the default palette.
            BasaltThemeHost(
                themeIdFlow = graph.themeIds,
                initialThemeId = graph.cachedThemeId,
            ) {
                AlarmScreenContent(
                    instanceId = instanceId,
                    onSnooze = {
                        sendBroadcast(AlarmStateManager.snoozeIntent(this, instanceId))
                        finish()
                    },
                    onDismiss = {
                        sendBroadcast(AlarmStateManager.dismissIntent(this, instanceId))
                        finish()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        instanceId = intent.getLongExtra(EXTRA_INSTANCE_ID, instanceId)
    }

    private fun showOverLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // Asking the keyguard to step aside is a request, not a
            // command: on a secured lockscreen the system shows the alarm
            // above it instead, which is the correct behaviour anyway.
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        private const val EXTRA_INSTANCE_ID = "instance_id"

        fun intent(context: Context, instanceId: Long): Intent =
            Intent(context, AlarmActivity::class.java).apply {
                putExtra(EXTRA_INSTANCE_ID, instanceId)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION,
                )
            }
    }
}

@Composable
private fun AlarmScreenContent(
    instanceId: Long,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalBasaltColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var label by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }

    LaunchedEffect(instanceId) {
        val graph = BasaltGraph.get(context)
        val instance = graph.alarmInstances.get(instanceId)
        val alarm = instance?.let { graph.alarms.get(it.alarmId) }
        time = instance?.firesAt?.let { "%02d:%02d".format(it.hour, it.minute) } ?: ""
        label = alarm?.label?.takeIf(String::isNotBlank) ?: "ALARM"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ink)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DotMatrixText(
            text = label.uppercase(),
            cellSize = 2f,
            shape = DotShape.Chunky,
            litColor = colors.silver,
            unlitColor = Color.Transparent,
        )
        DotMatrixText(
            text = time,
            cellSize = 9f,
            litColor = colors.emberAlarm,
            unlitColor = colors.unlit,
            modifier = Modifier.padding(vertical = 32.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasaltButton(label = "SNOOZE", onClick = onSnooze, cellSize = 2.4f)
            BasaltButton(label = "DISMISS", emphasised = true, onClick = onDismiss, cellSize = 2.4f)
        }
    }
}
