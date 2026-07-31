package com.tempo.rn.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tempo.rn.core.config.ForecastConfig
import com.tempo.rn.data.RadarFrame
import com.tempo.rn.data.RadarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val frames: List<RadarFrame> = emptyList(),
    val selectedIndex: Int = 0,
    val playing: Boolean = false,
    val loading: Boolean = true,
    val error: Boolean = false,
) {
    val selectedFrame: RadarFrame? get() = frames.getOrNull(selectedIndex)
}

@HiltViewModel
class MapViewModel @Inject constructor(
    private val radarRepository: RadarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    init {
        refresh()
        // Automatische Aktualisierung alle 10 Minuten, solange der Screen abonniert ist (Spez. §20)
        viewModelScope.launch {
            while (true) {
                delay(ForecastConfig.RADAR_REFRESH_MINUTES * 60_000)
                refresh()
            }
        }
        // Playback-Schleife
        viewModelScope.launch {
            while (true) {
                delay(650)
                val s = _state.value
                if (s.playing && s.frames.isNotEmpty()) {
                    _state.value = s.copy(selectedIndex = (s.selectedIndex + 1) % s.frames.size)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val result = radarRepository.loadFrames()
            val frames = result.getOrNull()
            if (frames.isNullOrEmpty()) {
                _state.value = _state.value.copy(loading = false, error = true)
            } else {
                val lastPast = frames.indexOfLast { !it.isNowcast }.coerceAtLeast(0)
                _state.value = MapUiState(
                    frames = frames,
                    selectedIndex = lastPast,
                    playing = _state.value.playing,
                    loading = false,
                    error = false,
                )
            }
        }
    }

    fun select(index: Int) {
        val s = _state.value
        if (s.frames.isEmpty()) return
        _state.value = s.copy(selectedIndex = index.coerceIn(0, s.frames.size - 1), playing = false)
    }

    fun togglePlay() {
        _state.value = _state.value.copy(playing = !_state.value.playing)
    }
}
