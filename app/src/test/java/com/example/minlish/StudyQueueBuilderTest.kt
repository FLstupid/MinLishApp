package com.example.minlish

import com.example.minlish.data.model.Word
import com.example.minlish.logic.StudyQueueBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyQueueBuilderTest {

    @Test
    fun ordersDueBeforeNew_andDedupesByLemma() {
        val due = listOf(
            Word(id = 1, word = "a", wordNorm = "a", meaning = "1", repetitions = 1),
        )
        val retry = listOf(
            Word(id = 2, word = "b", wordNorm = "b", meaning = "2", repetitions = 0),
        )
        val introduce = listOf(
            Word(id = 3, word = "c", wordNorm = "c", meaning = "3"),
            Word(id = 4, word = "A", wordNorm = "a", meaning = "dup"),
        )

        val queue = StudyQueueBuilder.buildDailyQueue(
            dueWords = due,
            pendingIntroductionRetries = retry,
            introduceWords = introduce,
        )

        assertEquals(listOf(1L, 2L, 3L), queue.map { it.id })
    }

    @Test
    fun emptyInputs_returnsEmptyQueue() {
        val queue = StudyQueueBuilder.buildDailyQueue(
            dueWords = emptyList(),
            pendingIntroductionRetries = emptyList(),
            introduceWords = emptyList(),
        )
        assertTrue(queue.isEmpty())
    }

    @Test
    fun dueWordsFirst_inQueue() {
        val due = listOf(
            Word(id = 1, word = "a", wordNorm = "a", meaning = "1", repetitions = 2),
            Word(id = 2, word = "b", wordNorm = "b", meaning = "2", repetitions = 1),
        )
        val introduce = listOf(
            Word(id = 3, word = "c", wordNorm = "c", meaning = "3"),
        )

        val queue = StudyQueueBuilder.buildDailyQueue(
            dueWords = due,
            pendingIntroductionRetries = emptyList(),
            introduceWords = introduce,
        )

        assertEquals(3, queue.size)
        assertEquals(1L, queue[0].id)
        assertEquals(2L, queue[1].id)
        assertEquals(3L, queue[2].id)
    }

    @Test
    fun retriesBeforeNew() {
        val retry = listOf(
            Word(id = 1, word = "a", wordNorm = "a", meaning = "1", repetitions = 0),
        )
        val introduce = listOf(
            Word(id = 2, word = "b", wordNorm = "b", meaning = "2"),
        )

        val queue = StudyQueueBuilder.buildDailyQueue(
            dueWords = emptyList(),
            pendingIntroductionRetries = retry,
            introduceWords = introduce,
        )

        assertEquals(listOf(1L, 2L), queue.map { it.id })
    }

    @Test
    fun deduplication_keepsFirstOccurrence() {
        val due = listOf(
            Word(id = 1, word = "hello", wordNorm = "hello", meaning = "1", repetitions = 1),
        )
        val introduce = listOf(
            Word(id = 2, word = "Hello", wordNorm = "hello", meaning = "2"),
        )

        val queue = StudyQueueBuilder.buildDailyQueue(
            dueWords = due,
            pendingIntroductionRetries = emptyList(),
            introduceWords = introduce,
        )

        assertEquals(1, queue.size)
        assertEquals(1L, queue[0].id)
    }
}
