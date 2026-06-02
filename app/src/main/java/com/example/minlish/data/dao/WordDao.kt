package com.example.minlish.data.dao

import androidx.room.*
import com.example.minlish.data.model.Word
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE setId = :setId")
    fun getWordsBySet(setId: Long): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE nextReviewDate <= :currentTime")
    fun getWordsToReview(currentTime: Long): Flow<List<Word>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<Word>)

    @Update
    suspend fun updateWord(word: Word)

    @Delete
    suspend fun deleteWord(word: Word)
}
