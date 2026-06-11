package com.example.minlish.logic

object CefrLevels {
    const val DEFAULT_LEVEL = "B1"
    const val DEFAULT_GOAL = "IELTS"

    val ORDERED = listOf("A1", "A2", "B1", "B2", "C1", "C2")

    fun normalize(raw: String?): String {
        val trimmed = raw?.trim()?.uppercase().orEmpty()
        return trimmed.takeIf { it in ORDERED } ?: DEFAULT_LEVEL
    }

    /** CEFR bands at or below [userLevel] (inclusive). */
    fun allowedUpTo(userLevel: String): List<String> {
        val normalized = normalize(userLevel)
        val maxIndex = ORDERED.indexOf(normalized).coerceAtLeast(0)
        return ORDERED.take(maxIndex + 1)
    }

    fun orderIndex(level: String?): Int {
        if (level.isNullOrBlank()) return Int.MAX_VALUE
        val idx = ORDERED.indexOf(level.trim().uppercase())
        return if (idx >= 0) idx else Int.MAX_VALUE
    }
}
