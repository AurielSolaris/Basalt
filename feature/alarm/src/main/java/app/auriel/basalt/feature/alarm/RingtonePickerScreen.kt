package app.auriel.basalt.feature.alarm

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.auriel.basalt.core.design.BasaltButton
import app.auriel.basalt.core.design.BezelPanel
import app.auriel.basalt.core.design.InlineStepper
import app.auriel.basalt.core.design.LocalBasaltColors
import app.auriel.basalt.core.design.SectionHeader
import app.auriel.basalt.core.design.SettingRow
import app.auriel.basalt.core.design.BasaltText
import app.auriel.basalt.core.dotmatrix.DotShape

/**
 * Choose the sound, and choose which part of it plays.
 *
 * Most clock apps take a file and play it from the beginning, fading in.
 * That is fine for a stock tone and useless for a song: the part worth
 * waking up to is almost never the first thirty seconds. Here the user sets
 * the start and end themselves and hears the result before committing.
 */
@Composable
fun RingtonePickerScreen(
    alarmId: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val viewModel: RingtonePickerViewModel =
        viewModel { RingtonePickerViewModel(context, alarmId) }
    val colors = LocalBasaltColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val preview = remember { RingtonePreview(context, scope) }

    DisposableEffect(Unit) { onDispose { preview.stop() } }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> uri?.let(viewModel::chooseFile) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasaltText(
                text = "ALARM SOUND",
                cellSize = 1.9f,
                shape = DotShape.Chunky,
                litColor = colors.silver,
                unlitColor = Color.Transparent,
            )
            BasaltButton(
                label = "DONE",
                emphasised = true,
                cellSize = 1.4f,
                onClick = {
                    preview.stop()
                    onDone()
                },
            )
        }

        if (state.selectedUri != null && state.durationMillis > 0L) {
            TrimPanel(
                state = state,
                onAdjustStart = viewModel::adjustStart,
                onAdjustEnd = viewModel::adjustEnd,
                onClearTrim = viewModel::clearTrim,
                onPreview = {
                    if (preview.isPlaying) {
                        preview.stop()
                    } else {
                        state.selectedUri?.let {
                            preview.play(Uri.parse(it), state.startMillis, state.endMillis)
                        }
                    }
                },
            )
        }

        SectionHeader("YOUR FILES")

        SettingRow(
            label = "CHOOSE MP3 OR WAV",
            caption = "FROM ANY FOLDER OR APP",
        ) {
            BasaltButton(
                label = "BROWSE",
                cellSize = 1.4f,
                onClick = { openDocument.launch(RingtoneCatalog.AudioMimeTypes) },
            )
        }

        state.userFile?.let { file ->
            ToneRow(
                title = file.title,
                selected = state.selectedUri == file.uri,
                onClick = {
                    preview.stop()
                    viewModel.select(file.uri)
                },
            )
        }

        SectionHeader("ON THIS DEVICE")

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            items(state.systemTones, key = { it.uri ?: "default" }) { tone ->
                ToneRow(
                    title = tone.title,
                    selected = state.selectedUri == tone.uri,
                    onClick = {
                        preview.stop()
                        viewModel.select(tone.uri)
                    },
                )
            }
        }
    }
}

@Composable
private fun TrimPanel(
    state: RingtonePickerUiState,
    onAdjustStart: (Long) -> Unit,
    onAdjustEnd: (Long) -> Unit,
    onClearTrim: () -> Unit,
    onPreview: () -> Unit,
) {
    val colors = LocalBasaltColors.current
    BezelPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BasaltText(
                text = "TRIM",
                cellSize = 1.5f,
                shape = DotShape.Chunky,
                litColor = colors.bronze,
                unlitColor = Color.Transparent,
            )

            // A plain bar rather than a waveform: drawing a real waveform
            // means decoding the whole file, and the position of the
            // selection is the part that actually needs to be visible.
            val total = state.durationMillis.coerceAtLeast(1L).toFloat()
            val startFraction = (state.startMillis / total).coerceIn(0f, 1f)
            val endFraction = (state.effectiveEndMillis / total).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(colors.ironOxide),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(endFraction - startFraction)
                        .height(12.dp)
                        .padding(start = 0.dp)
                        .background(colors.copper)
                        .align(Alignment.CenterStart),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BasaltText(
                    text = state.startMillis.asClock(),
                    cellSize = 1.3f,
                    shape = DotShape.Chunky,
                    litColor = colors.copper,
                    unlitColor = Color.Transparent,
                )
                BasaltText(
                    text = state.durationMillis.asClock(),
                    cellSize = 1.3f,
                    shape = DotShape.Chunky,
                    litColor = colors.pewter,
                    unlitColor = Color.Transparent,
                )
            }

            SettingRow(label = "START", caption = state.startMillis.asClock()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InlineStepper(
                        value = "1S",
                        onDown = { onAdjustStart(-1000L) },
                        onUp = { onAdjustStart(1000L) },
                        label = "start by one second",
                    )
                    InlineStepper(
                        value = "10S",
                        onDown = { onAdjustStart(-10_000L) },
                        onUp = { onAdjustStart(10_000L) },
                        label = "start by ten seconds",
                    )
                }
            }

            SettingRow(label = "END", caption = state.effectiveEndMillis.asClock()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InlineStepper(
                        value = "1S",
                        onDown = { onAdjustEnd(-1000L) },
                        onUp = { onAdjustEnd(1000L) },
                        label = "end by one second",
                    )
                    InlineStepper(
                        value = "10S",
                        onDown = { onAdjustEnd(-10_000L) },
                        onUp = { onAdjustEnd(10_000L) },
                        label = "end by ten seconds",
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasaltButton(
                    label = "AUDITION",
                    emphasised = true,
                    cellSize = 1.4f,
                    onClick = onPreview,
                )
                BasaltButton(label = "FULL LENGTH", cellSize = 1.4f, onClick = onClearTrim)
            }
        }
    }
}

@Composable
private fun ToneRow(title: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalBasaltColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(if (selected) colors.copper else colors.ironOxide),
        )
        BasaltText(
            text = title,
            cellSize = 1.5f,
            shape = DotShape.Chunky,
            litColor = if (selected) colors.copperHot else colors.silver,
            unlitColor = Color.Transparent,
        )
    }
}

/** `M:SS`, which is how anyone thinks about a position in a song. */
private fun Long.asClock(): String {
    val total = this / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
