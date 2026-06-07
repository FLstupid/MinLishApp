package com.example.minlish

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.minlish.data.AppDatabase
import com.example.minlish.data.model.VocabularySet
import com.example.minlish.data.repository.WordRepository
import com.example.minlish.logic.importexport.VocabIO
import com.example.minlish.logic.importexport.WordImportHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WordImportHelperInstrumentedTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: WordRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WordRepository(db.wordDao())
        runBlocking {
            db.vocabularySetDao().insertSet(
                VocabularySet(
                    id = 1L,
                    title = "Test",
                    description = null,
                    tags = "",
                    wordCount = 0,
                    createdAt = 0L,
                    userId = "",
                ),
            )
        }
    }

    @Test
    fun importSkipsDuplicateRowsInSameBatch() = runBlocking {
        val rows = listOf(
            row("Persistence", "mot"),
            row("persistence", "hai"),
            row("PERSISTENCE", "ba"),
        )
        val stats = WordImportHelper.importRowsIntoSet(
            wordRepository = repository,
            setId = 1L,
            rows = rows,
        )
        assertEquals(1, stats.inserted)
        assertEquals(2, stats.skipped)
        assertEquals(1, db.wordDao().countWordsInSet(1L))
    }

    private fun row(word: String, meaning: String): VocabIO.VocabRow =
        VocabIO.VocabRow(
            word = word,
            pronunciation = null,
            meaning = meaning,
            descriptionEn = null,
            example = null,
            collocation = null,
            relatedWords = null,
            note = null,
        )
}
