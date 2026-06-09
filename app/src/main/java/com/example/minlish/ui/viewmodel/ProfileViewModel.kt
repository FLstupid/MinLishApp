package com.example.minlish.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.minlish.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
)

sealed class ProfileUiEvent {
    data class NotificationSettingsSaved(
        val enabled: Boolean,
        val hour: Int,
        val minute: Int,
    ) : ProfileUiEvent()
}

class ProfileViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _events = Channel<ProfileUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

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
            val safeHour = hour.coerceIn(0, 23)
            val safeMinute = minute.coerceIn(0, 59)
            userPreferencesRepository.setNotificationsEnabled(enabled)
            userPreferencesRepository.setReminderTime(safeHour, safeMinute)
            _events.send(
                ProfileUiEvent.NotificationSettingsSaved(
                    enabled = enabled,
                    hour = safeHour,
                    minute = safeMinute,
                ),
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
