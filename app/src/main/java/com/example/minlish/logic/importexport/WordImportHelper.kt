package com.example.minlish.logic.importexport

import com.example.minlish.data.repository.WordRepository
import com.example.minlish.logic.WordNorm

object WordImportHelper {

    data class ImportStats(
        val total: Int,
        val inserted: Int,
        val skipped: Int,
        val merged: Int,
        val errors: Int,
    )

    suspend fun importRowsIntoSet(
        wordRepository: WordRepository,
        setId: Long,
        rows: List<VocabIO.VocabRow>,
    ): ImportStats {
        val toInsert = mutableListOf<com.example.minlish.data.model.Word>()
        val pendingKeys = mutableSetOf<String>()
        var skipped = 0
        var merged = 0

        for (row in rows) {
            val normalizedWord = row.word.trim()
            if (normalizedWord.isBlank() || row.meaning.trim().isBlank()) {
                skipped += 1
                continue
            }

            val normKey = WordNorm.normalize(normalizedWord)
            val pendingKey = "$setId:$normKey"

            val existing = wordRepository.findInSet(setId = setId, word = normalizedWord)
            if (existing != null) {
                if (VocabIO.rowHasSrsFields(row)) {
                    val updated = VocabIO.mergeRowSrs(existing, row)
                    if (updated != existing) {
                        wordRepository.updateWordLocal(updated)
                        wordRepository.syncWordToCloud(updated)
                        merged += 1
                    } else {
                        skipped += 1
                    }
                } else {
                    skipped += 1
                }
                pendingKeys.add(pendingKey)
                continue
            }

            if (!pendingKeys.add(pendingKey)) {
                skipped += 1
                continue
            }

            toInsert.add(VocabIO.wordFromRow(row, setId = setId))
        }

        var errors = 0
        if (toInsert.isNotEmpty()) {
            try {
                wordRepository.insertWords(toInsert)
            } catch (_: Throwable) {
                errors = toInsert.size
                return ImportStats(
                    total = rows.size,
                    inserted = 0,
                    skipped = skipped,
                    merged = merged,
                    errors = errors,
                )
            }
        }

        return ImportStats(
            total = rows.size,
            inserted = toInsert.size,
            skipped = skipped,
            merged = merged,
            errors = errors,
        )
    }
}
