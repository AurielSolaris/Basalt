package app.auriel.basalt.feature.bedtime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.auriel.basalt.core.data.rememberGraph
import app.auriel.basalt.core.design.BasaltGauge
import app.auriel.basalt.core.design.BasaltToggle
import app.auriel.basalt.core.design.InlineStepper
import app.auriel.basalt.core.design.LocalBasaltColors
import app.auriel.basalt.core.design.SectionHeader
import app.auriel.basalt.core.design.SettingRow
import app.auriel.basalt.core.design.WeekdayChips
import app.auriel.basalt.core.design.BasaltText
import app.auriel.basalt.core.dotmatrix.DotShape
import java.time.Duration
import java.time.LocalTime

/**
 * The sleep window.
 *
 * Drawn as an arc across the 24-hour dial rather than two times in a list,
 * because the thing worth seeing is the *shape* of the night: how long it
 * is, and how much of it is left.
 */
@Composable
fun BedtimeScreen(modifier: Modifier = Modifier) {
    val graph = rememberGraph()
    val viewModel: BedtimeViewModel = viewModel { BedtimeViewModel(graph) }
    val colors = LocalBasaltColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val schedule = state.schedule

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val startFraction = schedule.windDownAt.toSecondOfDay() / 86_400f
        val sweepFraction =
            ((schedule.durationMinutes + schedule.windDownMinutes) * 60f) / 86_400f

        BasaltGauge(
            start = startFraction,
            sweep = sweepFraction,
            needleAt = state.dayFraction,
            size = 230.dp,
            ticks = 24,
            arcColor = if (schedule.enabled) colors.patina else colors.pewter,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BasaltText(
                    text = "%dH %02dM".format(
                        schedule.durationMinutes / 60,
                        schedule.durationMinutes % 60,
                    ),
                    cellSize = 3f,
                    litColor = if (schedule.enabled) colors.copper else colors.pewter,
                    unlitColor = colors.unlit,
                    contentDescription = "Sleep window length",
                )
                BasaltText(
                    text = if (state.insideWindow) "WINDING DOWN" else "IN BED AT " + schedule.bedtime.hhmm(),
                    cellSize = 1.2f,
                    shape = DotShape.Chunky,
                    litColor = colors.pewter,
                    unlitColor = Color.Transparent,
                )
            }
        }

        state.untilWindDown?.let { until ->
            BasaltText(
                text = "WIND DOWN IN " + until.readable(),
                cellSize = 1.4f,
                shape = DotShape.Chunky,
                litColor = colors.copper,
                unlitColor = Color.Transparent,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        SectionHeader("SCHEDULE", modifier = Modifier.fillMaxWidth())

        SettingRow(label = "ENABLED") {
            BasaltToggle(
                checked = schedule.enabled,
                onCheckedChange = viewModel::setEnabled,
                contentDescription = "Bedtime schedule enabled",
            )
        }

        SettingRow(label = "BEDTIME") {
            InlineStepper(
                value = schedule.bedtime.hhmm(),
                onDown = { viewModel.adjustBedtime(-15) },
                onUp = { viewModel.adjustBedtime(15) },
                label = "bedtime",
            )
        }

        SettingRow(label = "WAKE") {
            InlineStepper(
                value = schedule.wakeTime.hhmm(),
                onDown = { viewModel.adjustWake(-15) },
                onUp = { viewModel.adjustWake(15) },
                label = "wake time",
            )
        }

        SettingRow(label = "WIND DOWN", caption = "MINUTES BEFORE BED") {
            InlineStepper(
                value = schedule.windDownMinutes.toString(),
                onDown = { viewModel.adjustWindDown(-5) },
                onUp = { viewModel.adjustWindDown(5) },
                label = "wind down",
            )
        }

        SectionHeader("DAYS", modifier = Modifier.fillMaxWidth())

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            WeekdayChips(
                isOn = { viewModel.isOn(schedule, it) },
                onToggle = viewModel::toggleDay,
            )
        }
    }
}

private fun LocalTime.hhmm(): String = "%02d:%02d".format(hour, minute)

private fun Duration.readable(): String {
    val hours = toHours()
    val minutes = toMinutes() % 60
    return if (hours > 0) "${hours}H ${minutes}M" else "${minutes}M"
}
