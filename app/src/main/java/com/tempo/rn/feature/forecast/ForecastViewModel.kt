package com.tempo.rn.feature.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tempo.rn.core.model.AppLocation
import com.tempo.rn.core.model.ConsolidatedForecast
import com.tempo.rn.core.model.Locations
import com.tempo.rn.data.LocationRepository
import com.tempo.rn.data.TimeUtil
import com.tempo.rn.data.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import javax.inject.Inject

enum class ForecastFilter(val labelPt: String) {
    RAIN("Chuva"),
    TEMPERATURE("Temperatura"),
    WIND("Vento"),
    CLOUDS("Nuvens"),
    UV("UV"),
}

data class ForecastUiState(
    val location: AppLocation = Locations.default,
    val forecast: ConsolidatedForecast? = null,
    val filter: ForecastFilter = ForecastFilter.RAIN,
    val selectedHourIndex: Int? = null,
    val now: LocalDateTime = LocalDateTime.now(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ForecastViewModel @Inject constructor(
    locationRepository: LocationRepository,
    weatherRepository: WeatherRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(ForecastFilter.RAIN)
    private val selectedHour = MutableStateFlow<Int?>(null)

    val state: StateFlow<ForecastUiState> = locationRepository.selectedLocation
        .flatMapLatest { location ->
            weatherRepository.observeConsolidated(location).map { location to it }
        }
        .combine(filter) { (location, forecast), f ->
            ForecastUiState(location = location, forecast = forecast, filter = f, now = TimeUtil.now())
        }
        .combine(selectedHour) { s, idx -> s.copy(selectedHourIndex = idx) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForecastUiState())

    fun setFilter(f: ForecastFilter) {
        filter.value = f
    }

    fun selectHour(index: Int?) {
        selectedHour.value = index
    }
}
