package com.example.mediscannerai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMedicineDao {

    @Insert
    suspend fun insert(medicine: SavedMedicineEntity): Long

    @Query("SELECT * FROM saved_medicines ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedMedicineEntity>>

    @Query("SELECT * FROM saved_medicines")
    suspend fun getAll(): List<SavedMedicineEntity>

    @Query("DELETE FROM saved_medicines WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_medicines")
    suspend fun deleteAll()

    // Keeps only the newest [keep] rows.
    @Query(
        "DELETE FROM saved_medicines WHERE id NOT IN " +
                "(SELECT id FROM saved_medicines ORDER BY savedAt DESC LIMIT :keep)"
    )
    suspend fun trimTo(keep: Int)
}