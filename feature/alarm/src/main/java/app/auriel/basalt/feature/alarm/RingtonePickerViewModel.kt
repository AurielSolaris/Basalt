package app.auriel.basalt.feature.alarm

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.auriel.basalt.core.data.BasaltGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RingtonePickerUiState(
    val systemTones: List<RingtoneChoice> = emptyList(),
    val userFile: RingtoneChoice? = null,
    val selectedUri: String? = null,
    val durationMillis: Long = 0L,
    val startMillis: Long = 0L,
    /** Zero means "to the end"; [effectiveEndMillis] resolves it. */
    val endMillis: Long = 0L,
) {
    val effectiveEndMillis: Long
        get() = if (endMillis > 0L) endMillis else durationMillis
}

class RingtonePickerViewModel(
    private val context: Context,
    private val alarmId: Long,
) : ViewModel() {

    private val graph = BasaltGraph.get(context)
    private val _state = MutableStateFlow(RingtonePickerUiState())
    val state: StateFlow<RingtonePickerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val alarm = graph.alarms.get(alarmId)
            val tones = RingtoneCatalog.systemTones(context)
            val selected = alarm?.ringtoneUri
            val known = tones.any { it.uri == selected }
            _state.value = RingtonePickerUiState(
                systemTones = tones,
                // A URI that is not one of the device's tones must be a
                // file the user picked earlier; surface it so the choice is
                // visible rather than mysteriously absent.
                userFile = selected
                    ?.takeUnless { known }
                    ?.let {
                        RingtoneChoice(
                            uri = it,
                            title = RingtoneCatalog.displayName(context, Uri.parse(it)),
                            userSupplied = true,
                        )
                    },
                selectedUri = selected,
                durationMillis = selected?.let {
                    RingtoneCatalog.durationMillis(context, Uri.parse(it))
                } ?: 0L,
                startMillis = alarm?.ringtoneStartMillis ?: 0L,
                endMillis = alarm?.ringtoneEndMillis ?: 0L,
            )
        }
    }

    fun select(uri: String?) {
        viewModelScope.launch {
            val duration = uri?.let { RingtoneCatalog.durationMillis(context, Uri.parse(it)) } ?: 0L
            _state.update {
                // A trim measured against one file is meaningless against
                // another, so changing the sound clears it.
                it.copy(selectedUri = uri, durationMillis = duration, startMillis = 0L, endMillis = 0L)
            }
            persist()
        }
    }

    fun chooseFile(uri: Uri) {
        viewModelScope.launch {
            RingtoneCatalog.persist(context, uri)
            val choice = RingtoneChoice(
                uri = uri.toString(),
                title = RingtoneCatalog.displayName(context, uri),
                userSupplied = true,
            )
            _state.update {
                it.copy(
                    userFile = choice,
                    selectedUri = choice.uri,
                    durationMillis = RingtoneCatalog.durationMillis(context, uri),
                    startMillis = 0L,
                    endMillis = 0L,
                )
            }
            persist()
        }
    }

    fun adjustStart(deltaMillis: Long) {
        _state.update { current ->
            val ceiling = (current.effectiveEndMillis - MIN_SECTION_MILLIS).coerceAtLeast(0L)
            current.copy(startMillis = (current.startMillis + deltaMillis).coerceIn(0L, ceiling))
        }
        persistLater()
    }

    fun adjustEnd(deltaMillis: Long) {
        _state.update { current ->
            val floor = current.startMillis + MIN_SECTION_MILLIS
            val ceiling = current.durationMillis.coerceAtLeast(floor)
            current.copy(
                endMillis = (current.effectiveEndMillis + deltaMillis).coerceIn(floor, ceiling),
            )
        }
        persistLater()
    }

    fun clearTrim() {
        _state.update { it.copy(startMillis = 0L, endMillis = 0L) }
        persistLater()
    }

    private fun persistLater() {
        viewModelScope.launch { persist() }
    }

    private suspend fun persist() {
        val current = _state.value
        val alarm = graph.alarms.get(alarmId) ?: return
        graph.alarms.upsert(
            alarm.copy(
                ringtoneUri = current.selectedUri,
                ringtoneStartMillis = current.startMillis,
                // Persist zero when the end is the file's end, so a
                // re-encoded or replaced file still plays to completion
                // rather than stopping at a stale offset.
                ringtoneEndMillis = if (current.endMillis >= current.durationMillis) 0L
                else current.endMillis,
            ),
        )
    }

    private companion object {
        /** Nobody wants a 200ms alarm loop. */
        const val MIN_SECTION_MILLIS = 3_000L
    }
}
