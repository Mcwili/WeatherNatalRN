package com.tempo.rn.feature.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tempo.rn.core.model.WeatherModel
import com.tempo.rn.domain.RainClassifier
import com.tempo.rn.ui.components.ConfidenceChip
import com.tempo.rn.ui.components.ModelBar
import com.tempo.rn.ui.components.SectionLabel
import com.tempo.rn.ui.components.TempoCard
import com.tempo.rn.ui.components.formatHour
import com.tempo.rn.ui.components.formatMm
import com.tempo.rn.ui.components.PtBr
import com.tempo.rn.ui.theme.LocalTempoColors
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ModelsScreen(viewModel: ModelsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = LocalTempoColors.current
    val hours = state.forecast?.hours.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(2.dp))
        Text(
            "Modelos",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (hours.isEmpty() || state.modelHours.isEmpty()) {
            TempoCard(tonal = true) {
                Text(
                    "Sem dados de modelos. Verifique a conexão e tente novamente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            return@Column
        }

        val index = state.hourOffset.coerceIn(0, hours.size - 1)
        val selectedHour = hours[index]
        val target = selectedHour.time
        val dayFmt = DateTimeFormatter.ofPattern("EEEE", PtBr)
        val hourValues = state.modelHours.filter { it.time == target }

        TempoCard(tonal = true) {
            SectionLabel("${target.format(dayFmt)} · ${formatHour(target)}")
            Spacer(Modifier.height(4.dp))
            Slider(
                value = index.toFloat(),
                onValueChange = { viewModel.setHourOffset(it.roundToInt()) },
                valueRange = 0f..(hours.size - 1).toFloat(),
            )
            val rainCount = hourValues.count { RainClassifier.indicatesRain(it.precipitationMm) }
            Text(
                "$rainCount de ${hourValues.size} modelos indicam chuva às ${formatHour(target)}.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            ConfidenceChip(selectedHour.confidenceLevel)
        }

        TempoCard {
            SectionLabel("Chuva prevista às ${formatHour(target)} · mm")
            Spacer(Modifier.height(8.dp))
            val known = WeatherModel.entries
            val maxMm = max(0.5, hourValues.mapNotNull { it.precipitationMm }.maxOrNull() ?: 0.5)
            known.forEachIndexed { i, model ->
                val value = hourValues.firstOrNull { it.model == model }?.precipitationMm
                val color = colors.seriesColors[i % colors.seriesColors.size]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        model.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        modifier = Modifier.width(64.dp),
                    )
                    if (value != null) {
                        ModelBar(
                            fraction = (value / maxMm).toFloat(),
                            color = color,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            "indisponível",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        value?.let { formatMm(it) } ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Consolid.",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(64.dp),
                )
                ModelBar(
                    fraction = (selectedHour.precipMedianMm / maxMm).toFloat(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatMm(selectedHour.precipMedianMm),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(36.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Mediana ponderada · faixa ${formatMm(selectedHour.precipP25Mm)}–${formatMm(selectedHour.precipP75Mm)} mm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TempoCard {
            SectionLabel("Desempenho local")
            Spacer(Modifier.height(6.dp))
            WeatherModel.entries.forEachIndexed { i, model ->
                val weight = state.weights.firstOrNull { it.modelId == model.id }
                val color = colors.seriesColors[i % colors.seriesColors.size]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        model.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        modifier = Modifier.width(64.dp),
                    )
                    val text = if (weight != null && weight.evalCount > 0) {
                        val hitRate = (weight.hitCount * 100.0 / weight.evalCount).roundToInt()
                        "peso ${formatMm(weight.weight)} · acerto chuva $hitRate% · ${weight.evalCount} aval."
                    } else {
                        "peso inicial ${formatMm(model.startWeight)} · ainda sem avaliações locais"
                    }
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "O aplicativo combina diferentes modelos meteorológicos. Modelos com melhor desempenho recente na região recebem um peso um pouco maior.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "A confiança indica o nível de concordância entre os modelos, não uma garantia de que a previsão irá ocorrer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
