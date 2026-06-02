package com.example.minlish.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.minlish.data.dao.VocabularySetDao
import com.example.minlish.data.dao.WordDao
import com.example.minlish.data.model.VocabularySet
import com.example.minlish.data.model.Word

@Database(entities = [Word::class, VocabularySet::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun vocabularySetDao(): VocabularySetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "minlish_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
