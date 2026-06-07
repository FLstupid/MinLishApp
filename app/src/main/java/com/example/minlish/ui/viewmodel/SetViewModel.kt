package com.example.minlish.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.minlish.data.model.VocabularySet
import com.example.minlish.data.model.Word
import com.example.minlish.logic.importexport.VocabIO
import com.example.minlish.logic.importexport.WordImportHelper
import com.example.minlish.logic.starter.StarterPackCatalog
import com.example.minlish.logic.starter.StarterPackDefinition
import com.example.minlish.logic.starter.StarterPackInstaller
import java.io.ByteArrayInputStream
import com.example.minlish.data.preferences.UserPreferencesRepository
import com.example.minlish.data.repository.VocabSetRepository
import com.example.minlish.data.repository.WordRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SetViewModel(
    private val vocabSetRepository: VocabSetRepository,
    private val wordRepository: WordRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val starterPackInstaller: StarterPackInstaller,
) : ViewModel() {

    private val _selectedSetId = MutableStateFlow<Long?>(null)
    val selectedSetId: StateFlow<Long?> = _selectedSetId.asStateFlow()

    val sets: StateFlow<List<VocabularySet>> =
        vocabSetRepository.getAllSets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val wordsInSelectedSet: StateFlow<List<Word>> =
        selectedSetId
            .filterNotNull()
            .flatMapLatest { setId ->
                wordRepository.getWordsBySet(setId)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val installedStarterPackIds: StateFlow<Set<String>> =
        starterPackInstaller.installedPackIds
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val starterPacks: List<StarterPackDefinition> = starterPackInstaller.starterPacks()

    private val _uiEvents = Channel<SetUiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    fun wordCountForSet(setId: Long): Flow<Int> = wordRepository.getWordCountForSet(setId)

    init {
        viewModelScope.launch {
            vocabSetRepository.syncFromCurrentUser()
        }
    }

    fun selectSet(setId: Long) {
        _selectedSetId.value = setId
        viewModelScope.launch {
            userPreferencesRepository.setActiveSetId(setId)
        }
    }

    fun clearSelection() {
        _selectedSetId.value = null
    }

    fun addSet(title: String, description: String?, tags: String) {
        viewModelScope.launch {
            val newId = vocabSetRepository.insertSet(title, description, tags)
            val activeId = userPreferencesRepository.activeSetId.first()
            if (activeId <= 0L) {
                userPreferencesRepository.setActiveSetId(newId)
                _selectedSetId.value = newId
            }
        }
    }

    fun deleteSet(set: VocabularySet) {
        viewModelScope.launch {
            vocabSetRepository.deleteSet(set)
        }
    }

    fun updateSet(set: VocabularySet) {
        viewModelScope.launch {
            vocabSetRepository.updateSet(set)
        }
    }

    fun addWordToSelectedSet(
        word: String,
        pronunciation: String?,
        meaning: String,
        descriptionEn: String?,
        example: String?,
        collocation: String?,
        relatedWords: String?,
        note: String?,
        onResult: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val setId = selectedSetId.value ?: run {
                onResult(false)
                return@launch
            }

            val normalizedWord = word.trim()
            val normalizedMeaning = meaning.trim()
            if (normalizedWord.isBlank() || normalizedMeaning.isBlank()) {
                _uiEvents.send(SetUiEvent.Snackbar("Vui lòng nhập từ và nghĩa."))
                onResult(false)
                return@launch
            }

            val alreadyExists = wordRepository.existsInSet(setId = setId, word = normalizedWord)
            if (alreadyExists) {
                _uiEvents.send(SetUiEvent.Snackbar("Từ đã tồn tại trong bộ này."))
                onResult(false)
                return@launch
            }

            val newWord = Word(
                word = normalizedWord,
                pronunciation = pronunciation?.trim().ifNullOrBlank { null },
                meaning = normalizedMeaning,
                descriptionEn = descriptionEn?.trim().ifNullOrBlank { null },
                example = example?.trim().ifNullOrBlank { null },
                collocation = collocation?.trim().ifNullOrBlank { null },
                relatedWords = relatedWords?.trim().ifNullOrBlank { null },
                note = note?.trim().ifNullOrBlank { null },
                setId = setId,
            )
            wordRepository.insertWord(newWord)
            vocabSetRepository.refreshWordCount(setId)
            onResult(true)
        }
    }

    fun deleteWord(word: Word) {
        viewModelScope.launch {
            wordRepository.deleteWord(word)
            vocabSetRepository.refreshWordCount(word.setId)
        }
    }

    data class ImportStats(
        val total: Int,
        val inserted: Int,
        val skipped: Int,
        val merged: Int,
        val errors: Int,
    )

    fun recommendedPackIdForGoal(goal: String): String? =
        StarterPackCatalog.recommendedPackIdForGoal(goal)

    fun importCsvText(text: String) {
        if (text.isBlank()) {
            viewModelScope.launch { _uiEvents.send(SetUiEvent.PasteImportEmpty) }
            return
        }
        val rows = VocabIO.parseCsv(text)
        importParsedRows(rows)
    }

    fun importXlsxBytes(bytes: ByteArray) {
        val rows = try {
            VocabIO.parseXlsx(ByteArrayInputStream(bytes))
        } catch (_: Throwable) {
            emptyList()
        }
        importParsedRows(rows)
    }

    fun importCsvBytes(bytes: ByteArray) {
        val text = bytes.toString(Charsets.UTF_8)
        importCsvText(text)
    }

    fun importParsedRows(rows: List<VocabIO.VocabRow>) {
        viewModelScope.launch {
            if (rows.isEmpty()) {
                _uiEvents.send(SetUiEvent.ImportFailed)
                return@launch
            }
            val setId = selectedSetId.value ?: return@launch
            val stats = WordImportHelper.importRowsIntoSet(
                wordRepository = wordRepository,
                setId = setId,
                rows = rows,
            )
            val importStats = ImportStats(
                total = stats.total,
                inserted = stats.inserted,
                skipped = stats.skipped,
                merged = stats.merged,
                errors = stats.errors,
            )
            _uiEvents.send(
                SetUiEvent.ImportCompleted(
                    stats = importStats,
                    showGoLearnCta = importStats.inserted > 0,
                ),
            )
        }
    }

    fun buildExportCsv(words: List<Word>): String = VocabIO.buildCsv(words)

    fun buildExportXlsx(words: List<Word>): ByteArray = VocabIO.buildXlsx(words)

    fun installStarterPack(packId: String) {
        viewModelScope.launch {
            when (val outcome = starterPackInstaller.installPack(packId, setActive = true)) {
                is StarterPackInstaller.InstallOutcome.Success -> {
                    val stats = outcome.result.stats
                    _uiEvents.send(
                        SetUiEvent.Snackbar("Đã thêm ${stats.inserted} từ (bỏ qua ${stats.skipped})."),
                    )
                }
                StarterPackInstaller.InstallOutcome.AlreadyInstalled -> {
                    _uiEvents.send(SetUiEvent.Snackbar("Bộ này đã được cài."))
                }
                StarterPackInstaller.InstallOutcome.PackNotFound -> {
                    _uiEvents.send(SetUiEvent.Snackbar("Không tìm thấy bộ từ."))
                }
            }
        }
    }
}

private fun String?.ifNullOrBlank(replacement: () -> String?): String? {
    val v = this?.trim()
    return if (v.isNullOrBlank()) replacement() else v
}

sealed class SetUiEvent {
    data class Snackbar(val message: String) : SetUiEvent()
    data class ImportCompleted(val stats: SetViewModel.ImportStats, val showGoLearnCta: Boolean) : SetUiEvent()
    data object ImportFailed : SetUiEvent()
    data object PasteImportEmpty : SetUiEvent()
}

class SetViewModelFactory(
    private val vocabSetRepository: VocabSetRepository,
    private val wordRepository: WordRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val starterPackInstaller: StarterPackInstaller,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SetViewModel(
                vocabSetRepository,
                wordRepository,
                userPreferencesRepository,
                starterPackInstaller,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
