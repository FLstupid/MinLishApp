package com.example.minlish.logic

import com.example.minlish.data.model.Word

object WordNorm {
    fun normalize(raw: String): String = raw.trim().lowercase()

    fun withNorm(word: Word): Word {
        val norm = normalize(word.word)
        return if (word.wordNorm == norm) word else word.copy(wordNorm = norm)
    }
}
