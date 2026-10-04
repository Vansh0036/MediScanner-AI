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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mediscannerai.domain.usecase.TrendPoint
import com.example.mediscannerai.domain.usecase.TrendSeries
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
    val refRange = remember(series.referenceRange) { parseRange(series.referenceRange) }
    var showRange by rememberSaveable { mutableStateOf(true) }

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
                    Text(
                        "Value" + (series.unit?.let { " ($it)" } ?: "") + " over time",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TrendChart(
                        points = points,
                        refRange = if (showRange) refRange else null
                    )
                    if (refRange != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = showRange, onCheckedChange = { showRange = it })
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Show reference range", style = MaterialTheme.typography.bodyMedium)
                                if (showRange) {
                                    Text(
                                        "Shaded band: the range printed on the report " +
                                                "(${formatValue(refRange.first)} to ${formatValue(refRange.second)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Lowest", points.minOf { it.value }, unit, Modifier.weight(1f))
                StatCard("Highest", points.maxOf { it.value }, unit, Modifier.weight(1f))
                StatCard("Latest", last.value, unit, Modifier.weight(1f))
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
private fun StatCard(label: String, value: Double, unit: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                formatValue(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (unit.isNotBlank()) {
                Text(
                    unit.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrendChart(
    points: List<TrendPoint>,
    refRange: Pair<Double, Double>?
) {
    val measurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = MaterialTheme.colorScheme.onSurface
    val bandColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)

    val shortFormat = remember { SimpleDateFormat("MMM yy", Locale.getDefault()) }
    val shortLabels = remember(points) { points.map { shortFormat.format(Date(it.timeMillis)) } }

    Canvas(modifier = Modifier.fillMaxWidth().height(260.dp)) {
        val axisStyle = TextStyle(color = labelColor, fontSize = 11.sp)
        val valueStyle = TextStyle(color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

        val left = 44.dp.toPx()
        val right = 12.dp.toPx()
        val top = 22.dp.toPx()
        val bottom = 28.dp.toPx()
        val plotW = size.width - left - right
        val plotH = size.height - top - bottom
        val plotBottom = top + plotH

        // Value range of the axis (includes the reference band when it is shown).
        var lo = points.minOf { it.value }
        var hi = points.maxOf { it.value }
        if (refRange != null) {
            lo = min(lo, refRange.first)
            hi = max(hi, refRange.second)
        }
        val rawSpan = hi - lo
        val pad = if (rawSpan < 1e-9) max(abs(hi) * 0.1, 1.0) else rawSpan * 0.1
        var axisMin = lo - pad
        val axisMax = hi + pad
        if (lo >= 0 && axisMin < 0) axisMin = 0.0
        val axisSpan = axisMax - axisMin

        val minT = points.first().timeMillis
        val tSpan = (points.last().timeMillis - minT).let { if (it == 0L) 1L else it }

        fun yOf(v: Double): Float =
            top + plotH * (1f - ((v - axisMin) / axisSpan).toFloat())

        fun xOf(t: Long): Float =
            left + plotW * ((t - minT).toFloat() / tSpan.toFloat())

        // Reference range band
        if (refRange != null) {
            val yHigh = yOf(refRange.second)
            val yLow = yOf(refRange.first)
            drawRect(bandColor, topLeft = Offset(left, yHigh), size = Size(plotW, yLow - yHigh))
        }

        // Grid lines with value labels on the Y axis
        for (i in 0..3) {
            val v = axisMin + axisSpan * i / 3.0
            val y = yOf(v)
            drawLine(gridColor, Offset(left, y), Offset(size.width - right, y), strokeWidth = 1.dp.toPx())
            val layout = measurer.measure(formatTick(v, axisSpan), axisStyle)
            drawText(
                layout,
                topLeft = Offset(left - 6.dp.toPx() - layout.size.width, y - layout.size.height / 2f)
            )
        }

        // Area under the line
        val area = Path().apply {
            moveTo(xOf(points.first().timeMillis), plotBottom)
            points.forEach { lineTo(xOf(it.timeMillis), yOf(it.value)) }
            lineTo(xOf(points.last().timeMillis), plotBottom)
            close()
        }
        drawPath(area, lineColor.copy(alpha = 0.12f))

        // The line
        val line = Path()
        points.forEachIndexed { index, p ->
            val x = xOf(p.timeMillis)
            val y = yOf(p.value)
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        drawPath(line, lineColor, style = Stroke(width = 3.dp.toPx()))

        // Points, with the latest one larger
        points.forEachIndexed { index, p ->
            val radius = if (index == points.lastIndex) 7.dp.toPx() else 5.dp.toPx()
            drawCircle(lineColor, radius = radius, center = Offset(xOf(p.timeMillis), yOf(p.value)))
        }

        // Value above each point (only when there are few points)
        if (points.size <= 8) {
            points.forEach { p ->
                val layout = measurer.measure(formatValue(p.value), valueStyle)
                val x = (xOf(p.timeMillis) - layout.size.width / 2f)
                    .coerceIn(left - 4.dp.toPx(), size.width - layout.size.width.toFloat())
                val y = max(yOf(p.value) - 10.dp.toPx() - layout.size.height, 0f)
                drawText(layout, topLeft = Offset(x, y))
            }
        }

        // Dates on the X axis
        val n = points.size
        val indices = if (n <= 4) points.indices.toList()
        else listOf(0, n / 3, 2 * n / 3, n - 1).distinct()
        var lastRight = -1f
        indices.forEach { i ->
            val layout = measurer.measure(shortLabels[i], axisStyle)
            val x = (xOf(points[i].timeMillis) - layout.size.width / 2f)
                .coerceIn(left - 4.dp.toPx(), size.width - layout.size.width.toFloat())
            if (x >= lastRight + 6.dp.toPx()) {
                drawText(layout, topLeft = Offset(x, plotBottom + 8.dp.toPx()))
                lastRight = x + layout.size.width
            }
        }
    }
}

/** Reads a range such as "12-18" or "11.5 - 16.5". Returns null if it can't. */
private fun parseRange(text: String?): Pair<Double, Double>? {
    if (text == null) return null
    val match = Regex("""(\d+(?:\.\d+)?)\s*[-~–]\s*(\d+(?:\.\d+)?)""").find(text) ?: return null
    val a = match.groupValues[1].toDoubleOrNull() ?: return null
    val b = match.groupValues[2].toDoubleOrNull() ?: return null
    return if (a < b) a to b else null
}

private fun formatTick(value: Double, span: Double): String =
    if (span >= 10) String.format(Locale.US, "%.0f", value)
    else String.format(Locale.US, "%.1f", value)

private fun formatValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')