package com.example.mediscannerai.data.local


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_medicines")
data class SavedMedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val info: String,
    val savedAt: Long
)