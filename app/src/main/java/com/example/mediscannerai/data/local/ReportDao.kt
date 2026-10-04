package com.example.mediscannerai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    @Insert
    suspend fun insert(report: ReportEntity): Long

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports")
    suspend fun getAll(): List<ReportEntity>

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getById(id: Long): ReportEntity?

    @Query("UPDATE reports SET doctorQuestions = :questions WHERE id = :id")
    suspend fun updateDoctorQuestions(id: Long, questions: String)

    @Query(
        "UPDATE reports SET rawText = :rawText, aiSummary = :aiSummary, " +
                "doctorQuestions = :doctorQuestions WHERE id = :id"
    )
    suspend fun updateContent(id: Long, rawText: String, aiSummary: String, doctorQuestions: String)

    @Query("UPDATE reports SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteById(id: Long)
}