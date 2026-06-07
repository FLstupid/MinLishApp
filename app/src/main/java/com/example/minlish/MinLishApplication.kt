package com.example.minlish

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.minlish.data.AppDatabase
import com.example.minlish.data.preferences.UserPreferencesRepository
import com.example.minlish.data.repository.StudySessionRepository
import com.example.minlish.data.repository.UserRepository
import com.example.minlish.data.repository.VocabSetRepository
import com.example.minlish.data.repository.WordRepository
import com.example.minlish.logic.TtsManager
import com.example.minlish.logic.notification.StudyReminderWorker
import com.example.minlish.logic.starter.StarterPackInstaller
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.initialize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MinLishApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    
    // Modern Firebase Access
    val firebaseAuth by lazy { Firebase.auth }
    val firestore by lazy { Firebase.firestore }
    
    val repository by lazy { WordRepository(database.wordDao(), firebaseAuth, firestore) }
    val vocabSetRepository by lazy {
        VocabSetRepository(
            vocabularySetDao = database.vocabularySetDao(),
            wordDao = database.wordDao(),
            firebaseAuth = firebaseAuth,
            firestore = firestore
        )
    }
    val userRepository by lazy { UserRepository(firestore) }
    val studySessionRepository by lazy { StudySessionRepository(database.studySessionDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val ttsManager by lazy { TtsManager(applicationContext) }
    val starterPackInstaller by lazy {
        StarterPackInstaller(
            context = this,
            vocabSetRepository = vocabSetRepository,
            wordRepository = repository,
            userPreferencesRepository = userPreferencesRepository,
        )
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Modern initialization (optional if google-services.json is used, but good practice)
        Firebase.initialize(this)
        observePreferencesAndSchedule()
        ttsManager
        appScope.launch(Dispatchers.IO) {
            starterPackInstaller.runAutoSeedIfNeeded()
        }
    }

    private fun observePreferencesAndSchedule() {
        appScope.launch {
            userPreferencesRepository.notificationsEnabled
                .combine(userPreferencesRepository.reminderHour) { enabled, hour -> enabled to hour }
                .combine(userPreferencesRepository.reminderMinute) { (enabled, hour), minute ->
                    Triple(enabled, hour, minute)
                }
                .collect { (enabled, hour, minute) ->
                    scheduleStudyReminder(enabled = enabled, hour = hour, minute = minute)
                }
        }
    }

    private fun scheduleStudyReminder(enabled: Boolean, hour: Int, minute: Int) {
        val workManager = WorkManager.getInstance(this)

        if (!enabled) {
            workManager.cancelUniqueWork("StudyReminderWork")
            return
        }

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val nextTime = if (cal.timeInMillis <= now) {
            cal.apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        } else {
            cal.timeInMillis
        }
        val initialDelayMs = (nextTime - now).coerceAtLeast(0L)

        val workRequest = PeriodicWorkRequestBuilder<StudyReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "StudyReminderWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
