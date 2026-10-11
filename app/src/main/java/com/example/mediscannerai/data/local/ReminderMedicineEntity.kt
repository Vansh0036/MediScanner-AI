package com.example.mediscannerai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_medicines")
data class ReminderMedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountNote: String,
    val notes: String,
    val slotsMask: Int,
    val daysMask: Int,
    val startDay: Long?,
    val endDay: Long?,
    val isPaused: Boolean,
    val createdAt: Long,
    val profileId: Long
)