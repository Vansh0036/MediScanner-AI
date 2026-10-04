package com.example.mediscannerai.data.repository

import com.example.mediscannerai.data.local.ReportCrypto
import com.example.mediscannerai.data.local.ReportDao
import com.example.mediscannerai.data.local.ReportEntity
import com.example.mediscannerai.domain.model.ParsedReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * The only place that talks to the report table. Text is encrypted on the way
 * in and decrypted on the way out, so the screens always see normal text.
 */
class ReportRepository(private val dao: ReportDao) {

    suspend fun saveReport(
        report: ParsedReport,
        aiSummary: String,
        doctorQuestions: String = ""
    ): Long {
        val name = report.reportDate?.let { "Lab report - $it" } ?: "Lab report"
        return dao.insert(
            ReportEntity(
                name = name,
                createdAt = System.currentTimeMillis(),
                rawText = ReportCrypto.encryptIfNeeded(report.rawText),
                aiSummary = ReportCrypto.encryptIfNeeded(aiSummary),
                doctorQuestions = ReportCrypto.encryptIfNeeded(doctorQuestions)
            )
        )
    }

    fun observeAll(): Flow<List<ReportEntity>> =
        dao.observeAll()
            .map { list -> list.map { it.decrypted() } }
            .flowOn(Dispatchers.Default)

    suspend fun getById(id: Long): ReportEntity? = dao.getById(id)?.decrypted()

    suspend fun updateDoctorQuestions(id: Long, questions: String) =
        dao.updateDoctorQuestions(id, ReportCrypto.encryptIfNeeded(questions))

    suspend fun rename(id: Long, newName: String) = dao.updateName(id, newName.trim())

    suspend fun delete(id: Long) = dao.deleteById(id)

    /** Encrypts reports saved before encryption existed. Returns how many were changed. */
    suspend fun encryptLegacyReports(): Int = withContext(Dispatchers.IO) {
        var changed = 0
        dao.getAll().forEach { report ->
            val needsWork = listOf(report.rawText, report.aiSummary, report.doctorQuestions)
                .any { it.isNotEmpty() && !ReportCrypto.isEncrypted(it) }
            if (needsWork) {
                dao.updateContent(
                    id = report.id,
                    rawText = ReportCrypto.encryptIfNeeded(report.rawText),
                    aiSummary = ReportCrypto.encryptIfNeeded(report.aiSummary),
                    doctorQuestions = ReportCrypto.encryptIfNeeded(report.doctorQuestions)
                )
                changed++
            }
        }
        changed
    }

    private fun ReportEntity.decrypted(): ReportEntity = try {
        copy(
            rawText = ReportCrypto.decrypt(rawText),
            aiSummary = ReportCrypto.decrypt(aiSummary),
            doctorQuestions = ReportCrypto.decrypt(doctorQuestions)
        )
    } catch (e: Exception) {
        // The key can be missing if the phone was reset. Never crash the screen.
        copy(
            rawText = "",
            aiSummary = "This report could not be decrypted on this device.",
            doctorQuestions = ""
        )
    }
}