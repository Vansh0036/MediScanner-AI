package com.example.mediscannerai.data.repository

import com.example.mediscannerai.data.local.ProfileDao
import com.example.mediscannerai.data.local.ProfileEntity
import com.example.mediscannerai.data.local.ReportCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** People's names are encrypted before they are stored. */
class ProfileRepository(private val dao: ProfileDao) {

    fun observeAll(): Flow<List<ProfileEntity>> =
        dao.observeAll()
            .map { list -> list.map { it.decrypted() } }
            .flowOn(Dispatchers.Default)

    suspend fun getAll(): List<ProfileEntity> = dao.getAll().map { it.decrypted() }

    /** Makes sure "Me" exists. A fresh install has no migration to create it. */
    suspend fun ensureDefault() {
        if (dao.count() == 0) {
            dao.insert(
                ProfileEntity(
                    id = 1,
                    name = ReportCrypto.encryptIfNeeded("Me"),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun add(name: String): Long = dao.insert(
        ProfileEntity(
            name = ReportCrypto.encryptIfNeeded(name.trim()),
            createdAt = System.currentTimeMillis()
        )
    )

    suspend fun rename(id: Long, name: String) =
        dao.updateName(id, ReportCrypto.encryptIfNeeded(name.trim()))

    suspend fun delete(id: Long) = dao.deleteById(id)

    private fun ProfileEntity.decrypted(): ProfileEntity = try {
        copy(name = ReportCrypto.decrypt(name))
    } catch (e: Exception) {
        copy(name = "(could not be decrypted)")
    }
}