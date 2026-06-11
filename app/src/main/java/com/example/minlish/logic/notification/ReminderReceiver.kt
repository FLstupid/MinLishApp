package com.example.minlish.logic.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.minlish.MinLishApplication
import com.example.minlish.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * BroadcastReceiver that fires the study reminder notification at the scheduled time,
 * and re-schedules the next alarm after each trigger or boot.
 *
 * On Android 12+ uses [AlarmManager.setExactAndAllowWhileIdle] for reliable delivery,
 * but falls back to inexact alarms if the exact-alarm permission hasn't been granted.
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"
        const val ACTION_REMINDER = "com.example.minlish.ACTION_STUDY_REMINDER"
        const val ACTION_BOOT = "android.intent.action.BOOT_COMPLETED"
        private const val REQUEST_CODE = 2001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        scope.launch {
            try {
                when (intent.action) {
                    ACTION_BOOT -> {
                        rescheduleIfNeeded(context)
                    }
                    ACTION_REMINDER -> {
                        fireNotification(context)
                        rescheduleIfNeeded(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun fireNotification(context: Context) {
        val prefs = UserPreferencesRepository(context)
        val enabled = prefs.notificationsEnabled.first()
        if (!enabled) return

        val app = context.applicationContext as MinLishApplication
        val repository = app.repository
        val now = System.currentTimeMillis()
        val dueWords = repository.getDueReviewWordsAnySet(now).first()

        if (dueWords.isNotEmpty()) {
            val notificationHelper = NotificationHelper(context)
            notificationHelper.createNotificationChannel()
            notificationHelper.showReviewNotification(dueWords.size, openLearnTab = true)
        }
    }

    private suspend fun rescheduleIfNeeded(context: Context) {
        val prefs = UserPreferencesRepository(context)
        val enabled = prefs.notificationsEnabled.first()
        if (!enabled) return

        val hour = prefs.reminderHour.first()
        val minute = prefs.reminderMinute.first()
        scheduleExactAlarm(context, hour, minute)
    }

    /**
     * Schedule an alarm for [hour]:[minute] today (or tomorrow if already passed).
     *
     * On Android 12+ (API 31+), exact alarms require the `SCHEDULE_EXACT_ALARM` permission
     * to be explicitly granted by the user. If the permission is not yet granted,
     * we fall back to an inexact alarm so the app never crashes.
     */
    fun scheduleExactAlarm(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Permission not yet granted — use inexact alarm instead of crashing.
            Log.w(TAG, "SCHEDULE_EXACT_ALARM not granted, falling back to inexact alarm")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent,
            )
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context))
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
