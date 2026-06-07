package com.example.minlish.logic.starter

import android.content.Context
import com.example.minlish.data.preferences.UserPreferencesRepository
import com.example.minlish.data.repository.VocabSetRepository
import com.example.minlish.data.repository.WordRepository
import com.example.minlish.logic.importexport.VocabIO
import com.example.minlish.logic.importexport.WordImportHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StarterPackInstaller(
    private val context: Context,
    private val vocabSetRepository: VocabSetRepository,
    private val wordRepository: WordRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    data class InstallResult(
        val setId: Long,
        val stats: WordImportHelper.ImportStats,
    )

    sealed class InstallOutcome {
        data class Success(val result: InstallResult) : InstallOutcome()
        data object AlreadyInstalled : InstallOutcome()
        data object PackNotFound : InstallOutcome()
    }

    fun starterPacks(): List<StarterPackDefinition> = StarterPackCatalog.loadPacks(context)

    val installedPackIds: Flow<Set<String>> =
        userPreferencesRepository.installedStarterPackIds

    suspend fun isPackInstalled(packId: String): Boolean =
        userPreferencesRepository.isStarterPackInstalled(packId)

    suspend fun installPack(packId: String, setActive: Boolean): InstallOutcome {
        if (userPreferencesRepository.isStarterPackInstalled(packId)) {
            return InstallOutcome.AlreadyInstalled
        }
        val pack = starterPacks().firstOrNull { it.id == packId } ?: return InstallOutcome.PackNotFound

        val csv = StarterPackCatalog.loadCsv(context, pack.fileName)
        val rows = VocabIO.parseCsv(csv)
        if (rows.isEmpty()) return InstallOutcome.PackNotFound

        val setId = vocabSetRepository.insertSet(
            title = pack.title,
            description = pack.description,
            tags = pack.tags,
        )
        val stats = WordImportHelper.importRowsIntoSet(
            wordRepository = wordRepository,
            setId = setId,
            rows = rows,
        )
        userPreferencesRepository.markStarterPackInstalled(packId)
        if (setActive) {
            userPreferencesRepository.setActiveSetId(setId)
        }
        return InstallOutcome.Success(InstallResult(setId = setId, stats = stats))
    }

    /**
     * Installs the auto-install pack once when the local DB has no words yet.
     */
    suspend fun runAutoSeedIfNeeded() {
        val autoPack = starterPacks().firstOrNull { it.autoInstall } ?: run {
            if (!userPreferencesRepository.isStarterAutoInstalled()) {
                userPreferencesRepository.setStarterAutoInstalled()
            }
            return
        }

        if (userPreferencesRepository.isStarterPackInstalled(autoPack.id)) {
            if (!userPreferencesRepository.isStarterAutoInstalled()) {
                userPreferencesRepository.setStarterAutoInstalled()
            }
            return
        }

        when (installPack(autoPack.id, setActive = true)) {
            is InstallOutcome.Success -> {
                userPreferencesRepository.setStarterAutoInstalled()
                vocabSetRepository.deleteEmptyDefaultSets()
            }
            is InstallOutcome.AlreadyInstalled -> {
                userPreferencesRepository.setStarterAutoInstalled()
                vocabSetRepository.deleteEmptyDefaultSets()
            }
            is InstallOutcome.PackNotFound -> {
                val wordCount = wordRepository.countAllWords()
                if (wordCount > 0 && !userPreferencesRepository.isStarterAutoInstalled()) {
                    userPreferencesRepository.setStarterAutoInstalled()
                }
            }
        }
    }
}
