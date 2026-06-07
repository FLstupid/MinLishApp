package com.example.minlish.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.minlish.data.model.Word
import com.example.minlish.data.preferences.UserPreferencesRepository
import com.example.minlish.data.repository.StudySessionRepository
import com.example.minlish.data.repository.VocabSetRepository
import com.example.minlish.data.repository.WordRepository
import com.example.minlish.logic.SrsEngine
import com.example.minlish.logic.StudySessionRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class CheckpointUiState {
    data object Loading : CheckpointUiState()
    data object NotEnoughWords : CheckpointUiState()
    data class Question(
        val index: Int,
        val total: Int,
        val promptWord: String,
        val promptPronunciation: String?,
        val correctWordId: Long,
        val options: List<QuizOption>,
        val example: String?,
        val feedback: Boolean? = null,
    ) : CheckpointUiState()

    data class Finished(
        val correct: Int,
        val total: Int,
    ) : CheckpointUiState()
}

class CheckpointViewModel(
    private val wordRepository: WordRepository,
    private val vocabSetRepository: VocabSetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val studySessionRepository: StudySessionRepository,
) : ViewModel() {

    private val sessionRecorder = StudySessionRecorder(studySessionRepository, viewModelScope)

    private val _uiState = MutableStateFlow<CheckpointUiState>(CheckpointUiState.Loading)
    val uiState: StateFlow<CheckpointUiState> = _uiState.asStateFlow()

    private var pool: List<Word> = emptyList()
    private var questionWords: List<Word> = emptyList()
    private var questionIndex = 0
    private var correctAnswers = 0
    private var currentTarget: Word? = null

    fun startSession() {
        _uiState.value = CheckpointUiState.Loading
        viewModelScope.launch {
            val activeSetId = userPreferencesRepository.activeSetId.first()
            val setId =
                if (activeSetId > 0L) activeSetId else vocabSetRepository.getOrCreateDefaultSetId()
            pool = wordRepository.getWordsBySet(setId).first()
                .filter { it.lastReviewed != null }

            val studied = pool.shuffled()
            if (studied.size < MIN_STUDIED_WORDS) {
                _uiState.value = CheckpointUiState.NotEnoughWords
                return@launch
            }
            questionWords = studied.take(QUESTION_COUNT.coerceAtMost(studied.size))
            questionIndex = 0
            correctAnswers = 0
            showQuestionAt(0)
        }
    }

    fun submitAnswer(selectedWordId: Long) {
        val state = _uiState.value
        if (state !is CheckpointUiState.Question || state.feedback != null) return

        val isCorrect = selectedWordId == state.correctWordId
        if (isCorrect) correctAnswers += 1
        _uiState.value = state.copy(feedback = isCorrect)

        val target = currentTarget
        if (target != null) {
            viewModelScope.launch {
                val quality = if (isCorrect) 4 else 0
                val updated = SrsEngine.calculateNextReview(target, quality)
                wordRepository.updateWordLocal(updated)
                sessionRecorder.recordAnswer(quality, objective = true)
                launch { wordRepository.syncWordToCloud(updated) }
            }
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state !is CheckpointUiState.Question || state.feedback == null) return

        val next = questionIndex + 1
        if (next >= questionWords.size) {
            sessionRecorder.flushIfActive()
            _uiState.value = CheckpointUiState.Finished(
                correct = correctAnswers,
                total = questionWords.size,
            )
            return
        }
        showQuestionAt(next)
    }

    fun dismissFinished() {
        _uiState.value = CheckpointUiState.Loading
    }

    override fun onCleared() {
        sessionRecorder.flushIfActive()
        super.onCleared()
    }

    private fun showQuestionAt(index: Int) {
        questionIndex = index
        val target = questionWords[index]
        currentTarget = target

        val distractors = pool
            .filter { it.id != target.id }
            .shuffled()
            .take(3)
        val optionWords = (listOf(target) + distractors).shuffled()
        val options = optionWords.map { QuizOption(wordId = it.id, meaning = it.meaning) }

        _uiState.value = CheckpointUiState.Question(
            index = index + 1,
            total = questionWords.size,
            promptWord = target.word,
            promptPronunciation = target.pronunciation,
            correctWordId = target.id,
            options = options,
            example = target.example,
            feedback = null,
        )
    }

    companion object {
        const val MIN_STUDIED_WORDS = 5
        private const val QUESTION_COUNT = 5
    }
}

class CheckpointViewModelFactory(
    private val wordRepository: WordRepository,
    private val vocabSetRepository: VocabSetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val studySessionRepository: StudySessionRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CheckpointViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CheckpointViewModel(
                wordRepository,
                vocabSetRepository,
                userPreferencesRepository,
                studySessionRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
