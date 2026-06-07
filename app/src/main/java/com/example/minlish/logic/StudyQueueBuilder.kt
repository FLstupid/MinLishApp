package com.example.minlish.logic

import com.example.minlish.data.model.Word

/**
 * Builds the daily Learn queue: due reviews first, then pending failed introductions, then new words.
 */
object StudyQueueBuilder {

    fun buildDailyQueue(
        dueWords: List<Word>,
        pendingIntroductionRetries: List<Word>,
        introduceWords: List<Word>,
    ): List<Word> {
        val seenLemma = mutableSetOf<String>()
        val result = mutableListOf<Word>()
        for (word in dueWords + pendingIntroductionRetries + introduceWords) {
            val lemma = word.wordNorm.ifBlank { WordNorm.normalize(word.word) }
            if (lemma.isBlank()) continue
            if (seenLemma.add(lemma)) {
                result.add(word)
            }
        }
        return result
    }
}
