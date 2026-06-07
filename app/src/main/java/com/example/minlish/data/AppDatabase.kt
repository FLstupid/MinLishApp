package com.example.minlish.data

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.minlish.data.dao.VocabularySetDao
import com.example.minlish.data.dao.WordDao
import com.example.minlish.data.model.VocabularySet
import com.example.minlish.data.model.Word
import com.example.minlish.data.model.StudySession
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Word::class, VocabularySet::class, StudySession::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun vocabularySetDao(): VocabularySetDao
    abstract fun studySessionDao(): com.example.minlish.data.dao.StudySessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "minlish_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Word table: add collocation/relatedWords
        db.execSQL("ALTER TABLE words ADD COLUMN collocation TEXT")
        db.execSQL("ALTER TABLE words ADD COLUMN relatedWords TEXT")

        // Vocabulary sets metadata
        db.execSQL("ALTER TABLE vocabulary_sets ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE vocabulary_sets ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS study_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date INTEGER NOT NULL,
                wordsReviewed INTEGER NOT NULL,
                correctCount INTEGER NOT NULL,
                studyTimeMs INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE words ADD COLUMN cefrLevel TEXT")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE words ADD COLUMN wordNorm TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE words SET wordNorm = LOWER(TRIM(word))")
        db.execSQL(
            """
            DELETE FROM words
            WHERE id NOT IN (
                SELECT id FROM (
                    SELECT id,
                        ROW_NUMBER() OVER (
                            PARTITION BY setId, wordNorm
                            ORDER BY repetitions DESC,
                                COALESCE(lastReviewed, 0) DESC,
                                id ASC
                        ) AS rn
                    FROM words
                )
                WHERE rn = 1
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_words_setId_wordNorm ON words(setId, wordNorm)"
        )
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE study_sessions ADD COLUMN objectiveReviewed INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE study_sessions ADD COLUMN objectiveCorrect INTEGER NOT NULL DEFAULT 0"
        )
    }
}
