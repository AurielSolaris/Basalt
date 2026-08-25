package app.auriel.basalt.feature.clock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
 * v0.1.0 shows the local time and date only — no world-clock board yet.
 * It exists this early because it is the cheapest end-to-end check that the
 * glyph set, the palette and the tick loop agree with each other.
 */
@Composable
fun ClockScreen(
    modifier: Modifier = Modifier,
    timeSource: TimeSource = remember { SystemTimeSource() },
    use24Hour: Boolean = true,
) {
    val colors = LocalBasaltColors.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    // Aligns to the top of each minute rather than polling on a fixed
    // period, so the readout never lags a tick behind the real clock.
    LaunchedEffect(timeSource) {
        while (true) {
            val instant = timeSource.now()
            now = LocalDateTime.ofInstant(instant, timeSource.zone())
            val millisIntoMinute = (instant.epochSecond % 60) * 1000 + instant.nano / 1_000_000
            delay(60_000L - millisIntoMinute)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DotMatrixText(
            text = ClockFormat.time(now.toLocalTime(), use24Hour),
            cellSize = 9f,
            litColor = colors.copper,
            unlitColor = colors.unlit,
        )

        val meridiem = ClockFormat.meridiem(now.toLocalTime(), use24Hour)
        if (meridiem.isNotEmpty()) {
            DotMatrixText(
                text = meridiem,
                cellSize = 3f,
                litColor = colors.copperHot,
                unlitColor = colors.unlit,
            )
        }

        DotMatrixText(
            text = ClockFormat.date(now.toLocalDate()),
            cellSize = 3f,
            litColor = colors.silver,
            unlitColor = colors.unlit,
        )
    }
}
