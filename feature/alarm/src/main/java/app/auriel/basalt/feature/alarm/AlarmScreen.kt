package app.auriel.basalt.feature.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.auriel.basalt.core.data.model.Alarm
import app.auriel.basalt.core.design.BasaltButton
import app.auriel.basalt.core.design.BasaltStepper
import app.auriel.basalt.core.design.BasaltToggle
import app.auriel.basalt.core.design.BezelPanel
import app.auriel.basalt.core.design.LocalBasaltColors
import app.auriel.basalt.core.dotmatrix.DotMatrixText
import app.auriel.basalt.core.dotmatrix.DotShape
import java.time.Duration

/**
 * The alarm list.
 *
 * The alarms are real and persist for the life of the process, but nothing
 * rings yet: scheduling, the instance state machine and the firing activity
 * are a later pass. The screen says so rather than implying otherwise.
 */
@Composable
fun AlarmScreen(
    modifier: Modifier = Modifier,
    onEditSound: (Long) -> Unit = {},
) {
    val context = LocalContext.current.applicationContext
    val viewModel: AlarmViewModel = viewModel { AlarmViewModel(context) }
    val colors = LocalBasaltColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BezelPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StepperField(
                        label = "HRS",
                        value = state.draft.hour,
                        onUp = { viewModel.adjustDraft(hours = 1) },
                        onDown = { viewModel.adjustDraft(hours = -1) },
                    )
                    DotMatrixText(
                        text = ":",
                        cellSize = 3.4f,
                        litColor = colors.bronze,
                        unlitColor = Color.Transparent,
                    )
                    StepperField(
                        label = "MIN",
                        value = state.draft.minute,
                        onUp = { viewModel.adjustDraft(minutes = 5) },
                        onDown = { viewModel.adjustDraft(minutes = -5) },
                    )
                }
                BasaltButton(
                    label = "ADD ALARM",
                    emphasised = true,
                    onClick = viewModel::addDraftAlarm,
                )
            }
        }

        state.nextAlarmIn?.let { untilNext ->
            DotMatrixText(
                text = "NEXT IN " + untilNext.readable(),
                cellSize = 1.5f,
                shape = DotShape.Chunky,
                litColor = colors.copper,
                unlitColor = Color.Transparent,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        if (state.alarms.isEmpty()) {
            DotMatrixText(
                text = "NO ALARMS",
                cellSize = 2f,
                shape = DotShape.Chunky,
                litColor = colors.pewter,
                unlitColor = Color.Transparent,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggleEnabled = { viewModel.setEnabled(alarm, it) },
                        onToggleDay = { viewModel.toggleDay(alarm, it) },
                        onToggleSkip = { viewModel.setSkipNext(alarm, it) },
                        onEditSound = { onEditSound(alarm.id) },
                        onRemove = { viewModel.remove(alarm) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperField(
    label: String,
    value: Int,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    val colors = LocalBasaltColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DotMatrixText(
            text = label,
            cellSize = 1.3f,
            shape = DotShape.Chunky,
            litColor = colors.pewter,
            unlitColor = Color.Transparent,
        )
        DotMatrixText(
            text = value.toString().padStart(2, '0'),
            cellSize = 3.4f,
            litColor = colors.copper,
            unlitColor = colors.unlit,
            contentDescription = "$label $value",
        )
        BasaltStepper(onUp = onUp, onDown = onDown, label = label)
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleDay: (java.time.DayOfWeek) -> Unit,
    onToggleSkip: (Boolean) -> Unit,
    onEditSound: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalBasaltColors.current
    BezelPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DotMatrixText(
                        text = "%02d:%02d".format(alarm.time.hour, alarm.time.minute),
                        cellSize = 4f,
                        // A disabled alarm is dimmed, not hidden: it is
                        // still a thing the user set up.
                        litColor = if (alarm.enabled) colors.copper else colors.pewter,
                        unlitColor = colors.unlit,
                    )
                    DotMatrixText(
                        text = alarm.repeatDays.summary(),
                        cellSize = 1.3f,
                        shape = DotShape.Chunky,
                        litColor = colors.pewter,
                        unlitColor = Color.Transparent,
                    )
                }
                BasaltToggle(
                    checked = alarm.enabled,
                    onCheckedChange = onToggleEnabled,
                    contentDescription = "Alarm enabled",
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WeekdayInitials.forEach { (day, initial) ->
                    val on = day in alarm.repeatDays
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(if (on) colors.bronze else colors.ironOxide)
                            .clickable { onToggleDay(day) },
                        contentAlignment = Alignment.Center,
                    ) {
                        DotMatrixText(
                            text = initial,
                            cellSize = 1.8f,
                            shape = DotShape.Chunky,
                            litColor = if (on) colors.copperHot else colors.pewter,
                            unlitColor = Color.Transparent,
                            contentDescription = day.name,
                        )
                    }
                }
            }

            // Skipping is only meaningful for a repeating alarm: a one-shot
            // that you do not want is a one-shot you delete.
            if (alarm.repeatDays.isRepeating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DotMatrixText(
                            text = "SKIP NEXT",
                            cellSize = 1.4f,
                            shape = DotShape.Chunky,
                            litColor = colors.silver,
                            unlitColor = Color.Transparent,
                        )
                        DotMatrixText(
                            text = if (alarm.skipNext) "SKIPPING ONE" else "SCHEDULE INTACT",
                            cellSize = 1.1f,
                            shape = DotShape.Chunky,
                            litColor = colors.pewter,
                            unlitColor = Color.Transparent,
                        )
                    }
                    BasaltToggle(
                        checked = alarm.skipNext,
                        onCheckedChange = onToggleSkip,
                        contentDescription = "Skip the next occurrence",
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DotMatrixText(
                        text = "SOUND",
                        cellSize = 1.4f,
                        shape = DotShape.Chunky,
                        litColor = colors.silver,
                        unlitColor = Color.Transparent,
                    )
                    DotMatrixText(
                        text = alarm.soundSummary(),
                        cellSize = 1.1f,
                        shape = DotShape.Chunky,
                        litColor = colors.pewter,
                        unlitColor = Color.Transparent,
                    )
                }
                BasaltButton(label = "CHANGE", onClick = onEditSound, cellSize = 1.4f)
                BasaltButton(label = "DELETE", onClick = onRemove, cellSize = 1.4f)
            }
        }
    }
}

/** `2H 15M`, or `45M` under the hour. Long enough to be useful, no longer. */
private fun Duration.readable(): String {
    val hours = toHours()
    val minutes = toMinutes() % 60
    return if (hours > 0) "${hours}H ${minutes}M" else "${minutes}M"
}

/**
 * What the alarm will play, in a few characters.
 *
 * The trimmed case names its window: "TRIM 1:12" tells the user the choice
 * survived, which a bare "custom" would not.
 */
private fun Alarm.soundSummary(): String = when {
    hasTrim -> "TRIM FROM " + (ringtoneStartMillis / 1000).let { "%d:%02d".format(it / 60, it % 60) }
    ringtoneUri != null -> "CUSTOM SOUND"
    else -> "DEFAULT ALARM"
}
