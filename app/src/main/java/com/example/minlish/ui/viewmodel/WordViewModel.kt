package com.example.minlish.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.minlish.data.model.Word
import com.example.minlish.data.preferences.UserPreferencesRepository
import com.example.minlish.data.repository.VocabSetRepository
import com.example.minlish.data.repository.StudySessionRepository
import com.example.minlish.data.repository.WordRepository
import com.example.minlish.logic.CefrLevels
import com.example.minlish.logic.SrsEngine
import com.example.minlish.logic.StudyQueueBuilder
import com.example.minlish.logic.StudySessionRecorder
import com.example.minlish.logic.startOfDayMs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import android.util.Log
import kotlinx.coroutines.launch

enum class LearnQueueMode {
    Daily,
    Bonus,
}

class WordViewModel(
    private val repository: WordRepository,
    private val vocabSetRepository: VocabSetRepository,
    private val studySessionRepository: StudySessionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LearnUiState>(LearnUiState.Loading)
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    private val _reviewCount = MutableStateFlow(0)
    val reviewCount: StateFlow<Int> = _reviewCount.asStateFlow()

    private val _newAvailableCount = MutableStateFlow(0)
    val newAvailableCount: StateFlow<Int> = _newAvailableCount.asStateFlow()

    private val _poolRemainingCount = MutableStateFlow(0)
    val poolRemainingCount: StateFlow<Int> = _poolRemainingCount.asStateFlow()

    private val _learnedCount = MutableStateFlow(0)
    val learnedCount: StateFlow<Int> = _learnedCount.asStateFlow()

    private val _setWordCount = MutableStateFlow(0)
    val setWordCount: StateFlow<Int> = _setWordCount.asStateFlow()

    private var currentWords = listOf<Word>()
    private var currentIndex = 0
    /** Unique lemmas in queue at session start (before in-session retries). */
    private var initialQueueUniqueSize = 0
    private var currentSetId: Long = 0L
    private var queueMode: LearnQueueMode = LearnQueueMode.Daily

    private var dailyGoal: Int = 10
    private var userLevel: String = CefrLevels.DEFAULT_LEVEL

    private val sessionRecorder = StudySessionRecorder(studySessionRepository, viewModelScope)
    private var sessionWordsReviewed: Int = 0
    private var answerInFlight: Boolean = false

    private val _isAnswering = MutableStateFlow(false)
    val isAnswering: StateFlow<Boolean> = _isAnswering.asStateFlow()

    private val _offerCheckpoint = MutableStateFlow(false)
    val offerCheckpoint: StateFlow<Boolean> = _offerCheckpoint.asStateFlow()

    private var statsCollectionJob: Job? = null
    private var queueJob: Job? = null

    init {
        viewModelScope.launch {
            repository.syncFromCurrentUser()
            userPreferencesRepository.activeSetId
                .distinctUntilChanged()
                .collectLatest { activeSetId ->
                    currentSetId = if (activeSetId > 0L) {
                        activeSetId
                    } else {
                        vocabSetRepository.getOrCreateDefaultSetId()
                    }
                    queueMode = LearnQueueMode.Daily
                    updateStats()
                    loadQueue()
                }
        }
    }

    private suspend fun resolveCurrentSetId(): Long {
        if (currentSetId > 0L) return currentSetId
        val resolved = vocabSetRepository.getOrCreateDefaultSetId()
        currentSetId = resolved
        return resolved
    }

    fun setDailyGoal(goal: Int) {
        val safeGoal = goal.coerceAtLeast(0)
        if (safeGoal == dailyGoal) return
        dailyGoal = safeGoal
        if (shouldDeferQueueReload()) return
        queueMode = LearnQueueMode.Daily
        loadQueue()
    }

    fun setUserLevel(level: String) {
        val normalized = CefrLevels.normalize(level)
        if (normalized == userLevel) return
        userLevel = normalized
        if (shouldDeferQueueReload()) return
        queueMode = LearnQueueMode.Daily
        loadQueue()
    }

    /** Reload daily queue when opening Learn tab (no-op while a session is in progress). */
    fun refreshDailyQueueIfIdle() {
        if (shouldDeferQueueReload()) return
        queueMode = LearnQueueMode.Daily
        loadQueue()
    }

    fun consumeCheckpointOffer() {
        _offerCheckpoint.value = false
    }

    fun startBonusSession() {
        if (shouldDeferQueueReload()) return
        queueMode = LearnQueueMode.Bonus
        sessionWordsReviewed = 0
        loadQueue(force = true)
    }

    fun bonusBatchSize(): Int = bonusBatchSizeForGoal(dailyGoal)

    private fun shouldDeferQueueReload(): Boolean =
        answerInFlight || sessionWordsReviewed > 0 || currentIndex > 0

    private fun loadQueue(force: Boolean = false) {
        if (!force && shouldDeferQueueReload()) return
        queueJob?.cancel()
        queueJob = viewModelScope.launch {
            val setId = resolveCurrentSetId()
            val now = System.currentTimeMillis()
            val mode = queueMode

            val poolTotal = repository.countIntroducePoolForLevel(setId, userLevel)
            _poolRemainingCount.value = poolTotal

            val todayKey = startOfDayMs(now)
            val studiedToday = userPreferencesRepository.getAndResetDailyNewStudiedCount(todayKey)
            val dailyNewLimit = (dailyGoal - studiedToday).coerceAtLeast(0)

            val newLimit = when (mode) {
                LearnQueueMode.Daily -> dailyNewLimit
                LearnQueueMode.Bonus -> bonusBatchSizeForGoal(dailyGoal).coerceAtMost(poolTotal)
            }

            val dueWords = repository.getDueReviewWords(setId, now).first()
            val pendingRetries = repository.getPendingIntroductionRetries(setId, userLevel, now)
            val introduceWords = if (newLimit > 0) {
                repository.getIntroduceWordsForLevel(setId, userLevel, newLimit)
            } else {
                emptyList()
            }

            _reviewCount.value = dueWords.size
            _newAvailableCount.value = dailyNewLimit.coerceAtMost(poolTotal)

            currentWords = StudyQueueBuilder.buildDailyQueue(
                dueWords = dueWords,
                pendingIntroductionRetries = pendingRetries,
                introduceWords = introduceWords,
            )
            currentIndex = 0
            initialQueueUniqueSize = currentWords.size

            val totalWords = repository.getWordCountForSet(setId).first()
            if (currentWords.isEmpty()) {
                _uiState.value = if (totalWords == 0) {
                    LearnUiState.EmptyNoWords
                } else {
                    LearnUiState.EmptyAllCaughtUp
                }
                return@launch
            }

            _uiState.value = buildLearnSuccess()
        }
    }


    private fun buildLearnSuccess(): LearnUiState.Success {
        val uniqueTotal = initialQueueUniqueSize
        return LearnUiState.Success(
            word = currentWords[currentIndex],
            uniqueIndex = (currentIndex + 1).coerceAtMost(uniqueTotal.coerceAtLeast(1)),
            uniqueTotal = uniqueTotal,
            cardIndex = currentIndex + 1,
            cardTotal = currentWords.size,
        )
    }

    private fun updateStats() {
        statsCollectionJob?.cancel()
        statsCollectionJob = viewModelScope.launch {
            val setId = resolveCurrentSetId()
            launch {
                repository.getLearnedCountForSet(setId).collect { learnedCount ->
                    _learnedCount.value = learnedCount
                }
            }
            launch {
                repository.getWordCountForSet(setId).collect { count ->
                    _setWordCount.value = count
                }
            }
        }
    }

    fun onAnswer(quality: Int) {
        if (answerInFlight) return
        val state = _uiState.value
        if (state is LearnUiState.Success) {
            val wasFirstIntroduction = state.word.lastReviewed == null
            val retryIntroductionSameSession =
                quality < 3 && state.word.repetitions == 0 && wasFirstIntroduction

            val updatedWord = SrsEngine.calculateNextReview(state.word, quality)
            answerInFlight = true
            _isAnswering.value = true
            sessionWordsReviewed += 1
            sessionRecorder.recordAnswer(quality)
            viewModelScope.launch {
                try {
                    repository.updateWordLocal(updatedWord)
                    if (
                        queueMode == LearnQueueMode.Daily &&
                        wasFirstIntroduction &&
                        quality >= 3
                    ) {
                        val todayKey = startOfDayMs(System.currentTimeMillis())
                        userPreferencesRepository.incrementDailyNewStudiedCount(todayKey)
                    }
                    launch {
                        repository.syncWordToCloud(updatedWord)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to persist SRS answer for word ${updatedWord.id}", e)
                } finally {
                    showNextWord(
                        retryWord = if (retryIntroductionSameSession) updatedWord else null,
                    )
                }
            }
        }
    }

    private fun showNextWord(retryWord: Word? = null) {
        answerInFlight = false
        _isAnswering.value = false
        if (retryWord != null) {
            currentWords = currentWords + retryWord
        }
        currentIndex++
        if (currentIndex < currentWords.size) {
            _uiState.value = buildLearnSuccess()
        } else {
            val reviewedThisSession = sessionWordsReviewed
            sessionRecorder.flushIfActive()
            if (reviewedThisSession > 0) {
                _offerCheckpoint.value = true
            }
            sessionWordsReviewed = 0
            queueMode = LearnQueueMode.Daily
            loadQueue(force = true)
        }
    }

    private companion object {
        private const val TAG = "WordViewModel"

        fun bonusBatchSizeForGoal(dailyGoal: Int): Int =
            dailyGoal.coerceIn(BONUS_BATCH_MIN, BONUS_BATCH_MAX)

        private const val BONUS_BATCH_MIN = 5
        private const val BONUS_BATCH_MAX = 15
    }
}

sealed class LearnUiState {
    data object Loading : LearnUiState()
    data class Success(
        val word: Word,
        /** 1-based position among unique words in today's queue. */
        val uniqueIndex: Int,
        val uniqueTotal: Int,
        /** Includes in-session retries. */
        val cardIndex: Int,
        val cardTotal: Int,
    ) : LearnUiState()

    data object EmptyNoWords : LearnUiState()
    data object EmptyAllCaughtUp : LearnUiState()
}

class WordViewModelFactory(
    private val repository: WordRepository,
    private val vocabSetRepository: VocabSetRepository,
    private val studySessionRepository: StudySessionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WordViewModel(
                repository,
                vocabSetRepository,
                studySessionRepository,
                userPreferencesRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
