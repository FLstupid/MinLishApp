package com.example.minlish.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Epoch millis for the day start (local time).
    val date: Long = 0,

    /** Self-rated SRS reviews (Learn / practice flashcards). */
    val wordsReviewed: Int = 0,
    val correctCount: Int = 0,

    /** Quiz, typing, and checkpoint answers. */
    val objectiveReviewed: Int = 0,
    val objectiveCorrect: Int = 0,

    // Total time spent in this learn session (ms).
    val studyTimeMs: Long = 0,
)
