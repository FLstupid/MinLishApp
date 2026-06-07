package com.example.minlish.data.dao

import androidx.room.*
import com.example.minlish.data.model.VocabularySet
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularySetDao {
    @Query("SELECT * FROM vocabulary_sets ORDER BY createdAt ASC, id ASC")
    fun getAllSets(): Flow<List<VocabularySet>>

    @Query("SELECT * FROM vocabulary_sets WHERE id = :setId LIMIT 1")
    suspend fun getSetById(setId: Long): VocabularySet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: VocabularySet): Long

    @Update
    suspend fun updateSet(set: VocabularySet)

    @Delete
    suspend fun deleteSet(set: VocabularySet)
}
