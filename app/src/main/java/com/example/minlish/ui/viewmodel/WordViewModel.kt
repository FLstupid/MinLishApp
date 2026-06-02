package com.example.minlish.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.minlish.data.model.Word
import com.example.minlish.data.repository.WordRepository
import com.example.minlish.logic.SrsEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WordViewModel(private val repository: WordRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LearnUiState>(LearnUiState.Loading)
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    private val _reviewCount = MutableStateFlow(0)
    val reviewCount: StateFlow<Int> = _reviewCount.asStateFlow()

    private val _learnedCount = MutableStateFlow(0)
    val learnedCount: StateFlow<Int> = _learnedCount.asStateFlow()

    private var currentWords = listOf<Word>()
    private var currentIndex = 0

    init {
        loadWordsToReview()
        updateStats()
    }

    private fun loadWordsToReview() {
        viewModelScope.launch {
            repository.getWordsToReview(System.currentTimeMillis())
                .collect { words ->
                    _reviewCount.value = words.size
                    if (words.isEmpty()) {
                        _uiState.value = LearnUiState.Empty
                    } else {
                        currentWords = words.shuffled()
                        currentIndex = 0
                        _uiState.value = LearnUiState.Success(currentWords[currentIndex])
                    }
                }
        }
    }

    private fun updateStats() {
        viewModelScope.launch {
            repository.getWordsBySet(1).collect { words ->
                _learnedCount.value = words.size
            }
        }
    }

    fun onAnswer(quality: Int) {
        val state = _uiState.value
        if (state is LearnUiState.Success) {
            val updatedWord = SrsEngine.calculateNextReview(state.word, quality)
            viewModelScope.launch {
                repository.updateWord(updatedWord)
                showNextWord()
            }
        }
    }

    private fun showNextWord() {
        currentIndex++
        if (currentIndex < currentWords.size) {
            _uiState.value = LearnUiState.Success(currentWords[currentIndex])
        } else {
            _uiState.value = LearnUiState.Empty
        }
    }

    fun addNewWord(wordText: String, meaning: String, pronunciation: String?, example: String?) {
        viewModelScope.launch {
            val newWord = Word(
                word = wordText,
                meaning = meaning,
                pronunciation = pronunciation,
                example = example,
                setId = 1 // Default set
            )
            repository.insertWord(newWord)
        }
    }

    fun importCsv(csvContent: String) {
        viewModelScope.launch {
            val lines = csvContent.lines()
            val wordsToInsert = mutableListOf<Word>()
            for (line in lines) {
                if (line.isBlank()) continue
                val parts = line.split(",")
                if (parts.size >= 2) {
                    wordsToInsert.add(
                        Word(
                            word = parts[0].trim(),
                            meaning = parts[1].trim(),
                            pronunciation = parts.getOrNull(2)?.trim(),
                            example = parts.getOrNull(3)?.trim(),
                            setId = 1
                        )
                    )
                }
            }
            if (wordsToInsert.isNotEmpty()) {
                repository.insertWords(wordsToInsert)
            }
        }
    }

    fun addDummyData() {
        viewModelScope.launch {
            repository.insertWord(Word(word = "Persistence", meaning = "Sự kiên trì", setId = 1))
            repository.insertWord(Word(word = "Resilience", meaning = "Khả năng phục hồi", setId = 1))
            repository.insertWord(Word(word = "Ambition", meaning = "Tham vọng", setId = 1))
        }
    }
}

sealed class LearnUiState {
    data object Loading : LearnUiState()
    data class Success(val word: Word) : LearnUiState()
    data object Empty : LearnUiState()
}

class WordViewModelFactory(private val repository: WordRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WordViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
