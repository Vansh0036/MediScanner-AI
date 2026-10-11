package com.example.mediscannerai.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val rawText: String,
    val aiSummary: String,
    val doctorQuestions: String,
    // Which person this report belongs to. Older reports belong to person 1 ("Me").
    @ColumnInfo(defaultValue = "1") val profileId: Long = 1
)