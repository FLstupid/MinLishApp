package com.example.minlish.logic.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.minlish.MinLishApplication
import com.example.minlish.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class StudyReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val application = applicationContext as MinLishApplication
        val repository = application.repository
        val prefs = UserPreferencesRepository(applicationContext)
        val enabled = prefs.notificationsEnabled.first()
        if (!enabled) return androidx.work.ListenableWorker.Result.success()
        
        val now = System.currentTimeMillis()
        val dueWords = repository.getDueReviewWordsAnySet(now).first()

        if (dueWords.isNotEmpty()) {
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.createNotificationChannel()
            notificationHelper.showReviewNotification(dueWords.size, openLearnTab = true)
        }
        
        return androidx.work.ListenableWorker.Result.success()
    }
}
