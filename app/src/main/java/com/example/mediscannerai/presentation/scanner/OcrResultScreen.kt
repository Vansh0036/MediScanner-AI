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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
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
                    Text("Extracted Text", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { retryTrigger++ }) {
                            Text("Retry")
                        }
                        Button(
                            onClick = {
                                android.widget.Toast.makeText(
                                    context,
                                    "Report processing continues in Phase 6.",
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

private suspend fun extractTextFromImage(context: Context, uri: Uri): String {
    val image = InputImage.fromFilePath(context, uri)
    return recognizeText(image)
}

private suspend fun extractTextFromPdf(context: Context, uri: Uri): String {
    val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        ?: throw IllegalStateException("Could not open the selected PDF.")
    val renderer = PdfRenderer(pfd)
    val builder = StringBuilder()
    try {
        for (pageIndex in 0 until renderer.pageCount) {
            val page = renderer.openPage(pageIndex)
            val scale = 2 // render bigger than screen size for sharper OCR
            val bitmap = Bitmap.createBitmap(
                page.width * scale,
                page.height * scale,
                Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val pageText = recognizeText(InputImage.fromBitmap(bitmap, 0))
            builder.append(pageText)
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

private suspend fun recognizeText(image: InputImage): String {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { visionText -> continuation.resume(visionText.text) }
            .addOnFailureListener { exception -> continuation.resumeWithException(exception) }
    }
}