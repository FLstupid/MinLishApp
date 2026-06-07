package com.example.minlish.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocabulary_sets")
data class VocabularySet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val description: String? = null,
    val tags: String = "", // Room doesn't support List out of the box, use CSV or TypeConverter
    val wordCount: Int = 0,

    // Metadata for future per-user support.
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String = ""
)
