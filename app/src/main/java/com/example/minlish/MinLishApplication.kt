package com.example.minlish

import android.app.Application
import com.example.minlish.data.AppDatabase
import com.example.minlish.data.preferences.UserPreferencesRepository
import com.example.minlish.data.repository.StudySessionRepository
import com.example.minlish.data.repository.UserRepository
import com.example.minlish.data.repository.VocabSetRepository
import com.example.minlish.data.repository.WordRepository
import com.example.minlish.logic.notification.ReminderReceiver
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
    val studySessionRepository by lazy { StudySessionRepository(database.studySessionDao()) }
    val userRepository by lazy { UserRepository(firestore) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val starterPackInstaller by lazy {
        StarterPackInstaller(
            context = this,
            vocabSetRepository = vocabSetRepository,
            wordRepository = repository,
            userPreferencesRepository = userPreferencesRepository,
        )
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val reminderReceiver = ReminderReceiver()

    override fun onCreate() {
        super.onCreate()
        // Modern initialization (optional if google-services.json is used, but good practice)
        Firebase.initialize(this)
        observePreferencesAndSchedule()
        appScope.launch(Dispatchers.IO) {
            starterPackInstaller.runAutoSeedIfNeeded()
        }
    }

    /**
     * Observe notification preferences and reschedule the exact alarm whenever
     * the enabled state, hour, or minute changes.
     */
    private fun observePreferencesAndSchedule() {
        appScope.launch {
            userPreferencesRepository.notificationsEnabled
                .combine(userPreferencesRepository.reminderHour) { enabled, hour -> enabled to hour }
                .combine(userPreferencesRepository.reminderMinute) { (enabled, hour), minute ->
                    Triple(enabled, hour, minute)
                }
                .collect { (enabled, hour, minute) ->
                    if (enabled) {
                        reminderReceiver.scheduleExactAlarm(this@MinLishApplication, hour, minute)
                    } else {
                        reminderReceiver.cancelAlarm(this@MinLishApplication)
                    }
                }
        }
    }
}
