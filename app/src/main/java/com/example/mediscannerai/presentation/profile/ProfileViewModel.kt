package com.example.mediscannerai.presentation.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediscannerai.data.local.ActiveProfile
import com.example.mediscannerai.data.local.AppDatabase
import com.example.mediscannerai.data.local.ProfileEntity
import com.example.mediscannerai.data.repository.ProfileRepository
import com.example.mediscannerai.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = ProfileRepository(database.profileDao())

    val profiles: StateFlow<List<ProfileEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The person on screen now. */
    val active: StateFlow<ProfileEntity?> = combine(profiles, ActiveProfile.id) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch { repository.ensureDefault() }
        // If the saved person no longer exists, fall back to the first one.
        viewModelScope.launch {
            profiles.collect { list ->
                if (list.isNotEmpty() && list.none { it.id == ActiveProfile.id.value }) {
                    ActiveProfile.set(getApplication<Application>(), list.first().id)
                }
            }
        }
    }

    fun select(id: Long) = ActiveProfile.set(getApplication<Application>(), id)

    /** Adds a person and switches to them. */
    fun add(name: String) {
        viewModelScope.launch {
            val id = repository.add(name)
            ActiveProfile.set(getApplication<Application>(), id)
        }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { repository.rename(id, name) }
    }

    /** Removes the person and everything saved for them. The last person cannot be deleted. */
    fun delete(id: Long) {
        if (profiles.value.size <= 1) return
        viewModelScope.launch {
            val reminderDao = database.reminderMedicineDao()
            reminderDao.idsForProfile(id).forEach { database.intakeDao().deleteForMedicine(it) }
            reminderDao.deleteForProfile(id)
            database.reportDao().deleteForProfile(id)
            repository.delete(id)
            ReminderScheduler.rescheduleAll(getApplication<Application>())
        }
    }
}