package com.tempo.rn.feature.tide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tempo.rn.core.model.AppLocation
import com.tempo.rn.core.model.DailySummary
import com.tempo.rn.core.model.Locations
import com.tempo.rn.core.model.MarineHour
import com.tempo.rn.core.model.TideState
import com.tempo.rn.data.LocationRepository
import com.tempo.rn.data.MarineRepository
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
import java.time.LocalDateTime
import javax.inject.Inject

data class TideUiState(
    val location: AppLocation = Locations.default,
    val tide: TideState? = null,
    val marineNow: MarineHour? = null,
    val daily: List<DailySummary> = emptyList(),
    val now: LocalDateTime = LocalDateTime.now(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TideViewModel @Inject constructor(
    locationRepository: LocationRepository,
    tideRepository: TideRepository,
    marineRepository: MarineRepository,
    weatherRepository: WeatherRepository,
) : ViewModel() {

    val state: StateFlow<TideUiState> = locationRepository.selectedLocation
        .flatMapLatest { location ->
            combine(
                tideRepository.observeTideState(location),
                marineRepository.observe(location),
                weatherRepository.observeDaily(location),
            ) { tide, marine, daily ->
                val now = TimeUtil.now()
                TideUiState(
                    location = location,
                    tide = tide,
                    marineNow = marine.lastOrNull { !it.time.isAfter(now) } ?: marine.firstOrNull(),
                    daily = daily,
                    now = now,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TideUiState())
}
