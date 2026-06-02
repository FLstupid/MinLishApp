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
        val newRepetitions: Int
        val newInterval: Int
        var newEaseFactor: Double = word.easeFactor

        if (quality >= 3) { // Trả lời đúng (Hard, Good, Easy)
            if (word.repetitions == 0) {
                newInterval = 1
            } else if (word.repetitions == 1) {
                newInterval = 6
            } else {
                newInterval = (word.interval * word.easeFactor).toInt()
            }
            newRepetitions = word.repetitions + 1
            
            // Cập nhật Ease Factor: EF'=EF+(0.1-(5-q)*(0.08+(5-q)*0.02))
            newEaseFactor = word.easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
        } else { // Trả lời sai (Again)
            newRepetitions = 0
            newInterval = 1
            // Ease factor giữ nguyên hoặc giảm nhẹ nếu muốn
        }

        if (newEaseFactor < 1.3) newEaseFactor = 1.3

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
