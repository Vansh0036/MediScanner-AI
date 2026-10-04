package com.example.mediscannerai.presentation.scanner

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mediscannerai.data.local.AppDatabase
import com.example.mediscannerai.data.local.ReportSessionHolder
import com.example.mediscannerai.data.repository.ReportRepository
import kotlinx.coroutines.CancellationException

/**
 * Saves the report currently held in ReportSessionHolder (with its explanation
 * and any doctor questions generated so far). Returns true if the report is
 * saved afterwards, including when it had already been saved earlier.
 */
suspend fun saveCurrentReport(context: Context): Boolean {
    if (ReportSessionHolder.savedReportId != null) return true
    val report = ReportSessionHolder.currentReport ?: return false
    val explanation = ReportSessionHolder.explanation ?: return false

    return try {
        val questionsText = ReportSessionHolder.doctorQuestions?.joinToString("\n") ?: ""
        val newId = ReportRepository(AppDatabase.getInstance(context).reportDao())
            .saveReport(report, explanation, questionsText)
        ReportSessionHolder.savedReportId = newId
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }
}

/** Popup shown when the user tries to leave with a report that is not saved. */
@Composable
fun UnsavedReportDialog(
    isSaving: Boolean,
    errorMessage: String?,
    onSaveAndLeave: () -> Unit,
    onLeaveWithoutSaving: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onCancel() },
        title = { Text("Save this report?") },
        text = {
            Column {
                Text(
                    "This report hasn't been saved. If you leave without saving, " +
                            "you'll need to scan it again to see this explanation."
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSaveAndLeave,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSaving) "Saving…" else "Save and go home")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onLeaveWithoutSaving,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Don't save")
                }
                TextButton(
                    onClick = onCancel,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        },
        confirmButton = {}
    )
}