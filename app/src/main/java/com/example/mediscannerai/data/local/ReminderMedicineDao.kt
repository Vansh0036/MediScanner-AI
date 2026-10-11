package com.example.mediscannerai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderMedicineDao {

    @Insert
    suspend fun insert(medicine: ReminderMedicineEntity): Long

    @Update
    suspend fun update(medicine: ReminderMedicineEntity)

    @Query("SELECT * FROM reminder_medicines ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ReminderMedicineEntity>>

    @Query("SELECT * FROM reminder_medicines WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun observeForProfile(profileId: Long): Flow<List<ReminderMedicineEntity>>

    @Query("SELECT * FROM reminder_medicines")
    suspend fun getAll(): List<ReminderMedicineEntity>

    @Query("SELECT * FROM reminder_medicines WHERE id = :id")
    suspend fun getById(id: Long): ReminderMedicineEntity?

    @Query("SELECT id FROM reminder_medicines WHERE profileId = :profileId")
    suspend fun idsForProfile(profileId: Long): List<Long>

    @Query("UPDATE reminder_medicines SET isPaused = :paused WHERE id = :id")
    suspend fun setPaused(id: Long, paused: Boolean)

    @Query("DELETE FROM reminder_medicines WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM reminder_medicines WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: Long)
}