package com.tempo.rn.feature.tide

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tempo.rn.core.model.TideDataType
import com.tempo.rn.core.model.TideEvent
import com.tempo.rn.core.model.TideEventType
import com.tempo.rn.ui.components.SectionLabel
import com.tempo.rn.ui.components.TempoCard
import com.tempo.rn.ui.components.TideConfidenceChip
import com.tempo.rn.ui.components.TideCurveChart
import com.tempo.rn.ui.components.formatMeters
import com.tempo.rn.ui.components.formatTime
import com.tempo.rn.ui.components.updatedAtText
import com.tempo.rn.ui.components.PtBr
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun TideScreen(viewModel: TideViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val tide = state.tide
    val now = state.now

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
                "Maré em ${state.location.name}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Ref.: ${tide?.referenceStationPt ?: "Porto de Natal"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (tide == null || tide.curve.size < 3) {
            TempoCard(tonal = true) {
                Text(
                    "Situação da maré indisponível. Aguardando dados do nível do mar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            return@Column
        }

        TempoCard(tonal = true) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tide.direction.labelPt,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TideConfidenceChip(tide.confidence)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                when (tide.dataType) {
                    TideDataType.OFFICIAL_TIDE_TABLE -> "Horários oficiais da DHN disponíveis e boa concordância com o modelo."
                    TideDataType.MODELLED_SEA_LEVEL ->
                        "Os horários oficiais da DHN não estão disponíveis para este período. Exibindo uma estimativa baseada em modelo."
                    else -> "Previsão baseada parcialmente em dados modelados."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NextEventCard(tide.nextLow, "Próxima baixa", state, Modifier.weight(1f))
            NextEventCard(tide.nextHigh, "Próxima alta", state, Modifier.weight(1f))
        }

        val today = state.daily.firstOrNull()
        TempoCard {
            SectionLabel("Curva da maré · 24h")
            Spacer(Modifier.height(8.dp))
            TideCurveChart(
                state = tide,
                windowStart = now.minusHours(4),
                windowHours = 24,
                now = now,
                sunrise = today?.sunrise,
                sunset = today?.sunset,
            )
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(-4L, 2L, 8L, 14L, 20L).forEach { offset ->
                    Text(
                        "${now.plusHours(offset).hour}h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (today?.sunrise != null && today.sunset != null) {
                Text(
                    "☀ ${formatTime(today.sunrise)} / ${formatTime(today.sunset)} · faixa escura = noite",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        EventsCard(tide.events, now)
        MarineCard(state)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun NextEventCard(
    event: TideEvent?,
    label: String,
    state: TideUiState,
    modifier: Modifier = Modifier,
) {
    TempoCard(modifier = modifier) {
        SectionLabel(label)
        Spacer(Modifier.height(2.dp))
        if (event == null) {
            Text(
                "—",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            return@TempoCard
        }
        Text(
            formatTime(event.time),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        val remaining = state.tide?.timeTo(event, state.now)
        Text(
            buildString {
                append("${formatMeters(event.heightM)} m")
                if (remaining != null) append(" · faltam ${remaining.toHours()}h ${remaining.toMinutes() % 60}min")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EventsCard(events: List<TideEvent>, now: java.time.LocalDateTime) {
    val upcoming = events.filter { it.time.isAfter(now.minusHours(1)) }.take(10)
    if (upcoming.isEmpty()) return
    val dayFmt = DateTimeFormatter.ofPattern("EEE dd/MM", PtBr)
    TempoCard {
        SectionLabel("Próximos dias")
        Spacer(Modifier.height(4.dp))
        upcoming.forEachIndexed { i, event ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    event.time.format(dayFmt).removeSuffix("."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (event.type == TideEventType.LOW) "Maré baixa" else "Maré alta",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${formatTime(event.time)} · ${formatMeters(event.heightM)} m",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun MarineCard(state: TideUiState) {
    val marine = state.marineNow ?: return
    TempoCard {
        SectionLabel("Mar e vento agora")
        Spacer(Modifier.height(6.dp))
        val lines = buildList {
            val wave = marine.waveHeightM
            if (wave != null) {
                add(buildString {
                    append("Ondas ${formatMeters(wave)} m")
                    marine.wavePeriodS?.let { append(" · período ${it.roundToInt()} s") }
                    marine.waveDirectionDeg?.let { append(" · ${directionPt(it)}") }
                })
            }
            marine.swellHeightM?.let { add("Swell ${formatMeters(it)} m") }
            marine.seaSurfaceTemperatureC?.let { add("Água ${it.roundToInt()} °C") }
        }
        if (lines.isEmpty()) {
            Text(
                "Dados do mar indisponíveis no momento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            lines.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "As condições podem mudar rapidamente. Consulte as autoridades locais e observe o mar antes de entrar na água.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun directionPt(degrees: Double): String {
    val dirs = listOf("N", "NE", "L", "SE", "S", "SO", "O", "NO")
    val idx = (((degrees + 22.5) / 45.0).toInt()) % 8
    return dirs[idx]
}
