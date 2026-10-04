package com.example.mediscannerai.presentation.trends

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mediscannerai.data.local.TrendPrefs
import com.example.mediscannerai.domain.usecase.TrendPoint
import com.example.mediscannerai.domain.usecase.TrendSeries
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val HEADER_KEY = "trends_header"
private const val HEADER_COUNT = 1

private val CHART_LEFT = 44.dp
private val CHART_RIGHT = 12.dp

// ---------------------------------------------------------------------------
// Drag-to-reorder state
// ---------------------------------------------------------------------------

/**
 * Handles dragging a card up or down. The dragged card follows the finger, and
 * when its centre moves over a neighbour the two swap places in the list.
 */
private class ReorderState(
    private val listState: LazyListState,
    private val getNames: () -> List<String>,
    private val onReorder: (List<String>) -> Unit,
    private val onFinished: () -> Unit
) {
    var draggingName by mutableStateOf<String?>(null)
        private set
    private var startTop by mutableFloatStateOf(0f)
    private var totalDrag by mutableFloatStateOf(0f)

    fun onDragStart(name: String) {
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == name } ?: return
        draggingName = name
        startTop = info.offset.toFloat()
        totalDrag = 0f
    }

    fun onDrag(dy: Float) {
        val name = draggingName ?: return
        totalDrag += dy

        val names = getNames()
        val fromIndex = names.indexOf(name)
        if (fromIndex < 0) return

        val items = listState.layoutInfo.visibleItemsInfo
        val current = items.firstOrNull { it.key == name } ?: return
        // If the list has not been laid out after the last swap yet, wait.
        if (current.index != fromIndex + HEADER_COUNT) return

        val center = startTop + totalDrag + current.size / 2f
        val target = items.firstOrNull { item ->
            val key = item.key
            key is String && key != name && key != HEADER_KEY &&
                    center >= item.offset && center <= item.offset + item.size
        } ?: return

        val toIndex = names.indexOf(target.key as String)
        if (toIndex < 0 || toIndex == fromIndex) return
        onReorder(names.toMutableList().apply { add(toIndex, removeAt(fromIndex)) })
    }

    fun onDragEnd() {
        if (draggingName == null) return
        draggingName = null
        totalDrag = 0f
        onFinished()
    }

    /** How far the dragged card must be shifted from its slot to sit under the finger. */
    fun translationFor(name: String): Float {
        if (draggingName != name) return 0f
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == name } ?: return 0f
        return startTop + totalDrag - info.offset
    }
}

private fun applyOrder(list: List<TrendSeries>, order: List<String>): List<TrendSeries> {
    val rank = order.withIndex().associate { (index, name) -> name to index }
    // sortedBy is stable, so tests missing from the saved order keep their normal order at the end.
    return list.sortedBy { rank[it.testName] ?: Int.MAX_VALUE }
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    onBack: () -> Unit,
    viewModel: TrendsViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val trends by viewModel.trends.collectAsStateWithLifecycle()
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val selected = trends?.firstOrNull { it.testName == selectedName }

    val listState = rememberLazyListState()
    // First time: tests starred in the earlier version go to the top.
    var order by remember {
        mutableStateOf(TrendPrefs.getOrder(context) ?: TrendPrefs.getPinned(context).sorted())
    }
    val reorder = remember {
        ReorderState(
            listState = listState,
            getNames = { applyOrder(viewModel.trends.value.orEmpty(), order).map { it.testName } },
            onReorder = { order = it },
            onFinished = { TrendPrefs.setOrder(context, order) }
        )
    }

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
                    val canReorder = query.isBlank()
                    val ordered = remember(list, order) { applyOrder(list, order) }
                    val visible = remember(ordered, query) {
                        if (query.isBlank()) ordered
                        else ordered.filter { it.testName.contains(query.trim(), ignoreCase = true) }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { if (it.length <= 40) query = it },
                            label = { Text("Search tests") },
                            placeholder = { Text("For example: Ferritin") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                        )

                        if (visible.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Text(
                                    "No tests match \"${query.trim()}\".",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item(key = HEADER_KEY) {
                                    Text(
                                        if (canReorder) {
                                            "Tap a test to see its chart. Press and drag the ≡ handle to move a test up or down."
                                        } else {
                                            "Tap a test to see its chart. Clear the search to reorder tests."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                items(visible, key = { it.testName }) { series ->
                                    val isDragging = reorder.draggingName == series.testName
                                    SeriesCard(
                                        series = series,
                                        isDragging = isDragging,
                                        showHandle = canReorder,
                                        onClick = { selectedName = series.testName },
                                        onDragStart = { reorder.onDragStart(series.testName) },
                                        onDrag = { reorder.onDrag(it) },
                                        onDragEnd = { reorder.onDragEnd() },
                                        modifier = Modifier
                                            .zIndex(if (isDragging) 1f else 0f)
                                            .graphicsLayer {
                                                translationY = reorder.translationFor(series.testName)
                                            }
                                            .then(if (isDragging) Modifier else Modifier.animateItem())
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesCard(
    series: TrendSeries,
    isDragging: Boolean,
    showHandle: Boolean,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latest = series.points.last()
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
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
            if (showHandle) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .pointerInput(series.testName) {
                            detectDragGestures(
                                onDragStart = { currentOnDragStart() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    currentOnDrag(dragAmount.y)
                                },
                                onDragEnd = { currentOnDragEnd() },
                                onDragCancel = { currentOnDragEnd() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Press and drag to move ${series.testName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Detail screen and chart
// ---------------------------------------------------------------------------

@Composable
private fun TrendDetail(series: TrendSeries) {
    val points = series.points
    val unit = series.unit?.let { " $it" } ?: ""
    val refRange = remember(series.referenceRange) { parseRange(series.referenceRange) }
    var showRange by rememberSaveable { mutableStateOf(true) }
    var selectedIndex by rememberSaveable(series.testName) { mutableStateOf<Int?>(null) }
    val selectedPoint = selectedIndex?.let { points.getOrNull(it) }

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

                    // Readout of the selected point (also useful for screen readers).
                    Text(
                        text = selectedPoint?.let { "${it.dateLabel}:  ${formatValue(it.value)}$unit" }
                            ?: "Tap or drag on the chart to see the exact value and date.",
                        style = if (selectedPoint != null) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodySmall
                        },
                        color = if (selectedPoint != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Value" + (series.unit?.let { " ($it)" } ?: "") + " over time",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TrendChart(
                        points = points,
                        unit = unit,
                        refRange = if (showRange) refRange else null,
                        selectedIndex = selectedIndex,
                        onSelect = { selectedIndex = it }
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
                points.asReversed().forEachIndexed { reversedIndex, point ->
                    val index = points.lastIndex - reversedIndex
                    val isSelected = selectedIndex == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent
                            )
                            .clickable(enabled = points.size >= 2) {
                                selectedIndex = if (isSelected) null else index
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(point.dateLabel)
                        Text(
                            formatValue(point.value) + unit,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
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
    unit: String,
    refRange: Pair<Double, Double>?,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit
) {
    val measurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = MaterialTheme.colorScheme.onSurface
    val bandColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
    val tooltipBackground = MaterialTheme.colorScheme.inverseSurface
    val tooltipText = MaterialTheme.colorScheme.inverseOnSurface
    val surfaceColor = MaterialTheme.colorScheme.surface

    val shortFormat = remember { SimpleDateFormat("MMM yy", Locale.getDefault()) }
    val shortLabels = remember(points) { points.map { shortFormat.format(Date(it.timeMillis)) } }

    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            // Tap: select the nearest point (tap it again to clear).
            .pointerInput(points) {
                val left = CHART_LEFT.toPx()
                val plotW = size.width - left - CHART_RIGHT.toPx()
                detectTapGestures { offset ->
                    val index = nearestIndex(offset.x, points, left, plotW)
                    currentOnSelect(if (index == currentSelected) null else index)
                }
            }
            // Drag: slide along the chart to move between readings.
            .pointerInput(points) {
                val left = CHART_LEFT.toPx()
                val plotW = size.width - left - CHART_RIGHT.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        currentOnSelect(nearestIndex(offset.x, points, left, plotW))
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        currentOnSelect(nearestIndex(change.position.x, points, left, plotW))
                    }
                )
            }
    ) {
        val axisStyle = TextStyle(color = labelColor, fontSize = 11.sp)
        val valueStyle = TextStyle(color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        val tipValueStyle = TextStyle(color = tooltipText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        val tipDateStyle = TextStyle(color = tooltipText, fontSize = 11.sp)

        val left = CHART_LEFT.toPx()
        val right = CHART_RIGHT.toPx()
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

        val selected = selectedIndex?.takeIf { it in points.indices }

        // Dashed guide line for the selected point
        if (selected != null) {
            val sx = xOf(points[selected].timeMillis)
            drawLine(
                color = lineColor.copy(alpha = 0.6f),
                start = Offset(sx, top),
                end = Offset(sx, plotBottom),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        // Points, with the latest one larger
        points.forEachIndexed { index, p ->
            val radius = if (index == points.lastIndex) 7.dp.toPx() else 5.dp.toPx()
            drawCircle(lineColor, radius = radius, center = Offset(xOf(p.timeMillis), yOf(p.value)))
        }

        // Value above each point (only when there are few points; the selected one has its own bubble)
        if (points.size <= 8) {
            points.forEachIndexed { index, p ->
                if (index == selected) return@forEachIndexed
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

        // Highlight ring and the value/date bubble for the selected point
        if (selected != null) {
            val p = points[selected]
            val center = Offset(xOf(p.timeMillis), yOf(p.value))
            drawCircle(lineColor.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = center)
            drawCircle(lineColor, radius = 8.dp.toPx(), center = center)
            drawCircle(surfaceColor, radius = 3.dp.toPx(), center = center)

            val valueLayout = measurer.measure(formatValue(p.value) + unit, tipValueStyle)
            val dateLayout = measurer.measure(p.dateLabel, tipDateStyle)
            val padX = 10.dp.toPx()
            val padY = 6.dp.toPx()
            val boxW = max(valueLayout.size.width, dateLayout.size.width) + 2 * padX
            val boxH = valueLayout.size.height + dateLayout.size.height + 2 * padY
            val boxX = (center.x - boxW / 2f).coerceIn(0f, max(0f, size.width - boxW))
            val gap = 16.dp.toPx()
            var boxY = center.y - gap - boxH
            if (boxY < 0f) boxY = center.y + gap

            drawRoundRect(
                color = tooltipBackground,
                topLeft = Offset(boxX, boxY),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            drawText(
                valueLayout,
                topLeft = Offset(boxX + (boxW - valueLayout.size.width) / 2f, boxY + padY)
            )
            drawText(
                dateLayout,
                topLeft = Offset(
                    boxX + (boxW - dateLayout.size.width) / 2f,
                    boxY + padY + valueLayout.size.height
                )
            )
        }
    }
}

/** The index of the point whose x position is closest to [x]. */
private fun nearestIndex(x: Float, points: List<TrendPoint>, left: Float, plotW: Float): Int {
    val minT = points.first().timeMillis
    val tSpan = (points.last().timeMillis - minT).let { if (it == 0L) 1L else it }
    var best = 0
    var bestDistance = Float.MAX_VALUE
    points.forEachIndexed { index, p ->
        val px = left + plotW * ((p.timeMillis - minT).toFloat() / tSpan.toFloat())
        val distance = abs(px - x)
        if (distance < bestDistance) {
            bestDistance = distance
            best = index
        }
    }
    return best
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