package com.example.mediscannerai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Query("SELECT * FROM profiles ORDER BY id")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY id")
    suspend fun getAll(): List<ProfileEntity>

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int

    @Query("UPDATE profiles SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}