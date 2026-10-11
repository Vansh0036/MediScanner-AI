package com.example.mediscannerai.presentation.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediscannerai.data.local.ActiveProfile
import com.example.mediscannerai.data.local.AppDatabase
import com.example.mediscannerai.data.local.IntakeEntity
import com.example.mediscannerai.data.local.ReminderMedicineEntity
import com.example.mediscannerai.data.local.ReminderPrefs
import com.example.mediscannerai.data.repository.ReminderRepository
import com.example.mediscannerai.domain.model.MealSlot
import com.example.mediscannerai.reminder.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = ReminderRepository(database.reminderMedicineDao())
    private val intakeDao = database.intakeDao()

    /** Medicines of the person on screen. null while loading, then the list (possibly empty). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val medicines: StateFlow<List<ReminderMedicineEntity>?> = ActiveProfile.id
        .flatMapLatest { repository.observeForProfile(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _slotTimes = MutableStateFlow(
        MealSlot.entries.associateWith {
            ReminderPrefs.getMinutes(getApplication<Application>(), it)
        }
    )
    val slotTimes: StateFlow<Map<MealSlot, Int>> = _slotTimes.asStateFlow()

    // Today as "days since 1970". Refreshed when the screen comes back, so midnight is handled.
    private val _today = MutableStateFlow(LocalDate.now().toEpochDay())
    val todayEpoch: StateFlow<Long> = _today.asStateFlow()

    /** Pairs of (medicine id, slot bit) that were ticked as taken today. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val takenToday: StateFlow<Set<Pair<Long, Int>>> = _today
        .flatMapLatest { intakeDao.observeDay(it) }
        .map { list -> list.map { it.medicineId to it.slot }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun refreshDay() {
        _today.value = LocalDate.now().toEpochDay()
    }

    fun setTaken(medicineId: Long, slot: MealSlot, taken: Boolean) {
        viewModelScope.launch {
            val day = _today.value
            if (taken) {
                intakeDao.insert(
                    IntakeEntity(
                        medicineId = medicineId,
                        slot = slot.bit,
                        day = day,
                        takenAt = System.currentTimeMillis()
                    )
                )
            } else {
                intakeDao.delete(medicineId, slot.bit, day)
            }
        }
    }

    private suspend fun reschedule() =
        ReminderScheduler.rescheduleAll(getApplication<Application>())

    fun setSlotTime(slot: MealSlot, minutes: Int) {
        ReminderPrefs.setMinutes(getApplication<Application>(), slot, minutes)
        _slotTimes.value = _slotTimes.value + (slot to minutes)
        viewModelScope.launch { reschedule() }
    }

    suspend fun load(id: Long): ReminderMedicineEntity? = repository.get(id)

    fun save(medicine: ReminderMedicineEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.save(medicine)
            reschedule()
            onDone()
        }
    }

    fun setPaused(id: Long, paused: Boolean) {
        viewModelScope.launch {
            repository.setPaused(id, paused)
            reschedule()
        }
    }

    fun delete(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.delete(id)
            intakeDao.deleteForMedicine(id)
            reschedule()
            onDone()
        }
    }
}