package com.example.mediscannerai.presentation.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediscannerai.data.local.ActiveProfile
import com.example.mediscannerai.data.local.AppDatabase
import com.example.mediscannerai.data.local.ReportEntity
import com.example.mediscannerai.data.repository.ReportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository =
        ReportRepository(AppDatabase.getInstance(application).reportDao())

    /** Reports of the person on screen. Switching person switches the list. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val reports: StateFlow<List<ReportEntity>> = ActiveProfile.id
        .flatMapLatest { repository.observeForProfile(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun rename(id: Long, newName: String) {
        viewModelScope.launch { repository.rename(id, newName) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}