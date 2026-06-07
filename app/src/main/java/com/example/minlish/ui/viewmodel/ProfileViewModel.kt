package com.example.minlish.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.minlish.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
)

class ProfileViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val notificationSettings: StateFlow<NotificationSettingsUiState> =
        combine(
            userPreferencesRepository.notificationsEnabled,
            userPreferencesRepository.reminderHour,
            userPreferencesRepository.reminderMinute,
        ) { enabled, hour, minute ->
            NotificationSettingsUiState(
                notificationsEnabled = enabled,
                reminderHour = hour,
                reminderMinute = minute,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationSettingsUiState(),
        )

    fun saveNotificationSettings(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(enabled)
            userPreferencesRepository.setReminderTime(
                hour.coerceIn(0, 23),
                minute.coerceIn(0, 59),
            )
        }
    }
}

class ProfileViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
