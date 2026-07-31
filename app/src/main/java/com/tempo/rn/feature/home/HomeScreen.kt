package com.tempo.rn.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tempo.rn.core.model.ConsolidatedHour
import com.tempo.rn.core.model.DailySummary
import com.tempo.rn.core.model.RainClass
import com.tempo.rn.core.model.TideDirection
import com.tempo.rn.ui.components.ConfidenceChip
import com.tempo.rn.ui.components.OfflineBanner
import com.tempo.rn.ui.components.RainSparkline
import com.tempo.rn.ui.components.SectionLabel
import com.tempo.rn.ui.components.TempoCard
import com.tempo.rn.ui.components.TideCurveChart
import com.tempo.rn.ui.components.formatHour
import com.tempo.rn.ui.components.formatMm
import com.tempo.rn.ui.components.formatMeters
import com.tempo.rn.ui.components.formatTime
import com.tempo.rn.ui.components.updatedAtText
import com.tempo.rn.ui.components.PtBr
import com.tempo.rn.ui.icons.TempoIcons
import com.tempo.rn.ui.icons.WeatherIcons
import androidx.compose.foundation.Image
import java.time.Duration
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val forecast = state.forecast
    val now = state.now
    val currentHour = forecast?.hours?.firstOrNull { !it.time.isBefore(now.minusMinutes(30)) }
        ?: forecast?.hours?.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(2.dp))
        HomeAppBar(state, onSelect = viewModel::selectLocation, onRefresh = viewModel::refresh)

        val fetchedAt = forecast?.fetchedAt
        if (fetchedAt == null || Duration.between(fetchedAt, now).toHours() >= 3) {
            OfflineBanner(
                text = if (fetchedAt == null) "Carregando dados… Se estiver sem conexão, tente novamente mais tarde."
                else "Sem conexão. Exibindo a última previsão disponível."
            )
        }

        NowHeader(currentHour)
        RainCard(state, currentHour)
        TideCard(state)
        HourStrip(forecast?.hours.orEmpty(), now)
        DaysCard(state.daily)

        forecast?.warnings?.takeIf { it.isNotEmpty() }?.let { warnings ->
            TempoCard(tonal = true) {
                SectionLabel("Divergência entre modelos")
                Spacer(Modifier.height(6.dp))
                warnings.forEach { w ->
                    Text(
                        w.messagePt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HomeAppBar(
    state: HomeUiState,
    onSelect: (com.tempo.rn.core.model.AppLocation) -> Unit,
    onRefresh: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clickable { menuOpen = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                state.location.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                TempoIcons.ChevronDown,
                contentDescription = "Escolher local",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                state.allLocations.forEach { loc ->
                    DropdownMenuItem(
                        text = { Text(loc.name) },
                        onClick = {
                            menuOpen = false
                            onSelect(loc)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            updatedAtText(state.forecast?.fetchedAt, state.now),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onRefresh) {
            Icon(
                TempoIcons.Refresh,
                contentDescription = "Atualizar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun NowHeader(hour: ConsolidatedHour?) {
    val (icon, description) = WeatherIcons.forCode(hour?.weatherCode)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = hour?.temperatureC?.let { "${it.roundToInt()}°" } ?: "--°",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Image(imageVector = icon, contentDescription = description, modifier = Modifier.size(52.dp))
    }
    val feels = hour?.apparentC?.let { " · Sensação de ${it.roundToInt()}°" } ?: ""
    Text(
        text = description + feels,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RainCard(state: HomeUiState, currentHour: ConsolidatedHour?) {
    val hours = state.forecast?.hours.orEmpty()
    val next3 = hours.take(3)
    val prob3h = next3.maxOfOrNull { it.rainProbabilityPercent } ?: 0
    val next12 = hours.take(12)
    val onset = next12.firstOrNull { it.rainClass != RainClass.NONE || it.rainProbabilityPercent >= 50 }
    val amount = next12.sumOf { it.precipMedianMm }

    TempoCard(tonal = true) {
        SectionLabel("Vai chover?")
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "$prob3h%",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "de chance nas próximas 3 horas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        val subParts = mutableListOf<String>()
        if (onset != null) {
            subParts += "Possível início entre ${formatHour(onset.time)} e ${formatHour(onset.time.plusHours(1))}"
        }
        if (amount >= 0.1) subParts += "até ${formatMm(amount)} mm"
        if (subParts.isNotEmpty()) {
            Text(
                subParts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        currentHour?.let { ConfidenceChip(it.confidenceLevel) }
        Spacer(Modifier.height(10.dp))
        val probs = hours.take(12).map { it.rainProbabilityPercent }
        RainSparkline(probabilities = probs, highlightIndex = probs.indices.maxByOrNull { probs[it] } ?: 0)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            hours.take(12).filterIndexed { i, _ -> i % 3 == 0 }.forEach {
                Text(
                    formatHour(it.time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TideCard(state: HomeUiState) {
    val tide = state.tide ?: return
    val now = state.now
    TempoCard {
        SectionLabel("Maré · ${tide.referenceStationPt}")
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                tide.direction.labelPt,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            when (tide.direction) {
                TideDirection.FALLING, TideDirection.NEAR_LOW -> Icon(
                    TempoIcons.ArrowDown, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).padding(start = 4.dp),
                )
                TideDirection.RISING, TideDirection.NEAR_HIGH -> Icon(
                    TempoIcons.ArrowUp, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).padding(start = 4.dp),
                )
                else -> {}
            }
            Spacer(Modifier.weight(1f))
            val nextEvent = listOfNotNull(tide.nextLow, tide.nextHigh).minByOrNull { it.time }
            val remaining = nextEvent?.let { tide.timeTo(it, now) }
            if (remaining != null) {
                Text(
                    "Faltam ${remaining.toHours()}h ${remaining.toMinutes() % 60}min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val nextEvent = listOfNotNull(tide.nextLow, tide.nextHigh).minByOrNull { it.time }
        if (nextEvent != null) {
            val label = if (nextEvent.type == com.tempo.rn.core.model.TideEventType.LOW) "Próxima maré baixa" else "Próxima maré alta"
            Text(
                "$label às ${formatTime(nextEvent.time)} · altura prevista ${formatMeters(nextEvent.heightM)} m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                "Situação da maré indisponível",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        TideCurveChart(
            state = tide,
            windowStart = now.minusHours(2),
            windowHours = 12,
            now = now,
            sunrise = null,
            sunset = null,
            compact = true,
        )
    }
}

@Composable
private fun HourStrip(hours: List<ConsolidatedHour>, now: java.time.LocalDateTime) {
    if (hours.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(hours.take(24)) { hour ->
            val isNow = hour == hours.first()
            val shape = RoundedCornerShape(14.dp)
            val bg = if (isNow) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .background(bg, shape)
                    .border(1.dp, if (isNow) bg else MaterialTheme.colorScheme.outline, shape)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (isNow) "Agora" else formatHour(hour.time),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isNow) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val (icon, desc) = WeatherIcons.forCode(hour.weatherCode)
                Image(imageVector = icon, contentDescription = desc, modifier = Modifier.size(22.dp).padding(top = 2.dp))
                Text(
                    hour.temperatureC?.let { "${it.roundToInt()}°" } ?: "--",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isNow) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${hour.rainProbabilityPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DaysCard(daily: List<DailySummary>) {
    if (daily.isEmpty()) return
    val dayFmt = DateTimeFormatter.ofPattern("EEE", PtBr)
    TempoCard {
        SectionLabel("Próximos dias")
        Spacer(Modifier.height(4.dp))
        daily.take(4).forEachIndexed { index, day ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    day.date.format(dayFmt).removeSuffix("."),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(44.dp),
                )
                val (icon, desc) = WeatherIcons.forCode(day.weatherCode)
                Image(imageVector = icon, contentDescription = desc, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                val meta = buildList {
                    day.precipProbabilityMaxPercent?.let { add("${it.roundToInt()}%") }
                    day.precipSumMm?.takeIf { it >= 0.1 }?.let { add("${formatMm(it)} mm") }
                    day.uvIndexMax?.let { add("UV ${it.roundToInt()}") }
                    day.windMaxKmh?.let { add("${it.roundToInt()} km/h") }
                }.joinToString(" · ")
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${day.tMinC?.roundToInt() ?: "--"}° / ${day.tMaxC?.roundToInt() ?: "--"}°",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
