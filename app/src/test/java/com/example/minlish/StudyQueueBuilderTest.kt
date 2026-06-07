package com.example.minlish

import com.example.minlish.data.model.Word
import com.example.minlish.logic.StudyQueueBuilder
import org.junit.Assert.assertEquals
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
}
