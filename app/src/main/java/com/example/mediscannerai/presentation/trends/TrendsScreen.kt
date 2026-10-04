package com.example.mediscannerai.presentation.trends

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mediscannerai.domain.usecase.TrendPoint
import com.example.mediscannerai.domain.usecase.TrendSeries
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    onBack: () -> Unit,
    viewModel: TrendsViewModel = viewModel()
) {
    val trends by viewModel.trends.collectAsStateWithLifecycle()
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = trends?.firstOrNull { it.testName == selectedName }

    BackHandler(enabled = selected != null) { selectedName = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selected?.testName ?: "Health Trends") },
                navigationIcon = {
                    IconButton(onClick = { if (selected != null) selectedName = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val list = trends
            when {
                list == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                list.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No test values found yet. Save a few lab reports and your " +
                                    "results will be tracked here over time.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                selected != null -> TrendDetail(selected)

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "Tap a test to see how your recorded values changed between reports.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(list, key = { it.testName }) { series ->
                            SeriesCard(series) { selectedName = series.testName }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesCard(series: TrendSeries, onClick: () -> Unit) {
    val latest = series.points.last()
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(series.testName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (series.points.size == 1) "1 reading" else "${series.points.size} readings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatValue(latest.value) + (series.unit?.let { " $it" } ?: ""),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    latest.dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrendDetail(series: TrendSeries) {
    val points = series.points
    val unit = series.unit?.let { " $it" } ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (points.size >= 2) {
            val first = points.first()
            val last = points.last()
            val change = when {
                last.value > first.value -> "increased"
                last.value < first.value -> "decreased"
                else -> "stayed the same"
            }
            val sentence = if (change == "stayed the same") {
                "Your recorded value stayed at ${formatValue(last.value)}$unit " +
                        "between ${first.dateLabel} and ${last.dateLabel}."
            } else {
                "Your recorded value $change from ${formatValue(first.value)}$unit " +
                        "(${first.dateLabel}) to ${formatValue(last.value)}$unit (${last.dateLabel})."
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(sentence, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    TrendChart(points)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(first.dateLabel, style = MaterialTheme.typography.bodySmall)
                        Text(last.dateLabel, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Only one reading is saved for this test. Save another report that " +
                            "includes it to see a chart of how it changes.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("All readings", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                points.asReversed().forEach { point ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(point.dateLabel)
                        Text(formatValue(point.value) + unit)
                    }
                }
                series.referenceRange?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Reference range printed on the latest report: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "This chart only shows the numbers recorded on your reports. It does not " +
                        "explain why they changed or what they mean for your health. Please " +
                        "discuss your results with a qualified healthcare professional.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun TrendChart(points: List<TrendPoint>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        val pad = 20.dp.toPx()
        val w = size.width - 2 * pad
        val h = size.height - 2 * pad

        val minV = points.minOf { it.value }
        val maxV = points.maxOf { it.value }
        val vSpan = if (maxV - minV < 1e-9) 1.0 else maxV - minV
        val minT = points.first().timeMillis
        val tSpan = (points.last().timeMillis - minT).let { if (it == 0L) 1L else it }

        fun xOf(p: TrendPoint): Float =
            pad + w * ((p.timeMillis - minT).toFloat() / tSpan.toFloat())

        fun yOf(p: TrendPoint): Float =
            pad + h * (1f - ((p.value - minV) / vSpan).toFloat())

        for (i in 0..2) {
            val y = pad + h * i / 2f
            drawLine(gridColor, Offset(pad, y), Offset(size.width - pad, y), strokeWidth = 1.dp.toPx())
        }

        val path = Path()
        points.forEachIndexed { index, p ->
            val x = xOf(p)
            val y = yOf(p)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx()))
        points.forEach { p ->
            drawCircle(lineColor, radius = 5.dp.toPx(), center = Offset(xOf(p), yOf(p)))
        }
    }
}

private fun formatValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')