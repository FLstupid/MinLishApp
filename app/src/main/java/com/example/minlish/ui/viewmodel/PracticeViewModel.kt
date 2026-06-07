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
import com.example.minlish.logic.TtsManager
import com.example.minlish.logic.TtsState
import com.example.minlish.logic.TtsUiEvent
import com.example.minlish.logic.UnavailableReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class QuizOption(val wordId: Long, val meaning: String)

sealed class QuizUiState {
    data object Loading : QuizUiState()
    data object Empty : QuizUiState()
    data class Question(
        val promptWord: String,
        val promptPronunciation: String?,
        val correctWordId: Long,
        val options: List<QuizOption>,
        val example: String?,
        val feedback: Boolean? = null,
    ) : QuizUiState()
}

data class TypingUiState(
    val targetWord: Word?,
    val userInput: String,
    val attemptsLeft: Int,
    val feedbackKind: TypingFeedbackKind? = null,
    val isCorrect: Boolean = false,
)

enum class TypingFeedbackKind {
    TryAgain,
    OutOfAttempts,
    Correct,
}

data class FlashcardUiState(
    val word: Word? = null,
    val cardOneBased: Int = 0,
    val cardTotal: Int = 0,
    val canNavigate: Boolean = false,
    val isReviewSaving: Boolean = false,
    val speakEnabled: Boolean = false,
    val speakLoading: Boolean = false,
)

class PracticeViewModel(
    private val wordRepository: WordRepository,
    private val vocabSetRepository: VocabSetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val studySessionRepository: StudySessionRepository,
    private val ttsManager: TtsManager,
) : ViewModel() {

    private val sessionRecorder = StudySessionRecorder(studySessionRepository, viewModelScope)

    private val _words = MutableStateFlow<List<Word>>(emptyList())

    private var flashcardIndex = 0

    private val _isReviewSaving = MutableStateFlow(false)

    private val _flashcardUiState = MutableStateFlow(FlashcardUiState())
    val flashcardUiState: StateFlow<FlashcardUiState> = _flashcardUiState.asStateFlow()

    val uiEvents: SharedFlow<TtsUiEvent> = ttsManager.uiEvents

    private var lastTtsState: TtsState = TtsState.Initializing

    private val _quizState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val quizState: StateFlow<QuizUiState> = _quizState.asStateFlow()

    private val _typingState = MutableStateFlow(
        TypingUiState(
            targetWord = null,
            userInput = "",
            attemptsLeft = 3,
        )
    )
    val typingState: StateFlow<TypingUiState> = _typingState.asStateFlow()

    private var currentQuizTarget: Word? = null

    init {
        viewModelScope.launch {
            ttsManager.state.collect { state ->
                    lastTtsState = state
                    publishFlashcardUiState()
                }
        }

        viewModelScope.launch {
            userPreferencesRepository.activeSetId
                .distinctUntilChanged()
                .collectLatest { activeSetId ->
                    val setId =
                        if (activeSetId > 0L) activeSetId else vocabSetRepository.getOrCreateDefaultSetId()
                    val now = System.currentTimeMillis()
                    wordRepository.getPracticeWords(setId, now).collectLatest { list ->
                        _words.value = list
                        flashcardIndex = 0
                        publishFlashcardUiState()
                        if (list.isNotEmpty() && _quizState.value is QuizUiState.Loading) {
                            nextQuiz()
                        } else if (list.isEmpty()) {
                            _quizState.value = QuizUiState.Empty
                        }
                        if (_typingState.value.targetWord == null && list.isNotEmpty()) {
                            nextTyping()
                        } else if (list.isEmpty()) {
                            _typingState.value = TypingUiState(
                                targetWord = null,
                                userInput = "",
                                attemptsLeft = 3,
                                feedbackKind = null,
                                isCorrect = false,
                            )
                        }
                    }
                }
        }
    }

    fun onSpeakFlashcardClicked() {
        val word = _flashcardUiState.value.word ?: return
        ttsManager.speakEnglishWord(word)
    }

    fun previousFlashcard() {
        val list = _words.value
        if (list.isEmpty()) {
            publishFlashcardUiState()
            return
        }
        flashcardIndex = if (flashcardIndex <= 0) list.size - 1 else flashcardIndex - 1
        publishFlashcardUiState()
    }

    fun nextFlashcard() {
        val list = _words.value
        if (list.isEmpty()) {
            publishFlashcardUiState()
            return
        }
        flashcardIndex = (flashcardIndex + 1) % list.size
        publishFlashcardUiState()
    }

    fun onPracticeReview(quality: Int) {
        val list = _words.value
        if (list.isEmpty() || flashcardIndex !in list.indices) return
        val word = list[flashcardIndex]
        if (_isReviewSaving.value) return
        _isReviewSaving.value = true
        publishFlashcardUiState()
        val updated = SrsEngine.calculateNextReview(word, quality)
        viewModelScope.launch {
            try {
                wordRepository.updateWordLocal(updated)
                sessionRecorder.recordAnswer(quality, objective = false)
                launch { wordRepository.syncWordToCloud(updated) }
            } finally {
                _isReviewSaving.value = false
                publishFlashcardUiState()
            }
        }
    }

    fun nextQuiz() {
        val list = _words.value
        if (list.isEmpty()) {
            _quizState.value = QuizUiState.Empty
            currentQuizTarget = null
            return
        }

        val now = System.currentTimeMillis()
        val target = list.firstDueOrFirst(now)
        currentQuizTarget = target
        if (target == null) {
            _quizState.value = QuizUiState.Empty
            return
        }

        val distractors = list
            .filter { it.id != target.id }
            .shuffled(kotlin.random.Random(System.nanoTime()))
            .take(3)

        val optionWords = (listOf(target) + distractors).shuffled(kotlin.random.Random(System.nanoTime()))

        val options = optionWords.map { QuizOption(wordId = it.id, meaning = it.meaning) }
        _quizState.value = QuizUiState.Question(
            promptWord = target.word,
            promptPronunciation = target.pronunciation,
            correctWordId = target.id,
            options = options,
            example = target.example,
            feedback = null,
        )
    }

    fun submitQuizAnswer(selectedWordId: Long) {
        val state = _quizState.value
        if (state !is QuizUiState.Question) return

        val isCorrect = selectedWordId == state.correctWordId
        _quizState.value = state.copy(feedback = isCorrect)

        val target = currentQuizTarget
        if (target != null) {
            val quality = if (isCorrect) 4 else 0
            viewModelScope.launch {
                val updated = SrsEngine.calculateNextReview(target, quality)
                wordRepository.updateWordLocal(updated)
                sessionRecorder.recordAnswer(quality, objective = true)
                launch { wordRepository.syncWordToCloud(updated) }
            }
        }
    }

    fun nextTyping() {
        val list = _words.value
        val now = System.currentTimeMillis()
        val target = list.firstDueOrFirst(now)
        if (target == null) {
            _typingState.value = TypingUiState(
                targetWord = null,
                userInput = "",
                attemptsLeft = 3,
                feedbackKind = null,
                isCorrect = false,
            )
            return
        }

        _typingState.value = TypingUiState(
            targetWord = target,
            userInput = "",
            attemptsLeft = 3,
            feedbackKind = null,
            isCorrect = false,
        )
    }

    fun onTypingInputChanged(text: String) {
        _typingState.value = _typingState.value.copy(userInput = text)
    }

    fun submitTypingAnswer() {
        val state = _typingState.value
        val target = state.targetWord ?: return

        if (state.isCorrect) return

        val normalizedInput = state.userInput.trim().lowercase()
        val normalizedTarget = target.word.trim().lowercase()

        val isCorrect = normalizedInput == normalizedTarget
        if (isCorrect) {
            _typingState.value = state.copy(
                feedbackKind = TypingFeedbackKind.Correct,
                isCorrect = true,
            )
            viewModelScope.launch {
                val updated = SrsEngine.calculateNextReview(target, 4)
                wordRepository.updateWordLocal(updated)
                sessionRecorder.recordAnswer(4, objective = true)
                launch { wordRepository.syncWordToCloud(updated) }
            }
        } else {
            val remaining = (state.attemptsLeft - 1).coerceAtLeast(0)
            _typingState.value = state.copy(
                userInput = "",
                attemptsLeft = remaining,
                feedbackKind = if (remaining == 0) {
                    TypingFeedbackKind.OutOfAttempts
                } else {
                    TypingFeedbackKind.TryAgain
                },
                isCorrect = false,
            )
            if (remaining == 0) {
                viewModelScope.launch {
                    val updated = SrsEngine.calculateNextReview(target, 0)
                    wordRepository.updateWordLocal(updated)
                    sessionRecorder.recordAnswer(0, objective = true)
                    launch { wordRepository.syncWordToCloud(updated) }
                }
            }
        }
    }

    private fun publishFlashcardUiState() {
        val list = _words.value
        val word = list.getOrNull(flashcardIndex)
        val total = list.size
        _flashcardUiState.value = FlashcardUiState(
            word = word,
            cardOneBased = if (word != null && total > 0) flashcardIndex + 1 else 0,
            cardTotal = total,
            canNavigate = total > 1,
            isReviewSaving = _isReviewSaving.value,
            speakEnabled = when (val s = lastTtsState) {
                is TtsState.Unavailable -> s.reason != UnavailableReason.InitFailed
                else -> true
            },
            speakLoading = lastTtsState is TtsState.Initializing,
        )
    }

    override fun onCleared() {
        sessionRecorder.flushIfActive()
        super.onCleared()
    }
}

private fun List<Word>.firstDueOrFirst(now: Long): Word? {
    if (isEmpty()) return null
    return firstOrNull { it.repetitions > 0 && it.nextReviewDate <= now } ?: first()
}

class PracticeViewModelFactory(
    private val wordRepository: WordRepository,
    private val vocabSetRepository: VocabSetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val studySessionRepository: StudySessionRepository,
    private val ttsManager: TtsManager,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PracticeViewModel(
                wordRepository,
                vocabSetRepository,
                userPreferencesRepository,
                studySessionRepository,
                ttsManager,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
