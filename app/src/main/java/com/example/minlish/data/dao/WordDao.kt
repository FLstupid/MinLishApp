package com.example.minlish.data.dao

import androidx.room.*
import com.example.minlish.data.model.Word
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE setId = :setId")
    fun getWordsBySet(setId: Long): Flow<List<Word>>

    /** Words that have been studied at least once and are due for review (Learn queue, dashboard, notifications). */
    @Query("SELECT * FROM words WHERE nextReviewDate <= :currentTime AND repetitions > 0")
    fun getDueReviewWordsAnySet(currentTime: Long): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE setId = :setId AND nextReviewDate <= :currentTime AND repetitions > 0")
    fun getDueReviewWords(setId: Long, currentTime: Long): Flow<List<Word>>

    @Query(
        """
        SELECT w.* FROM words w
        INNER JOIN (
            SELECT MIN(id) AS id FROM words
            WHERE setId = :setId
              AND repetitions = 0
              AND lastReviewed IS NULL
              AND (cefrLevel IS NULL OR cefrLevel IN (:allowedLevels))
            GROUP BY wordNorm
        ) picks ON w.id = picks.id
        ORDER BY w.id ASC
        LIMIT :limit
        """
    )
    suspend fun getIntroduceWords(
        setId: Long,
        allowedLevels: List<String>,
        limit: Int,
    ): List<Word>

    @Query(
        """
        SELECT COUNT(DISTINCT wordNorm) FROM words
        WHERE setId = :setId
          AND repetitions = 0
          AND lastReviewed IS NULL
          AND (cefrLevel IS NULL OR cefrLevel IN (:allowedLevels))
        """
    )
    suspend fun countIntroducePool(
        setId: Long,
        allowedLevels: List<String>,
    ): Int

    @Query(
        """
        SELECT w.* FROM words w
        INNER JOIN (
            SELECT MIN(id) AS id FROM words
            WHERE setId = :setId
              AND repetitions = 0
              AND lastReviewed IS NOT NULL
              AND nextReviewDate <= :now
              AND (cefrLevel IS NULL OR cefrLevel IN (:allowedLevels))
            GROUP BY wordNorm
        ) picks ON w.id = picks.id
        ORDER BY w.nextReviewDate ASC, w.id ASC
        """
    )
    suspend fun getPendingIntroductionRetries(
        setId: Long,
        allowedLevels: List<String>,
        now: Long,
    ): List<Word>

    @Query(
        """
        SELECT * FROM words
        WHERE setId = :setId
          AND lastReviewed IS NOT NULL
        ORDER BY
          CASE WHEN repetitions > 0 AND nextReviewDate <= :now THEN 0 ELSE 1 END,
          nextReviewDate ASC,
          id ASC
        """
    )
    fun getPracticeWords(setId: Long, now: Long): Flow<List<Word>>

    @Query("SELECT COUNT(*) FROM words WHERE setId = :setId")
    fun getWordCountForSet(setId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun countAllWords(): Int

    @Query("SELECT COUNT(*) FROM words WHERE setId = :setId")
    suspend fun countWordsInSet(setId: Long): Int

    @Query("SELECT COUNT(*) FROM words WHERE setId = :setId AND repetitions > 0")
    fun getLearnedCountForSet(setId: Long): Flow<Int>

    @Query("DELETE FROM words WHERE setId = :setId")
    suspend fun deleteWordsBySetId(setId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word): Long

    @Query(
        "SELECT EXISTS(" +
            "SELECT 1 FROM words " +
            "WHERE setId = :setId AND LOWER(word) = LOWER(:word)" +
            ")"
    )
    suspend fun existsInSet(setId: Long, word: String): Boolean

    @Query(
        "SELECT * FROM words WHERE setId = :setId AND LOWER(word) = LOWER(:word) LIMIT 1"
    )
    suspend fun findInSet(setId: Long, word: String): Word?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<Word>): List<Long>

    @Update
    suspend fun updateWord(word: Word)

    @Delete
    suspend fun deleteWord(word: Word)
}
