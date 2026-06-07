package com.example.minlish.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [Index(value = ["setId", "wordNorm"], unique = true)],
)
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val setId: Long = 0, // Foreign key to VocabularySet
    val word: String = "",
    /** Lowercase trimmed [word]; used for uniqueness within a set. */
    val wordNorm: String = "",
    val pronunciation: String? = null,
    val meaning: String = "",
    val descriptionEn: String? = null,
    val collocation: String? = null,
    val example: String? = null,
    val relatedWords: String? = null,
    val note: String? = null,
    val cefrLevel: String? = null,

    // SM-2 Algorithm fields
    val easeFactor: Double = 2.5,
    val interval: Int = 0, // days
    val repetitions: Int = 0,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastReviewed: Long? = null
)
