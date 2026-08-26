package app.auriel.basalt.feature.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import app.auriel.basalt.core.data.model.TimerState
import app.auriel.basalt.core.design.BasaltButton
import app.auriel.basalt.core.design.BasaltStepper
import app.auriel.basalt.core.design.BezelPanel
import app.auriel.basalt.core.design.LocalBasaltColors
import app.auriel.basalt.core.dotmatrix.DotMatrixText
import app.auriel.basalt.core.dotmatrix.DotShape
import app.auriel.basalt.core.time.DurationFormat

/**
 * The timer bank.
 *
 * A setter plate at the top dials in a duration; below it, every timer that
 * exists, each with its own controls. Timers run past zero and count up
 * negatively, as AOSP's do, so "how long ago did that go off" is answerable.
 */
@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val viewModel: TimerViewModel = viewModel { TimerViewModel(context) }
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
                DurationSetter(
                    seconds = state.draftSeconds,
                    onAdjust = viewModel::adjustDraft,
                )
                BasaltButton(
                    label = "ADD TIMER",
                    emphasised = true,
                    onClick = viewModel::addDraftTimer,
                )
            }
        }

        if (state.rows.isEmpty()) {
            DotMatrixText(
                text = "NO TIMERS",
                cellSize = 2f,
                shape = DotShape.Chunky,
                litColor = colors.pewter,
                unlitColor = Color.Transparent,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.rows, key = { it.timer.id }) { row ->
                    TimerCard(
                        row = row,
                        onStart = { viewModel.start(row.timer) },
                        onPause = { viewModel.pause(row.timer) },
                        onReset = { viewModel.reset(row.timer) },
                        onAddMinute = { viewModel.addMinute(row.timer) },
                        onRemove = { viewModel.remove(row.timer) },
                    )
                }
            }
        }
    }
}

/** Hours / minutes / seconds, each with its own stepper. */
@Composable
private fun DurationSetter(
    seconds: Int,
    onAdjust: (Int) -> Unit,
) {
    val colors = LocalBasaltColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            Triple("HRS", seconds / 3600, 3600),
            Triple("MIN", (seconds % 3600) / 60, 60),
            Triple("SEC", seconds % 60, 1),
        ).forEachIndexed { index, (label, value, step) ->
            if (index > 0) {
                DotMatrixText(
                    text = ":",
                    cellSize = 3.4f,
                    litColor = colors.bronze,
                    unlitColor = Color.Transparent,
                )
            }
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
                BasaltStepper(
                    onUp = { onAdjust(step) },
                    onDown = { onAdjust(-step) },
                    label = label,
                )
            }
        }
    }
}

@Composable
private fun TimerCard(
    row: TimerRow,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onAddMinute: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalBasaltColors.current
    // Copper while it is counting down, ember once it has overrun: the
    // readout itself carries the state, so no separate status line is
    // needed.
    val readoutColor = when {
        row.expired -> colors.emberAlarm
        row.running -> colors.copper
        else -> colors.silver
    }

    BezelPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DotMatrixText(
                text = DurationFormat.coarse(row.remainingMillis),
                cellSize = 5f,
                litColor = readoutColor,
                unlitColor = colors.unlit,
            )
            DotMatrixText(
                text = "OF " + DurationFormat.coarse(row.timer.totalMillis),
                cellSize = 1.3f,
                shape = DotShape.Chunky,
                litColor = colors.pewter,
                unlitColor = Color.Transparent,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (row.running) {
                    BasaltButton(label = "PAUSE", onClick = onPause, cellSize = 1.5f)
                } else {
                    BasaltButton(
                        label = if (row.timer.state == TimerState.Paused) "RESUME" else "START",
                        emphasised = true,
                        onClick = onStart,
                        cellSize = 1.5f,
                    )
                }
                BasaltButton(label = "+1 MIN", onClick = onAddMinute, cellSize = 1.5f)
                BasaltButton(label = "RESET", onClick = onReset, cellSize = 1.5f)
                BasaltButton(label = "DEL", onClick = onRemove, cellSize = 1.5f)
            }
        }
    }
}
