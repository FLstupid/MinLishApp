package com.example.minlish.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val setId: Long, // Foreign key to VocabularySet
    val word: String,
    val pronunciation: String? = null,
    val meaning: String,
    val descriptionEn: String? = null,
    val example: String? = null,
    val note: String? = null,
    
    // SM-2 Algorithm fields
    val easeFactor: Double = 2.5,
    val interval: Int = 0, // days
    val repetitions: Int = 0,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastReviewed: Long? = null
)
