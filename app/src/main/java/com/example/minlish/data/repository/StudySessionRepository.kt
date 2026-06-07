package com.example.minlish.data.repository

import com.example.minlish.data.dao.StudySessionDao
import com.example.minlish.data.model.StudySession
import kotlinx.coroutines.flow.Flow

class StudySessionRepository(
    private val studySessionDao: StudySessionDao,
) {
    suspend fun insert(session: StudySession) = studySessionDao.insertSession(session)

    fun getAllSessions(): Flow<List<StudySession>> = studySessionDao.getAllSessions()
}

