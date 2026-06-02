package com.example.minlish.data.repository

import com.example.minlish.data.dao.WordDao
import com.example.minlish.data.model.Word
import kotlinx.coroutines.flow.Flow

class WordRepository(private val wordDao: WordDao) {
    fun getWordsBySet(setId: Long): Flow<List<Word>> = wordDao.getWordsBySet(setId)

    fun getWordsToReview(currentTime: Long): Flow<List<Word>> = wordDao.getWordsToReview(currentTime)

    suspend fun insertWord(word: Word) = wordDao.insertWord(word)

    suspend fun insertWords(words: List<Word>) = wordDao.insertWords(words)

    suspend fun updateWord(word: Word) = wordDao.updateWord(word)

    suspend fun deleteWord(word: Word) = wordDao.deleteWord(word)
}
