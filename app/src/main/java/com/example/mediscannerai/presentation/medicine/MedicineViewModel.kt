package com.example.mediscannerai.presentation.medicine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediscannerai.data.local.AppDatabase
import com.example.mediscannerai.data.local.SavedMedicineEntity
import com.example.mediscannerai.data.repository.SavedMedicineRepository
import com.example.mediscannerai.domain.usecase.MedicineInfoUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MedicineUiState {
    data object Idle : MedicineUiState
    data object Loading : MedicineUiState
    data class Success(
        val name: String,
        val text: String,
        val savedAt: Long,
        val fromSaved: Boolean
    ) : MedicineUiState
    data class NotFound(val name: String) : MedicineUiState
    data class Error(val message: String) : MedicineUiState
}

class MedicineViewModel(application: Application) : AndroidViewModel(application) {

    private val useCase = MedicineInfoUseCase()
    private val repository =
        SavedMedicineRepository(AppDatabase.getInstance(application).savedMedicineDao())

    private val _state = MutableStateFlow<MedicineUiState>(MedicineUiState.Idle)
    val state: StateFlow<MedicineUiState> = _state.asStateFlow()

    val saved: StateFlow<List<SavedMedicineEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Shows the saved copy when there is one, unless [forceRefresh] asks for a new lookup. */
    fun search(name: String, forceRefresh: Boolean = false) {
        if (_state.value is MedicineUiState.Loading) return
        val trimmed = name.trim()
        _state.value = MedicineUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                lookUp(trimmed, forceRefresh)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MedicineUiState.Error(
                    e.localizedMessage ?: "Something went wrong. Please try again."
                )
            }
        }
    }

    private suspend fun lookUp(name: String, forceRefresh: Boolean): MedicineUiState {
        if (!forceRefresh) {
            repository.find(name)?.let {
                return MedicineUiState.Success(it.name, it.info, it.savedAt, fromSaved = true)
            }
        }
        val text = useCase(name) ?: return MedicineUiState.NotFound(name)
        // A failed save should not hide a result that was found.
        try {
            repository.save(name, text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // ignored on purpose
        }
        return MedicineUiState.Success(name, text, System.currentTimeMillis(), fromSaved = false)
    }

    fun openSaved(item: SavedMedicineEntity) {
        _state.value = MedicineUiState.Success(item.name, item.info, item.savedAt, fromSaved = true)
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }
}