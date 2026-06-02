package com.example.minlish

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.minlish.data.AppDatabase
import com.example.minlish.data.repository.WordRepository
import com.example.minlish.logic.notification.StudyReminderWorker
import java.util.concurrent.TimeUnit

class MinLishApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { WordRepository(database.wordDao()) }

    override fun onCreate() {
        super.onCreate()
        scheduleStudyReminder()
    }

    private fun scheduleStudyReminder() {
        val workRequest = PeriodicWorkRequestBuilder<StudyReminderWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "StudyReminderWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
