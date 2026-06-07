package com.example.minlish

import com.example.minlish.logic.WordNorm
import org.junit.Assert.assertEquals
import org.junit.Test

class WordNormTest {

    @Test
    fun normalizeTrimsAndLowercases() {
        assertEquals("hello", WordNorm.normalize("  Hello  "))
    }
}
