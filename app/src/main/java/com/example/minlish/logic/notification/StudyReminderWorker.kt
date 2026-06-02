package com.example.minlish.logic.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.minlish.MinLishApplication
import kotlinx.coroutines.flow.first

class StudyReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val application = applicationContext as MinLishApplication
        val repository = application.repository
        
        // Get count of words due for review
        val wordsToReview = repository.getWordsToReview(System.currentTimeMillis()).first()
        
        if (wordsToReview.isNotEmpty()) {
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.createNotificationChannel()
            notificationHelper.showReviewNotification(wordsToReview.size)
        }
        
        return androidx.work.ListenableWorker.Result.success()
    }
}
