package com.example.minlish.data.dao

import androidx.room.*
import com.example.minlish.data.model.VocabularySet
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularySetDao {
    @Query("SELECT * FROM vocabulary_sets")
    fun getAllSets(): Flow<List<VocabularySet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: VocabularySet): Long

    @Update
    suspend fun updateSet(set: VocabularySet)

    @Delete
    suspend fun deleteSet(set: VocabularySet)
}
