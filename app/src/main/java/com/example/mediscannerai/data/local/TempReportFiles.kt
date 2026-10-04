package com.example.mediscannerai.data.local

import android.content.Context
import java.io.File

/**
 * Report photos and uploaded files are copied into the app's cache so they can
 * be read for OCR. They are sensitive medical documents, so they are deleted as
 * soon as they are no longer needed.
 */
object TempReportFiles {

    private val folderNames = listOf("images", "uploads")

    fun clearAll(context: Context) {
        folderNames.forEach { name ->
            try {
                File(context.cacheDir, name).deleteRecursively()
            } catch (e: Exception) {
                // Cleanup is best-effort and must never crash the app.
            }
        }
    }
}

