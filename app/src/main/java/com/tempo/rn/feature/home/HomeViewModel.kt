package com.tempo.rn.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tempo.rn.core.model.AppLocation
import com.tempo.rn.core.model.ConsolidatedForecast
import com.tempo.rn.core.model.DailySummary
import com.tempo.rn.core.model.Locations
import com.tempo.rn.core.model.TideState
import com.tempo.rn.data.LocationRepository
import com.tempo.rn.data.SyncScheduler
import com.tempo.rn.data.TideRepository
import com.tempo.rn.data.TimeUtil
import com.tempo.rn.data.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class HomeUiState(
    val location: AppLocation = Locations.default,
    val allLocations: List<AppLocation> = Locations.all,
    val forecast: ConsolidatedForecast? = null,
    val daily: List<DailySummary> = emptyList(),
    val tide: TideState? = null,
    val now: LocalDateTime = LocalDateTime.now(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
    private val tideRepository: TideRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = locationRepository.selectedLocation
        .flatMapLatest { location ->
            combine(
                weatherRepository.observeConsolidated(location),
                weatherRepository.observeDaily(location),
                tideRepository.observeTideState(location),
            ) { forecast, daily, tide ->
                HomeUiState(
                    location = location,
                    forecast = forecast,
                    daily = daily,
                    tide = tide,
                    now = TimeUtil.now(),
                    isLoading = forecast.hours.isEmpty(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectLocation(location: AppLocation) {
        viewModelScope.launch {
            locationRepository.select(location)
            syncScheduler.refreshNow()
        }
    }

    fun refresh() {
        syncScheduler.refreshNow()
    }
}
