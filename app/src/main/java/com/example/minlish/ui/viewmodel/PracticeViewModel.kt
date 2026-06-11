package com.example.minlish.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.minlish.data.model.VocabularySet
import com.example.minlish.data.model.Word
import com.example.minlish.data.preferences.UserPreferencesRepository
import com.example.minlish.data.repository.StudySessionRepository
import com.example.minlish.data.repository.VocabSetRepository
import com.example.minlish.data.repository.WordRepository
import com.example.minlish.logic.SrsEngine
import com.example.minlish.logic.StudySessionRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
)

/**
 * ViewModel for the Practice screen.
 *
 * Practice only shows words that have been studied at least once (lastReviewed IS NOT NULL).
 * The word list depends on the active vocabulary set, which can be switched via the dropdown.
 *
 * Three sub-modes share the same word list [_words]:
 * - Flashcard: browse words and rate via SRS buttons
 * - Quiz: multiple-choice meaning test, prioritizes due words with randomization
 * - Typing: type the English word, prioritizes due words with randomization
 */
class PracticeViewModel(
    private val wordRepository: WordRepository,
    private val vocabSetRepository: VocabSetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val studySessionRepository: StudySessionRepository,
) : ViewModel() {

    private val sessionRecorder = StudySessionRecorder(studySessionRepository, viewModelScope)

    // ── Set selector ──────────────────────────────────────────────

    /** All available vocabulary sets for the dropdown. */
    val availableSets: StateFlow<List<VocabularySet>> =
        vocabSetRepository.getAllSets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The set currently selected in the dropdown (initially follows the active set from preferences). */
    private val _selectedSetId = MutableStateFlow<Long?>(null)
    val selectedSetId: StateFlow<Long?> = _selectedSetId.asStateFlow()

    /** Human-readable name of the currently selected set. */
    val currentSetName: StateFlow<String?> =
        _selectedSetId.map { setId ->
            if (setId == null) return@map null
            availableSets.value.firstOrNull { it.id == setId }?.title
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Switch the practice session to a different vocabulary set. */
    fun switchPracticeSet(setId: Long) {
        if (_selectedSetId.value == setId) return
        _selectedSetId.value = setId
        // Persist so other screens (Learn, etc.) also pick up the change.
        viewModelScope.launch {
            userPreferencesRepository.setActiveSetId(setId)
        }
    }

    // ── Word list & flashcard ─────────────────────────────────────

    private val _words = MutableStateFlow<List<Word>>(emptyList())
    private var flashcardIndex = 0
    private val _isReviewSaving = MutableStateFlow(false)

    private val _flashcardUiState = MutableStateFlow(FlashcardUiState())
    val flashcardUiState: StateFlow<FlashcardUiState> = _flashcardUiState.asStateFlow()

    // ── Quiz ──────────────────────────────────────────────────────

    private val _quizState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val quizState: StateFlow<QuizUiState> = _quizState.asStateFlow()
    private var currentQuizTarget: Word? = null
    /** Track last shown quiz word to avoid immediate repetition. */
    private var lastQuizWordId: Long? = null

    // ── Typing ────────────────────────────────────────────────────

    private val _typingState = MutableStateFlow(
        TypingUiState(targetWord = null, userInput = "", attemptsLeft = 3),
    )
    val typingState: StateFlow<TypingUiState> = _typingState.asStateFlow()
    /** Track last shown typing word to avoid immediate repetition. */
    private var lastTypingWordId: Long? = null

    // ── Init: observe active set -> load words -> populate UI states ──

    init {
        // Initialize _selectedSetId from preferences, then follow changes.
        viewModelScope.launch {
            userPreferencesRepository.activeSetId
                .distinctUntilChanged()
                .collectLatest { activeSetId ->
                    val setId =
                        if (activeSetId > 0L) activeSetId else vocabSetRepository.getOrCreateDefaultSetId()
                    // Only update dropdown if user hasn't manually switched yet.
                    if (_selectedSetId.value == null) {
                        _selectedSetId.value = setId
                    }
                    loadWordsForSet(setId)
                }
        }
    }

    /**
     * Load studied words for the given set and refresh all sub-mode UI states.
     *
     * Flow: setId -> DAO query (lastReviewed IS NOT NULL) -> update _words -> reset indexes.
     */
    private fun loadWordsForSet(setId: Long) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            wordRepository.getPracticeWords(setId, now).collectLatest { list ->
                _words.value = list
                flashcardIndex = 0
                lastQuizWordId = null
                lastTypingWordId = null
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
                        targetWord = null, userInput = "", attemptsLeft = 3,
                    )
                }
            }
        }
    }

    // ── Flashcard ─────────────────────────────────────────────────

    fun previousFlashcard() {
        val list = _words.value
        if (list.isEmpty()) { publishFlashcardUiState(); return }
        flashcardIndex = if (flashcardIndex <= 0) list.size - 1 else flashcardIndex - 1
        publishFlashcardUiState()
    }

    fun nextFlashcard() {
        val list = _words.value
        if (list.isEmpty()) { publishFlashcardUiState(); return }
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

    // ── Quiz ──────────────────────────────────────────────────────

    fun nextQuiz() {
        val list = _words.value
        if (list.isEmpty()) {
            _quizState.value = QuizUiState.Empty
            currentQuizTarget = null
            return
        }

        val now = System.currentTimeMillis()
        val target = pickNextDueWordWithRandom(list, now, lastQuizWordId)
        currentQuizTarget = target
        lastQuizWordId = target?.id

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

    // ── Typing ────────────────────────────────────────────────────

    fun nextTyping() {
        val list = _words.value
        if (list.isEmpty()) {
            _typingState.value = TypingUiState(targetWord = null, userInput = "", attemptsLeft = 3)
            return
        }

        val now = System.currentTimeMillis()
        val target = pickNextDueWordWithRandom(list, now, lastTypingWordId)
        lastTypingWordId = target?.id

        if (target == null) {
            _typingState.value = TypingUiState(targetWord = null, userInput = "", attemptsLeft = 3)
            return
        }

        _typingState.value = TypingUiState(
            targetWord = target, userInput = "", attemptsLeft = 3,
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
                feedbackKind = TypingFeedbackKind.Correct, isCorrect = true,
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
                feedbackKind = if (remaining == 0) TypingFeedbackKind.OutOfAttempts else TypingFeedbackKind.TryAgain,
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

    // ── Helpers ───────────────────────────────────────────────────

    /**
     * Aggregate current word list, flashcard index, review-in-progress flag, and TTS state
     * into a single [FlashcardUiState] for the UI to collect.
     */
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
        )
    }

    override fun onCleared() {
        sessionRecorder.flushIfActive()
        super.onCleared()
    }
}

/**
 * Pick the next word for Quiz or Typing using SRS priority + randomization.
 *
 * Strategy:
 * 1. Due words (nextReviewDate <= now) are prioritized.
 * 2. Among due words, pick randomly (excluding [excludeId] to avoid immediate repetition).
 * 3. If no due words, pick randomly from all words (excluding [excludeId]).
 * 4. If only 1 word remains (the excluded one itself), return it anyway.
 */
private fun pickNextDueWordWithRandom(
    words: List<Word>,
    now: Long,
    excludeId: Long?,
): Word? {
    if (words.isEmpty()) return null

    val dueWords = words.filter {
        it.repetitions > 0 && it.nextReviewDate <= now
    }

    val candidates = if (dueWords.isNotEmpty()) {
        dueWords.filter { it.id != excludeId }.ifEmpty { dueWords }
    } else {
        words.filter { it.id != excludeId }.ifEmpty { words }
    }

    return candidates.random(kotlin.random.Random(System.nanoTime()))
}

class PracticeViewModelFactory(
    private val wordRepository: WordRepository,
    private val vocabSetRepository: VocabSetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val studySessionRepository: StudySessionRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PracticeViewModel(
                wordRepository,
                vocabSetRepository,
                userPreferencesRepository,
                studySessionRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
