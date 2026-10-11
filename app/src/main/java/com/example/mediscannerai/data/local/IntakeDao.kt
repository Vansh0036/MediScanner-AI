package com.example.mediscannerai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: IntakeEntity): Long

    @Query("SELECT * FROM intake_log WHERE day = :day")
    fun observeDay(day: Long): Flow<List<IntakeEntity>>

    @Query("SELECT * FROM intake_log WHERE day = :day")
    suspend fun getForDay(day: Long): List<IntakeEntity>

    @Query("DELETE FROM intake_log WHERE medicineId = :medicineId AND slot = :slot AND day = :day")
    suspend fun delete(medicineId: Long, slot: Int, day: Long)

    @Query("DELETE FROM intake_log WHERE medicineId = :medicineId")
    suspend fun deleteForMedicine(medicineId: Long)
}