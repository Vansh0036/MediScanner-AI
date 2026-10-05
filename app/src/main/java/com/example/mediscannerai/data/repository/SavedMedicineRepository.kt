package com.example.mediscannerai.data.repository

import com.example.mediscannerai.data.local.ReportCrypto
import com.example.mediscannerai.data.local.SavedMedicineDao
import com.example.mediscannerai.data.local.SavedMedicineEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Saved medicine searches. The name and info are encrypted before they are stored. */
class SavedMedicineRepository(private val dao: SavedMedicineDao) {

    fun observeAll(): Flow<List<SavedMedicineEntity>> =
        dao.observeAll()
            .map { list -> list.map { it.decrypted() } }
            .flowOn(Dispatchers.Default)

    /** The saved copy for [name] (ignoring capital letters), or null. */
    suspend fun find(name: String): SavedMedicineEntity? = withContext(Dispatchers.IO) {
        val wanted = name.trim()
        dao.getAll().map { it.decrypted() }
            .firstOrNull { it.name.equals(wanted, ignoreCase = true) }
    }

    /** Saves a lookup, replacing any older copy of the same medicine. */
    suspend fun save(name: String, info: String) = withContext(Dispatchers.IO) {
        val clean = name.trim()
        dao.getAll().forEach { existing ->
            if (existing.decrypted().name.equals(clean, ignoreCase = true)) {
                dao.deleteById(existing.id)
            }
        }
        dao.insert(
            SavedMedicineEntity(
                name = ReportCrypto.encryptIfNeeded(clean),
                info = ReportCrypto.encryptIfNeeded(info),
                savedAt = System.currentTimeMillis()
            )
        )
        dao.trimTo(MAX_SAVED)
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.deleteAll()

    private fun SavedMedicineEntity.decrypted(): SavedMedicineEntity = try {
        copy(name = ReportCrypto.decrypt(name), info = ReportCrypto.decrypt(info))
    } catch (e: Exception) {
        // The key can be missing if the phone was reset. Never crash the screen.
        copy(name = "(could not be decrypted)", info = "")
    }

    private companion object {
        const val MAX_SAVED = 50
    }
}