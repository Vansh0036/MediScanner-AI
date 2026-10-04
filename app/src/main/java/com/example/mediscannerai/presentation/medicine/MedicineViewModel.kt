package com.example.mediscannerai.presentation.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediscannerai.domain.usecase.MedicineInfoUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MedicineUiState {
    data object Idle : MedicineUiState
    data object Loading : MedicineUiState
    data class Success(val name: String, val text: String) : MedicineUiState
    data class NotFound(val name: String) : MedicineUiState
    data class Error(val message: String) : MedicineUiState
}

class MedicineViewModel : ViewModel() {

    private val useCase = MedicineInfoUseCase()

    private val _state = MutableStateFlow<MedicineUiState>(MedicineUiState.Idle)
    val state: StateFlow<MedicineUiState> = _state.asStateFlow()

    fun search(name: String) {
        if (_state.value is MedicineUiState.Loading) return
        val trimmed = name.trim()
        _state.value = MedicineUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val text = useCase(trimmed)
                if (text == null) MedicineUiState.NotFound(trimmed)
                else MedicineUiState.Success(trimmed, text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MedicineUiState.Error(
                    e.localizedMessage ?: "Something went wrong. Please try again."
                )
            }
        }
    }
}

