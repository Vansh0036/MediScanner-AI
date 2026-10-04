package com.example.mediscannerai.data.local

import android.content.Context
import com.example.mediscannerai.data.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs at app start. Encrypts any reports saved before encryption was added.
 * Safe to run every time: reports that are already encrypted are skipped.
 */
object ReportMigration {

    suspend fun run(context: Context) = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getInstance(context)
            val changed = ReportRepository(database.reportDao()).encryptLegacyReports()
            if (changed > 0) {
                // Old plain text can linger in free database pages and the write-ahead log.
                // This rewrites the file so those leftovers are removed.
                val sql = database.openHelper.writableDatabase
                sql.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
                sql.execSQL("VACUUM")
            }
        } catch (e: Exception) {
            // Never stop the app from opening. The next start tries again.
        }
    }
}