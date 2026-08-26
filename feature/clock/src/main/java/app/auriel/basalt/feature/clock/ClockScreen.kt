package app.auriel.basalt.feature.clock

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.auriel.basalt.core.data.model.Settings
import app.auriel.basalt.core.data.rememberGraph
import app.auriel.basalt.core.design.LocalBasaltColors
import app.auriel.basalt.core.dotmatrix.DotMatrixText
import app.auriel.basalt.core.time.ClockFormat
import app.auriel.basalt.core.time.SystemTimeSource
import app.auriel.basalt.core.time.TimeSource
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/**
 * The main readout.
 *
 * Local time and date only — no world-clock board yet. It exists this early
 * because it is the cheapest end-to-end check that the glyph set, the
 * palette and the tick loop agree with each other.
 *
 * Seconds and the meridiem share a small column beside the hours and
 * minutes rather than sitting inline with them. At the readout's cell size
 * an inline `HH:MM:SS` is forty-seven cells wide, which does not fit on a
 * phone; and putting the fast-moving field in its own column is what an
 * instrument does anyway, because it stops the whole readout reflowing
 * every second.
 */
@Composable
fun ClockScreen(
    modifier: Modifier = Modifier,
    timeSource: TimeSource = remember { SystemTimeSource() },
) {
    val colors = LocalBasaltColors.current
    val graph = rememberGraph()
    val settings by graph.settings.settings.collectAsStateWithLifecycle(
        initialValue = Settings(),
    )
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    // Aligned to the top of the field being displayed rather than polled on
    // a fixed period, so the readout never lags a tick behind the real
    // clock. With seconds off that is one wake-up a minute; with them on it
    // is one a second, which is what the setting's caption warns about.
    LaunchedEffect(timeSource, settings.showSeconds) {
        while (true) {
            val instant = timeSource.now()
            now = LocalDateTime.ofInstant(instant, timeSource.zone())
            val millisIntoSecond = instant.nano / 1_000_000L
            delay(
                if (settings.showSeconds) {
                    1_000L - millisIntoSecond
                } else {
                    60_000L - ((instant.epochSecond % 60) * 1_000L + millisIntoSecond)
                },
            )
        }
    }

    val time = now.toLocalTime()
    val meridiem = ClockFormat.meridiem(time, settings.use24Hour)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            DotMatrixText(
                text = ClockFormat.time(time, settings.use24Hour),
                cellSize = 9f,
                litColor = colors.copper,
                unlitColor = colors.unlit,
            )

            if (settings.showSeconds || meridiem.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (settings.showSeconds) {
                        DotMatrixText(
                            text = ClockFormat.seconds(time),
                            cellSize = 3f,
                            litColor = colors.copperHot,
                            unlitColor = colors.unlit,
                            contentDescription = "${time.second} seconds",
                        )
                    }
                    if (meridiem.isNotEmpty()) {
                        DotMatrixText(
                            text = meridiem,
                            cellSize = 3f,
                            litColor = colors.silver,
                            unlitColor = colors.unlit,
                        )
                    }
                }
            }
        }

        DotMatrixText(
            text = ClockFormat.date(now.toLocalDate()),
            cellSize = 3f,
            litColor = colors.silver,
            unlitColor = colors.unlit,
        )
    }
}
