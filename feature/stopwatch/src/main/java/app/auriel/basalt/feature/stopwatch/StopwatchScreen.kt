package app.auriel.basalt.feature.stopwatch

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
import app.auriel.basalt.core.data.model.Lap
import app.auriel.basalt.core.design.BasaltButton
import app.auriel.basalt.core.design.BasaltGauge
import app.auriel.basalt.core.design.LocalBasaltColors
import app.auriel.basalt.core.design.BasaltText
import app.auriel.basalt.core.dotmatrix.DotShape
import app.auriel.basalt.core.time.DurationFormat

/**
 * The stopwatch.
 *
 * A brass gauge sweeping the current minute, the elapsed time in the middle
 * of it, and the laps below — newest first, because the lap you just took is
 * the one you want to read.
 */
@Composable
fun StopwatchScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val viewModel: StopwatchViewModel = viewModel { StopwatchViewModel(context) }
    val colors = LocalBasaltColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val secondsIntoMinute = (state.elapsedMillis % 60_000L) / 60_000f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasaltGauge(
            start = 0f,
            sweep = secondsIntoMinute,
            needleAt = secondsIntoMinute,
            size = 240.dp,
            ticks = 12,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BasaltText(
                    text = DurationFormat.precise(state.elapsedMillis),
                    cellSize = 3f,
                    litColor = if (state.running) colors.copper else colors.silver,
                    unlitColor = colors.unlit,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasaltButton(
                label = if (state.running) "PAUSE" else if (state.started) "RESUME" else "START",
                emphasised = !state.running,
                onClick = viewModel::startOrPause,
            )
            BasaltButton(
                label = "LAP",
                enabled = state.running,
                onClick = viewModel::lap,
            )
            BasaltButton(
                label = "RESET",
                enabled = state.started,
                onClick = viewModel::reset,
            )
        }

        if (state.laps.isEmpty()) {
            BasaltText(
                text = "NO LAPS",
                cellSize = 1.6f,
                shape = DotShape.Chunky,
                litColor = colors.pewter,
                unlitColor = Color.Transparent,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(state.laps, key = Lap::number) { lap -> LapRow(lap) }
            }
        }
    }
}

@Composable
private fun LapRow(lap: Lap) {
    val colors = LocalBasaltColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasaltText(
            text = lap.number.toString().padStart(2, '0'),
            cellSize = 1.6f,
            shape = DotShape.Chunky,
            litColor = colors.pewter,
            unlitColor = Color.Transparent,
            contentDescription = "Lap ${lap.number}",
        )
        // The split is what a lap is for, so it gets the copper.
        BasaltText(
            text = DurationFormat.precise(lap.lapMillis),
            cellSize = 1.9f,
            shape = DotShape.Chunky,
            litColor = colors.copper,
            unlitColor = Color.Transparent,
        )
        BasaltText(
            text = DurationFormat.precise(lap.totalMillis),
            cellSize = 1.6f,
            shape = DotShape.Chunky,
            litColor = colors.silver,
            unlitColor = Color.Transparent,
        )
    }
}
