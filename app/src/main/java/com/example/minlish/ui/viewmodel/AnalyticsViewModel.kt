package com.example.minlish.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.minlish.data.repository.StudySessionRepository
import com.example.minlish.data.repository.VocabSetRepository
import com.example.minlish.data.repository.WordRepository
import com.example.minlish.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class DailyActivity(
    val dayStart: Long,
    val wordsReviewed: Int,
)

data class DailyRetention(
    val dayStart: Long,
    val retentionPercent: Int,
)

data class DailyObjectiveRetention(
    val dayStart: Long,
    val retentionPercent: Int,
)

data class AnalyticsUiState(
    val streakDays: Int = 0,
    /** Self-rated SRS (Learn / flashcard practice), last 7 days. */
    val accuracyPercent: Int = 0,
    /** Quiz, typing, checkpoint, last 7 days. */
    val objectiveAccuracyPercent: Int = 0,
    val learnedCount: Int = 0,
    val dailyActivity: List<DailyActivity> = emptyList(),
    val dailyRetention: List<DailyRetention> = emptyList(),
    val dailyObjectiveRetention: List<DailyObjectiveRetention> = emptyList(),
    val objectiveReviewedTotalLast7: Int = 0,
    val skillLevel: AnalyticsSkillLevel = AnalyticsSkillLevel.Beginner,
)

enum class AnalyticsSkillLevel {
    Beginner,
    Intermediate,
    Advanced,
}

class AnalyticsViewModel(
    private val studySessionRepository: StudySessionRepository,
    private val wordRepository: WordRepository,
    private val vocabSetRepository: VocabSetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        // Keep learned stats aligned with the active set selection.
        viewModelScope.launch {
            userPreferencesRepository.activeSetId
                .distinctUntilChanged()
                .collectLatest { activeSetId ->
                    val setId =
                        if (activeSetId > 0L) activeSetId else vocabSetRepository.getOrCreateDefaultSetId()
                    wordRepository.getLearnedCountForSet(setId)
                        .collectLatest { learnedCount ->
                            _uiState.update { cur ->
                                cur.copy(
                                    learnedCount = learnedCount,
                                    skillLevel = estimateLevel(
                                        learnedCount = learnedCount,
                                        objectiveAccuracy = cur.objectiveAccuracyPercent,
                                        subjectiveAccuracy = cur.accuracyPercent,
                                        objectiveReviewedTotal = cur.objectiveReviewedTotalLast7,
                                    ),
                                )
                            }
                        }
                }
        }

        viewModelScope.launch {
            studySessionRepository.getAllSessions().collectLatest { sessions ->
                val now = System.currentTimeMillis()
                val todayStart = startOfDayMs(now)
                val dayMs = 24L * 60L * 60L * 1000L
                val last7Start = todayStart - 6L * dayMs

                val recent = sessions.filter { it.date in last7Start..todayStart }
                val totalReviewed = recent.sumOf { it.wordsReviewed }
                val totalCorrect = recent.sumOf { it.correctCount }
                val accuracy = if (totalReviewed == 0) 0 else (100.0 * totalCorrect / totalReviewed).toInt()

                val dailyActivity = (0..6).map { offset ->
                    val day = todayStart - (6 - offset) * dayMs
                    DailyActivity(dayStart = day, wordsReviewed = recent.filter { it.date == day }.sumOf { it.wordsReviewed })
                }

                val dailyRetention = (0..6).map { offset ->
                    val day = todayStart - (6 - offset) * dayMs
                    val daySessions = recent.filter { it.date == day }
                    val reviewed = daySessions.sumOf { it.wordsReviewed }
                    val correct = daySessions.sumOf { it.correctCount }
                    val retention = if (reviewed == 0) 0 else ((100.0 * correct.toDouble()) / reviewed.toDouble()).toInt()
                    DailyRetention(dayStart = day, retentionPercent = retention)
                }

                val totalObjectiveReviewed = recent.sumOf { it.objectiveReviewed }
                val totalObjectiveCorrect = recent.sumOf { it.objectiveCorrect }
                val objectiveAccuracy = if (totalObjectiveReviewed == 0) {
                    0
                } else {
                    (100.0 * totalObjectiveCorrect / totalObjectiveReviewed).toInt()
                }

                val dailyObjectiveRetention = (0..6).map { offset ->
                    val day = todayStart - (6 - offset) * dayMs
                    val daySessions = recent.filter { it.date == day }
                    val reviewed = daySessions.sumOf { it.objectiveReviewed }
                    val correct = daySessions.sumOf { it.objectiveCorrect }
                    val retention = if (reviewed == 0) {
                        0
                    } else {
                        ((100.0 * correct.toDouble()) / reviewed.toDouble()).toInt()
                    }
                    DailyObjectiveRetention(dayStart = day, retentionPercent = retention)
                }

                // Streak: consecutive days with at least one study session.
                val daySet = sessions.map { it.date }.toSet()
                var streak = 0
                var cursor = todayStart
                while (daySet.contains(cursor)) {
                    streak += 1
                    cursor -= dayMs
                }

                _uiState.update { cur ->
                    val level = estimateLevel(
                        learnedCount = cur.learnedCount,
                        objectiveAccuracy = objectiveAccuracy,
                        subjectiveAccuracy = accuracy,
                        objectiveReviewedTotal = totalObjectiveReviewed,
                    )
                    cur.copy(
                        streakDays = streak,
                        accuracyPercent = accuracy,
                        objectiveAccuracyPercent = objectiveAccuracy,
                        objectiveReviewedTotalLast7 = totalObjectiveReviewed,
                        dailyActivity = dailyActivity,
                        dailyRetention = dailyRetention,
                        dailyObjectiveRetention = dailyObjectiveRetention,
                        skillLevel = level,
                    )
                }
            }
        }
    }

    private fun estimateLevel(
        learnedCount: Int,
        objectiveAccuracy: Int,
        subjectiveAccuracy: Int,
        objectiveReviewedTotal: Int,
    ): AnalyticsSkillLevel {
        val accuracyForLevel = if (objectiveReviewedTotal >= 20) {
            objectiveAccuracy
        } else {
            subjectiveAccuracy
        }
        if (accuracyForLevel >= 80 && learnedCount >= 150) return AnalyticsSkillLevel.Advanced
        if (accuracyForLevel >= 60 && learnedCount >= 50) return AnalyticsSkillLevel.Intermediate
        return AnalyticsSkillLevel.Beginner
    }

    private fun startOfDayMs(epochMs: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

class AnalyticsViewModelFactory(
    private val studySessionRepository: StudySessionRepository,
    private val wordRepository: WordRepository,
    private val vocabSetRepository: VocabSetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(
                studySessionRepository,
                wordRepository,
                vocabSetRepository,
                userPreferencesRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

