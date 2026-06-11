package com.example.minlish

import com.example.minlish.data.model.Word
import com.example.minlish.logic.SrsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsEngineTest {

    @Test
    fun success_incrementsRepetitions() {
        val word = Word(word = "test", meaning = "x", repetitions = 0, easeFactor = 2.5)
        val updated = SrsEngine.calculateNextReview(word, quality = 4)
        assertEquals(1, updated.repetitions)
        assertTrue(updated.lastReviewed != null)
    }

    @Test
    fun again_resetsRepetitions_andLowersEase() {
        val word = Word(
            word = "test",
            meaning = "x",
            repetitions = 2,
            interval = 6,
            easeFactor = 2.5,
        )
        val updated = SrsEngine.calculateNextReview(word, quality = 0)
        assertEquals(0, updated.repetitions)
        assertEquals(1, updated.interval)
        assertEquals(2.3, updated.easeFactor, 0.001)
    }

    @Test
    fun easeFactor_floorAtOnePointThree() {
        val word = Word(word = "test", meaning = "x", easeFactor = 1.35)
        val updated = SrsEngine.calculateNextReview(word, quality = 0)
        assertEquals(1.3, updated.easeFactor, 0.001)
    }

    @Test
    fun easy_highQuality_increasesEaseFactor() {
        val word = Word(word = "test", meaning = "x", repetitions = 1, easeFactor = 2.5)
        val updated = SrsEngine.calculateNextReview(word, quality = 5)
        assertTrue(updated.easeFactor > 2.5)
    }

    @Test
    fun interval_firstSuccess_isOne() {
        val word = Word(word = "test", meaning = "x", repetitions = 0, interval = 0)
        val updated = SrsEngine.calculateNextReview(word, quality = 4)
        assertEquals(1, updated.interval)
    }

    @Test
    fun interval_secondSuccess_isSix() {
        val word = Word(word = "test", meaning = "x", repetitions = 1, interval = 1, easeFactor = 2.5)
        val updated = SrsEngine.calculateNextReview(word, quality = 4)
        assertEquals(6, updated.interval)
    }

    @Test
    fun interval_thirdSuccess_usesEaseFactor() {
        val word = Word(word = "test", meaning = "x", repetitions = 2, interval = 6, easeFactor = 2.5)
        val updated = SrsEngine.calculateNextReview(word, quality = 4)
        assertEquals(15, updated.interval) // 6 * 2.5 = 15
    }

    @Test
    fun lastReviewed_alwaysUpdated() {
        val word = Word(word = "test", meaning = "x", lastReviewed = null)
        val updated = SrsEngine.calculateNextReview(word, quality = 3)
        assertTrue(updated.lastReviewed != null)
        assertTrue(updated.lastReviewed!! > 0)
    }
}
