package com.example.minlish.logic

import com.example.minlish.data.model.StudySession
import com.example.minlish.data.repository.StudySessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Accumulates study activity and persists a [StudySession] when flushed.
 */
class StudySessionRecorder(
    private val studySessionRepository: StudySessionRepository,
    private val scope: CoroutineScope,
) {
    private var sessionActive = false
    private var sessionStartMs = 0L
    private var wordsReviewed = 0
    private var correctCount = 0
    private var objectiveReviewed = 0
    private var objectiveCorrect = 0

    fun recordAnswer(quality: Int, objective: Boolean = false) {
        ensureSessionStarted()
        if (objective) {
            objectiveReviewed += 1
            if (quality >= 3) objectiveCorrect += 1
        } else {
            wordsReviewed += 1
            if (quality >= 3) correctCount += 1
        }
    }

    fun recordObjectiveAnswer(correct: Boolean) {
        recordAnswer(if (correct) 4 else 0, objective = true)
    }

    fun flushIfActive() {
        val hasActivity = wordsReviewed > 0 || objectiveReviewed > 0
        if (!sessionActive || !hasActivity) return
        sessionActive = false

        val durationMs = (System.currentTimeMillis() - sessionStartMs).coerceAtLeast(0L)
        val dateStartMs = startOfDayMs(sessionStartMs)
        val session = StudySession(
            date = dateStartMs,
            wordsReviewed = wordsReviewed,
            correctCount = correctCount,
            objectiveReviewed = objectiveReviewed,
            objectiveCorrect = objectiveCorrect,
            studyTimeMs = durationMs,
        )
        scope.launch {
            studySessionRepository.insert(session)
        }
    }

    private fun ensureSessionStarted() {
        if (!sessionActive) {
            sessionActive = true
            sessionStartMs = System.currentTimeMillis()
            wordsReviewed = 0
            correctCount = 0
            objectiveReviewed = 0
            objectiveCorrect = 0
        }
    }

    private fun startOfDayMs(epochMs: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
