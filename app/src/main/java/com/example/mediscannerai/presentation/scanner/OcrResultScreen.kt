package com.example.mediscannerai.presentation.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mediscannerai.domain.model.ParsedReport
import com.example.mediscannerai.domain.model.TestResult
import com.example.mediscannerai.domain.usecase.ParseReportTextUseCase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text as VisionText
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private sealed class OcrState {
    data object Loading : OcrState()
    data class Success(val text: String) : OcrState()
    data class Error(val message: String) : OcrState()
}

private enum class ResultView { RawText, Structured }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrResultScreen(
    fileUriString: String,
    isPdf: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<OcrState>(OcrState.Loading) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(fileUriString, retryTrigger) {
        state = OcrState.Loading
        state = try {
            val uri = Uri.parse(fileUriString)
            val text = if (isPdf) extractTextFromPdf(context, uri)
            else extractTextFromImage(context, uri)

            if (text.isBlank()) {
                OcrState.Error(
                    "No text could be found in this report. Try retaking with better lighting or a sharper, closer photo."
                )
            } else {
                OcrState.Success(text)
            }
        } catch (e: Exception) {
            OcrState.Error("Something went wrong while reading the report: ${e.localizedMessage ?: "unknown error"}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (val current = state) {
                is OcrState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Reading text from your report…")
                        }
                    }
                }

                is OcrState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = current.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { retryTrigger++ }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is OcrState.Success -> {
                    val parsedReport = remember(current.text) {
                        ParseReportTextUseCase().invoke(current.text)
                    }
                    var viewMode by remember { mutableStateOf(ResultView.RawText) }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = viewMode == ResultView.RawText,
                            onClick = { viewMode = ResultView.RawText },
                            label = { Text("Raw Text") }
                        )
                        FilterChip(
                            selected = viewMode == ResultView.Structured,
                            onClick = { viewMode = ResultView.Structured },
                            label = { Text("Structured (${parsedReport.results.size})") }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    when (viewMode) {
                        ResultView.RawText -> {
                            Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                SelectionContainer {
                                    Text(
                                        text = current.text,
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .verticalScroll(rememberScrollState())
                                    )
                                }
                            }
                        }
                        ResultView.Structured -> {
                            StructuredResultsView(
                                parsedReport = parsedReport,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { retryTrigger++ }) {
                            Text("Retry")
                        }
                        Button(
                            onClick = {
                                android.widget.Toast.makeText(
                                    context,
                                    "AI explanation comes in Phase 7.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Continue")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StructuredResultsView(parsedReport: ParsedReport, modifier: Modifier = Modifier) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        parsedReport.reportDate?.let {
            Text("Report date: $it", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (parsedReport.results.isEmpty()) {
            Text(
                "This app couldn't automatically identify individual test values from " +
                        "this report's layout. You can still view the full extracted text " +
                        "using the Raw Text tab.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            parsedReport.results.forEach { result ->
                TestResultCard(result)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                "These values are shown exactly as extracted from your report. This app " +
                        "does not interpret them — please discuss results with your doctor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TestResultCard(result: TestResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    result.testName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                result.flag?.let { flag ->
                    AssistChip(onClick = {}, label = { Text(flag) })
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val valueText = buildString {
                append(result.value ?: "—")
                result.unit?.let { append(" $it") }
            }
            Text("Value: $valueText", style = MaterialTheme.typography.bodyMedium)
            result.referenceRange?.let {
                Text(
                    "Reference range: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private suspend fun extractTextFromImage(context: Context, uri: Uri): String {
    val image = InputImage.fromFilePath(context, uri)
    val visionText = recognizeTextBlocks(image)
    return reconstructRows(visionText)
}

private suspend fun extractTextFromPdf(context: Context, uri: Uri): String {
    val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        ?: throw IllegalStateException("Could not open the selected PDF.")
    val renderer = PdfRenderer(pfd)
    val builder = StringBuilder()
    try {
        for (pageIndex in 0 until renderer.pageCount) {
            val page = renderer.openPage(pageIndex)
            val scale = 2
            val bitmap = Bitmap.createBitmap(
                page.width * scale,
                page.height * scale,
                Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val visionText = recognizeTextBlocks(InputImage.fromBitmap(bitmap, 0))
            builder.append(reconstructRows(visionText))
            if (pageIndex != renderer.pageCount - 1) {
                builder.append("\n\n--- Page ${pageIndex + 2} ---\n\n")
            }
        }
    } finally {
        renderer.close()
        pfd.close()
    }
    return builder.toString()
}

private suspend fun recognizeTextBlocks(image: InputImage): VisionText {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { visionText -> continuation.resume(visionText) }
            .addOnFailureListener { exception -> continuation.resumeWithException(exception) }
    }
}

/**
 * ML Kit reads text in the blocks it visually detects, which for wide
 * multi-column tables is often column-by-column rather than row-by-row.
 * This rebuilds proper rows by grouping every recognized line whose
 * vertical position overlaps, then ordering each row's pieces left to right —
 * turning a scrambled column dump back into a readable table.
 */
private fun reconstructRows(visionText: VisionText): String {
    data class PositionedLine(val text: String, val top: Int, val bottom: Int, val left: Int)

    val lines = visionText.textBlocks
        .flatMap { block -> block.lines }
        .mapNotNull { line -> line.boundingBox?.let { box -> PositionedLine(line.text, box.top, box.bottom, box.left) } }
        .sortedBy { it.top }

    if (lines.isEmpty()) return visionText.text

    val rows = mutableListOf<MutableList<PositionedLine>>()
    for (line in lines) {
        val lineHeight = (line.bottom - line.top).coerceAtLeast(1)
        val matchingRow = rows.find { row ->
            val rowTop = row.minOf { it.top }
            val rowBottom = row.maxOf { it.bottom }
            val overlap = minOf(line.bottom, rowBottom) - maxOf(line.top, rowTop)
            overlap > lineHeight * 0.5
        }
        if (matchingRow != null) matchingRow.add(line) else rows.add(mutableListOf(line))
    }

    return rows
        .sortedBy { row -> row.minOf { it.top } }
        .joinToString("\n") { row -> row.sortedBy { it.left }.joinToString("    ") { it.text } }
}