package com.example.mediscannerai.presentation.scanner

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadReportScreen(
    onBack: () -> Unit,
    onReportSelected: (Uri, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCopying by remember { mutableStateOf(false) }

    fun handlePicked(sourceUri: Uri?, isPdf: Boolean) {
        if (sourceUri == null) {
            errorMessage = if (isPdf) "No PDF selected." else "No image selected."
            return
        }
        isCopying = true
        errorMessage = null
        scope.launch {
            try {
                val localUri = copyToAppCache(context, sourceUri, isPdf)
                if (isPdf) {
                    selectedImageUri = null
                    selectedPdfUri = localUri
                } else {
                    selectedPdfUri = null
                    selectedImageUri = localUri
                }
            } catch (e: Exception) {
                errorMessage = "Couldn't read the selected file. Please try picking it again."
            } finally {
                isCopying = false
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> handlePicked(uri, isPdf = false) }

    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> handlePicked(uri, isPdf = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Report") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                isCopying -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Preparing file…")
                }

                selectedImageUri == null && selectedPdfUri == null -> {
                    Text("Choose a report file from your device.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose Image")
                        }
                        Button(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose PDF")
                        }
                    }
                }

                else -> {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected report image",
                            modifier = Modifier.fillMaxWidth().height(400.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("PDF selected")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = {
                            selectedImageUri = null
                            selectedPdfUri = null
                        }) {
                            Text("Remove")
                        }
                        Button(onClick = {
                            val uri = selectedImageUri ?: selectedPdfUri
                            if (uri != null) {
                                onReportSelected(uri, selectedPdfUri != null)
                            }
                        }) {
                            Text("Use This File")
                        }
                    }
                }
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * Copies the picked file into this app's own cache directory immediately,
 * using the permission grant while it's still fresh. Everything downstream
 * (OCR, navigation) then reads our own copy via FileProvider instead of
 * relying on the source app's URI permission surviving across screens —
 * which some devices/pickers (notably MIUI) don't reliably honor.
 */
private suspend fun copyToAppCache(context: Context, sourceUri: Uri, isPdf: Boolean): Uri =
    withContext(Dispatchers.IO) {
        val extension = if (isPdf) "pdf" else "jpg"
        val dir = File(context.cacheDir, "uploads").apply { mkdirs() }
        val destFile = File.createTempFile("upload_", ".$extension", dir)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Could not open the selected file.")

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            destFile
        )
    }