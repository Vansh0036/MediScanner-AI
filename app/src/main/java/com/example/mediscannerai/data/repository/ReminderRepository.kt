package com.example.mediscannerai.data.repository

import com.example.mediscannerai.data.local.ActiveProfile
import com.example.mediscannerai.data.local.ReminderMedicineDao
import com.example.mediscannerai.data.local.ReminderMedicineEntity
import com.example.mediscannerai.data.local.ReportCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Medicine names, amounts and notes are encrypted before they are stored. */
class ReminderRepository(private val dao: ReminderMedicineDao) {

    fun observeAll(): Flow<List<ReminderMedicineEntity>> =
        dao.observeAll()
            .map { list -> list.map { it.decrypted() } }
            .flowOn(Dispatchers.Default)

    fun observeForProfile(profileId: Long): Flow<List<ReminderMedicineEntity>> =
        dao.observeForProfile(profileId)
            .map { list -> list.map { it.decrypted() } }
            .flowOn(Dispatchers.Default)

    suspend fun getAll(): List<ReminderMedicineEntity> = dao.getAll().map { it.decrypted() }

    suspend fun get(id: Long): ReminderMedicineEntity? = dao.getById(id)?.decrypted()

    /** Inserts a new medicine (id 0) for the active person, or updates an existing one. */
    suspend fun save(medicine: ReminderMedicineEntity): Long {
        val owner = if (medicine.id == 0L) {
            ActiveProfile.id.value
        } else {
            dao.getById(medicine.id)?.profileId ?: medicine.profileId
        }
        val encrypted = medicine.copy(
            profileId = owner,
            name = ReportCrypto.encryptIfNeeded(medicine.name.trim()),
            amountNote = ReportCrypto.encryptIfNeeded(medicine.amountNote.trim()),
            notes = ReportCrypto.encryptIfNeeded(medicine.notes.trim())
        )
        return if (medicine.id == 0L) {
            dao.insert(encrypted)
        } else {
            dao.update(encrypted)
            medicine.id
        }
    }

    suspend fun setPaused(id: Long, paused: Boolean) = dao.setPaused(id, paused)

    suspend fun delete(id: Long) = dao.deleteById(id)

    private fun ReminderMedicineEntity.decrypted(): ReminderMedicineEntity = try {
        copy(
            name = ReportCrypto.decrypt(name),
            amountNote = ReportCrypto.decrypt(amountNote),
            notes = ReportCrypto.decrypt(notes)
        )
    } catch (e: Exception) {
        // The key can be missing if the phone was reset. Never crash the screen.
        copy(name = "(could not be decrypted)", amountNote = "", notes = "")
    }
}