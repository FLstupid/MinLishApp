package com.example.minlish.logic

import com.example.minlish.data.model.Word
import java.util.Calendar
import kotlin.math.max

object SrsEngine {
    /**
     * Thuật toán SM-2 tính toán thời gian ôn tập tiếp theo.
     * Quality: 0 (Again) -> 5 (Easy)
     */
    fun calculateNextReview(word: Word, quality: Int): Word {
        require(quality in 0..5) { "Quality must be 0..5, got $quality" }

        val newRepetitions: Int
        val newInterval: Int
        var newEaseFactor: Double

        if (quality >= 3) { // Trả lời đúng (Hard, Good, Easy)
            newInterval = when (word.repetitions) {
                0 -> 1
                1 -> 6
                else -> (word.interval * word.easeFactor).toInt().coerceAtLeast(1)
            }
            newRepetitions = word.repetitions + 1

            // Cập nhật Ease Factor: EF'=EF+(0.1-(5-q)*(0.08+(5-q)*0.02))
            newEaseFactor = word.easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
        } else { // Trả lời sai (Again)
            newRepetitions = 0
            newInterval = 1
            newEaseFactor = max(1.3, word.easeFactor - 0.2)
        }

        newEaseFactor = newEaseFactor.coerceIn(1.3, 2.5)

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, newInterval)

        return word.copy(
            repetitions = newRepetitions,
            interval = newInterval,
            easeFactor = newEaseFactor,
            nextReviewDate = calendar.timeInMillis,
            lastReviewed = System.currentTimeMillis()
        )
    }
}
