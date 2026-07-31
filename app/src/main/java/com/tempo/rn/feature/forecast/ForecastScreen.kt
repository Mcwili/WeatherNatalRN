package com.tempo.rn.feature.forecast

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tempo.rn.ui.components.ConfidenceChip
import com.tempo.rn.ui.components.ConfidenceStrip
import com.tempo.rn.ui.components.PrecipBarsChart
import com.tempo.rn.ui.components.ProbabilityAreaChart
import com.tempo.rn.ui.components.SectionLabel
import com.tempo.rn.ui.components.TempoCard
import com.tempo.rn.ui.components.ValueLineChart
import com.tempo.rn.ui.components.formatHour
import com.tempo.rn.ui.components.formatMm
import com.tempo.rn.ui.components.updatedAtText
import com.tempo.rn.ui.components.PtBr
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun ForecastScreen(viewModel: ForecastViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val hours = state.forecast?.hours.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Previsão · 96 horas",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                updatedAtText(state.forecast?.fetchedAt, state.now),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ForecastFilter.entries.forEach { f ->
                FilterChip(
                    selected = state.filter == f,
                    onClick = { viewModel.setFilter(f) },
                    label = { Text(f.labelPt) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        if (hours.isEmpty()) {
            TempoCard(tonal = true) {
                Text(
                    "Sem dados de previsão. Verifique a conexão e tente novamente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            return@Column
        }

        val dayFmt = DateTimeFormatter.ofPattern("EEE H'h'", PtBr)
        val axis: @Composable () -> Unit = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0, hours.size / 3, hours.size * 2 / 3, hours.size - 1).distinct().forEach { i ->
                    Text(
                        hours[i].time.format(dayFmt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        when (state.filter) {
            ForecastFilter.RAIN -> {
                TempoCard {
                    SectionLabel("Probabilidade de chuva")
                    Spacer(Modifier.height(8.dp))
                    ProbabilityAreaChart(hours)
                    axis()
                    Spacer(Modifier.height(10.dp))
                    ConfidenceStrip(hours.map { it.confidenceLevel })
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Faixa de confiança por hora: verde = alta, âmbar = moderada, laranja/vermelho = baixa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TempoCard {
                    SectionLabel("Quantidade de chuva")
                    Spacer(Modifier.height(8.dp))
                    PrecipBarsChart(hours)
                    axis()
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Barra cheia = mediana ponderada · faixa clara = intervalo provável (p25–p75)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ForecastFilter.TEMPERATURE -> TempoCard {
                SectionLabel("Temperatura")
                Spacer(Modifier.height(8.dp))
                ValueLineChart(values = hours.map { it.temperatureC }, unit = " °C")
                axis()
            }
            ForecastFilter.WIND -> TempoCard {
                SectionLabel("Vento e rajadas")
                Spacer(Modifier.height(8.dp))
                ValueLineChart(
                    values = hours.map { it.windSpeedKmh },
                    secondaryValues = hours.map { it.windGustKmh },
                    unit = " km/h",
                )
                axis()
                Spacer(Modifier.height(4.dp))
                Text(
                    "Linha cheia = vento médio · tracejada = rajadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ForecastFilter.CLOUDS -> TempoCard {
                SectionLabel("Cobertura de nuvens")
                Spacer(Modifier.height(8.dp))
                ValueLineChart(values = hours.map { it.cloudCoverPercent }, unit = " %")
                axis()
            }
            ForecastFilter.UV -> TempoCard {
                SectionLabel("Índice UV")
                Spacer(Modifier.height(8.dp))
                ValueLineChart(values = hours.map { it.uvIndex })
                axis()
            }
        }

        HourDetailCard(state, onSelect = viewModel::selectHour)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HourDetailCard(state: ForecastUiState, onSelect: (Int?) -> Unit) {
    val hours = state.forecast?.hours.orEmpty()
    if (hours.isEmpty()) return
    val index = (state.selectedHourIndex ?: 0).coerceIn(0, hours.size - 1)
    val hour = hours[index]
    val dayFmt = DateTimeFormatter.ofPattern("EEEE", PtBr)

    TempoCard(tonal = true) {
        SectionLabel("${hour.time.format(dayFmt)} · ${formatHour(hour.time)}")
        Spacer(Modifier.height(6.dp))
        Slider(
            value = index.toFloat(),
            onValueChange = { onSelect(it.roundToInt()) },
            valueRange = 0f..(hours.size - 1).toFloat(),
        )
        Text(
            buildString {
                append("Chance de chuva: ${hour.rainProbabilityPercent}%")
                if (hour.precipMedianMm >= 0.05) {
                    append("\nQuantidade prevista: ${formatMm(hour.precipMedianMm)} mm")
                    append(" · Faixa provável: ${formatMm(hour.precipP25Mm)} a ${formatMm(hour.precipP75Mm)} mm")
                }
                hour.windSpeedKmh?.let { w ->
                    append("\nVento ${w.roundToInt()} km/h")
                    hour.windGustKmh?.let { append(" · rajadas ${it.roundToInt()} km/h") }
                }
                hour.uvIndex?.let { append(" · UV ${it.roundToInt()}") }
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        ConfidenceChip(hour.confidenceLevel)
        Spacer(Modifier.height(6.dp))
        Text(
            "${hour.modelsIndicatingRain} de ${hour.modelsTotal} modelos indicam chuva nesta hora.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
