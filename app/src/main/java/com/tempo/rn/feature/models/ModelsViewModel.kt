package com.tempo.rn.feature.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tempo.rn.core.database.ModelWeightEntity
import com.tempo.rn.core.model.AppLocation
import com.tempo.rn.core.model.ConsolidatedForecast
import com.tempo.rn.core.model.Locations
import com.tempo.rn.core.model.ModelHourValue
import com.tempo.rn.data.LocationRepository
import com.tempo.rn.data.TimeUtil
import com.tempo.rn.data.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import javax.inject.Inject

data class ModelsUiState(
    val location: AppLocation = Locations.default,
    val modelHours: List<ModelHourValue> = emptyList(),
    val forecast: ConsolidatedForecast? = null,
    val weights: List<ModelWeightEntity> = emptyList(),
    val hourOffset: Int = 2,
    val now: LocalDateTime = LocalDateTime.now(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ModelsViewModel @Inject constructor(
    locationRepository: LocationRepository,
    weatherRepository: WeatherRepository,
) : ViewModel() {

    private val hourOffset = MutableStateFlow(2)

    val state: StateFlow<ModelsUiState> = locationRepository.selectedLocation
        .flatMapLatest { location ->
            combine(
                weatherRepository.observeModelHours(location),
                weatherRepository.observeConsolidated(location),
                weatherRepository.observeWeights(),
                hourOffset,
            ) { modelHours, forecast, weights, offset ->
                ModelsUiState(
                    location = location,
                    modelHours = modelHours,
                    forecast = forecast,
                    weights = weights,
                    hourOffset = offset,
                    now = TimeUtil.now(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModelsUiState())

    fun setHourOffset(offset: Int) {
        hourOffset.value = offset.coerceIn(0, 95)
    }
}
