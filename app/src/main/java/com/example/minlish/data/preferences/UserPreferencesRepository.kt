package com.example.minlish.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.minlish.data.model.User
import com.example.minlish.logic.CefrLevels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.minLishDataStore by preferencesDataStore(name = "minlish_prefs")

class UserPreferencesRepository(
    private val context: Context,
) {
    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")
    private val activeSetIdKey = longPreferencesKey("active_set_id")
    private val dailyNewStudiedDateKey = longPreferencesKey("daily_new_studied_date")
    private val dailyNewStudiedCountKey = intPreferencesKey("daily_new_studied_count")
    private val reminderHourKey = intPreferencesKey("reminder_hour")
    private val reminderMinuteKey = intPreferencesKey("reminder_minute")
    private val starterAutoInstalledKey = booleanPreferencesKey("starter_auto_installed")
    private val installedStarterPackIdsKey = stringPreferencesKey("installed_starter_pack_ids")

    private val profileOwnerUidKey = stringPreferencesKey("profile_owner_uid")
    private val profileNameKey = stringPreferencesKey("profile_name")
    private val profileGoalKey = stringPreferencesKey("profile_goal")
    private val profileLevelKey = stringPreferencesKey("profile_level")
    private val profileDailyGoalKey = intPreferencesKey("profile_daily_goal")

    // Serialize daily-new counter operations to avoid lost increments when multiple coroutines fire.
    private val dailyNewCounterMutex = Mutex()

    val activeSetId: Flow<Long> =
        context.minLishDataStore.data.map { prefs ->
            prefs[activeSetIdKey] ?: 0L
        }

    val notificationsEnabled: Flow<Boolean> =
        context.minLishDataStore.data.map { prefs ->
            prefs[notificationsEnabledKey] ?: true
        }

    val reminderHour: Flow<Int> =
        context.minLishDataStore.data.map { prefs ->
            prefs[reminderHourKey] ?: 9
        }

    val reminderMinute: Flow<Int> =
        context.minLishDataStore.data.map { prefs ->
            prefs[reminderMinuteKey] ?: 0
        }

    val installedStarterPackIds: Flow<Set<String>> =
        context.minLishDataStore.data.map { prefs ->
            parseStarterPackIds(prefs[installedStarterPackIdsKey])
        }

    suspend fun isStarterAutoInstalled(): Boolean {
        val prefs = context.minLishDataStore.data.first()
        return prefs[starterAutoInstalledKey] ?: false
    }

    suspend fun setStarterAutoInstalled() {
        context.minLishDataStore.edit { prefs ->
            prefs[starterAutoInstalledKey] = true
        }
    }

    suspend fun isStarterPackInstalled(packId: String): Boolean {
        val prefs = context.minLishDataStore.data.first()
        return parseStarterPackIds(prefs[installedStarterPackIdsKey]).contains(packId)
    }

    suspend fun markStarterPackInstalled(packId: String) {
        context.minLishDataStore.edit { prefs ->
            val updated = parseStarterPackIds(prefs[installedStarterPackIdsKey]).toMutableSet()
            updated.add(packId)
            prefs[installedStarterPackIdsKey] = updated.joinToString(",")
        }
    }

    private fun parseStarterPackIds(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.minLishDataStore.edit { prefs ->
            prefs[notificationsEnabledKey] = enabled
        }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        context.minLishDataStore.edit { prefs ->
            prefs[reminderHourKey] = safeHour
            prefs[reminderMinuteKey] = safeMinute
        }
    }

    suspend fun setActiveSetId(setId: Long) {
        context.minLishDataStore.edit { prefs ->
            prefs[activeSetIdKey] = setId
        }
    }

    /**
     * dailyNewStudiedCount is reset automatically when [todayKeyMs] changes.
     */
    suspend fun getAndResetDailyNewStudiedCount(todayKeyMs: Long): Int {
        return dailyNewCounterMutex.withLock {
            val prefs = context.minLishDataStore.data.first()
            val storedDate = prefs[dailyNewStudiedDateKey] ?: 0L
            if (storedDate != todayKeyMs) {
                context.minLishDataStore.edit { editPrefs ->
                    editPrefs[dailyNewStudiedDateKey] = todayKeyMs
                    editPrefs[dailyNewStudiedCountKey] = 0
                }
                return@withLock 0
            }
            prefs[dailyNewStudiedCountKey] ?: 0
        }
    }

    suspend fun incrementDailyNewStudiedCount(todayKeyMs: Long): Int {
        return dailyNewCounterMutex.withLock {
            val prefs = context.minLishDataStore.data.first()
            val storedDate = prefs[dailyNewStudiedDateKey] ?: 0L
            var newCount = 0
            context.minLishDataStore.edit { editPrefs ->
                if (storedDate != todayKeyMs) {
                    editPrefs[dailyNewStudiedDateKey] = todayKeyMs
                    editPrefs[dailyNewStudiedCountKey] = 0
                }
                val cur = editPrefs[dailyNewStudiedCountKey] ?: 0
                newCount = cur + 1
                editPrefs[dailyNewStudiedCountKey] = newCount
            }
            newCount
        }
    }

    /** Local cache of profile fields; Firestore `users/{uid}` is synced from [AuthViewModel]. */
    suspend fun loadProfileForUser(
        uid: String,
        email: String,
        displayName: String,
    ): User {
        val prefs = context.minLishDataStore.data.first()
        if (prefs[profileOwnerUidKey] != uid) {
            return User(
                uid = uid,
                email = email,
                name = displayName,
            )
        }
        return User(
            uid = uid,
            email = email,
            name = prefs[profileNameKey].orEmpty().ifBlank { displayName },
            goal = prefs[profileGoalKey] ?: CefrLevels.DEFAULT_GOAL,
            level = CefrLevels.normalize(prefs[profileLevelKey] ?: CefrLevels.DEFAULT_LEVEL),
            dailyGoal = prefs[profileDailyGoalKey] ?: 10,
        )
    }

    suspend fun saveProfile(profile: User) {
        context.minLishDataStore.edit { prefs ->
            prefs[profileOwnerUidKey] = profile.uid
            prefs[profileNameKey] = profile.name
            prefs[profileGoalKey] = profile.goal
            prefs[profileLevelKey] = CefrLevels.normalize(profile.level)
            prefs[profileDailyGoalKey] = profile.dailyGoal.coerceAtLeast(1)
        }
    }

}

