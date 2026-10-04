package com.example.mediscannerai.presentation.trends

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediscannerai.data.local.AppDatabase
import com.example.mediscannerai.data.repository.ReportRepository
import com.example.mediscannerai.domain.usecase.BuildTrendsUseCase
import com.example.mediscannerai.domain.usecase.TrendSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TrendsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository =
        ReportRepository(AppDatabase.getInstance(application).reportDao())
    private val buildTrends = BuildTrendsUseCase()

    /** null while loading, then the list (possibly empty). */
    val trends: StateFlow<List<TrendSeries>?> = repository.observeAll()
        .map { buildTrends(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}