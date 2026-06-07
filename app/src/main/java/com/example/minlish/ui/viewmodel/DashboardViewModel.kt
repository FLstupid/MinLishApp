package com.example.minlish.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.math.min

enum class DashboardPrimaryCta {
    StartDaily,
    StudyMore,
    OpenLibrary,
}

data class DashboardUiState(
    val reviewCount: Int = 0,
    val newAvailableCount: Int = 0,
    val poolRemainingCount: Int = 0,
    val learnedCount: Int = 0,
    val setWordCount: Int = 0,
    val streakDays: Int = 0,
    val accuracyPercent: Int = 0,
    val objectiveAccuracyPercent: Int = 0,
    val skillLevel: AnalyticsSkillLevel = AnalyticsSkillLevel.Beginner,
    val hasDailyQueue: Boolean = false,
    val canStudyMore: Boolean = false,
    val bonusBatch: Int = 0,
    val primaryCta: DashboardPrimaryCta = DashboardPrimaryCta.OpenLibrary,
    val showCheckpoint: Boolean = false,
)

class DashboardViewModel(
    reviewCount: Flow<Int>,
    newAvailableCount: Flow<Int>,
    poolRemainingCount: Flow<Int>,
    learnedCount: Flow<Int>,
    setWordCount: Flow<Int>,
    analyticsUiState: Flow<AnalyticsUiState>,
    private val bonusBatchSize: () -> Int,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        reviewCount,
        newAvailableCount,
        poolRemainingCount,
        learnedCount,
        setWordCount,
        analyticsUiState,
    ) { values ->
        val review = values[0] as Int
        val newAvail = values[1] as Int
        val pool = values[2] as Int
        val learned = values[3] as Int
        val setTotal = values[4] as Int
        val analytics = values[5] as AnalyticsUiState
        val hasDailyQueue = newAvail > 0 || review > 0
        val bonus = min(bonusBatchSize(), pool)
        val canStudyMore = !hasDailyQueue && pool > 0
        val primaryCta = when {
            hasDailyQueue -> DashboardPrimaryCta.StartDaily
            canStudyMore -> DashboardPrimaryCta.StudyMore
            else -> DashboardPrimaryCta.OpenLibrary
        }
        DashboardUiState(
            reviewCount = review,
            newAvailableCount = newAvail,
            poolRemainingCount = pool,
            learnedCount = learned,
            setWordCount = setTotal,
            streakDays = analytics.streakDays,
            accuracyPercent = analytics.accuracyPercent,
            objectiveAccuracyPercent = analytics.objectiveAccuracyPercent,
            skillLevel = analytics.skillLevel,
            hasDailyQueue = hasDailyQueue,
            canStudyMore = canStudyMore,
            bonusBatch = bonus,
            primaryCta = primaryCta,
            showCheckpoint = learned >= CheckpointViewModel.MIN_STUDIED_WORDS,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )
}

class DashboardViewModelFactory(
    private val wordViewModel: WordViewModel,
    private val analyticsViewModel: AnalyticsViewModel,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                reviewCount = wordViewModel.reviewCount,
                newAvailableCount = wordViewModel.newAvailableCount,
                poolRemainingCount = wordViewModel.poolRemainingCount,
                learnedCount = wordViewModel.learnedCount,
                setWordCount = wordViewModel.setWordCount,
                analyticsUiState = analyticsViewModel.uiState,
                bonusBatchSize = { wordViewModel.bonusBatchSize() },
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
