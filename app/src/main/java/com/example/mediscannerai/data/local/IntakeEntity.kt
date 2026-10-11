package com.example.mediscannerai.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One "taken" tick: this medicine, in this time slot, on this day. */
@Entity(
    tableName = "intake_log",
    indices = [Index(value = ["medicineId", "slot", "day"], unique = true)]
)
data class IntakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val slot: Int,
    val day: Long,
    val takenAt: Long
)