package com.example.mediscannerai.data.repository

import com.example.mediscannerai.data.local.ReportDao
import com.example.mediscannerai.data.local.ReportEntity
import com.example.mediscannerai.domain.model.ParsedReport
import kotlinx.coroutines.flow.Flow

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
                rawText = report.rawText,
                aiSummary = aiSummary,
                doctorQuestions = doctorQuestions
            )
        )
    }

    fun observeAll(): Flow<List<ReportEntity>> = dao.observeAll()

    suspend fun getById(id: Long): ReportEntity? = dao.getById(id)

    suspend fun updateDoctorQuestions(id: Long, questions: String) =
        dao.updateDoctorQuestions(id, questions)

    suspend fun rename(id: Long, newName: String) = dao.updateName(id, newName.trim())

    suspend fun delete(id: Long) = dao.deleteById(id)
}