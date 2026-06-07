package com.example.minlish.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.minlish.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Insert
    suspend fun insertSession(session: StudySession)

    @Query("SELECT * FROM study_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<StudySession>>
}

