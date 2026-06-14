package com.example.minlish.ui.screen

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.minlish.R
import com.example.minlish.data.model.User
import com.example.minlish.logic.CefrLevels
import com.example.minlish.ui.viewmodel.AuthOneTimeEvent
import com.example.minlish.ui.viewmodel.AuthUiState
import com.example.minlish.ui.viewmodel.AuthViewModel
import com.example.minlish.ui.viewmodel.ProfileUiEvent
import com.example.minlish.ui.viewmodel.ProfileViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProfileScreen(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val profile = authViewModel.profile.collectAsState().value
    val currentUser = authViewModel.currentUser.collectAsState().value
    val notificationSettings by profileViewModel.notificationSettings.collectAsState()
    val uiState by authViewModel.uiState.collectAsState()

    var enabledText by rememberSaveable { mutableStateOf(notificationSettings.notificationsEnabled) }
    var hourText by rememberSaveable { mutableStateOf(notificationSettings.reminderHour.toString()) }
    var minuteText by rememberSaveable { mutableStateOf(notificationSettings.reminderMinute.toString()) }

    var hasNotificationPermission by remember {
        mutableStateOf(checkNotificationPermission(context))
    }
    var canScheduleExact by remember {
        mutableStateOf(checkExactAlarmPermission(context))
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted
    }

    LaunchedEffect(notificationSettings) {
        enabledText = notificationSettings.notificationsEnabled
        hourText = notificationSettings.reminderHour.toString()
        minuteText = notificationSettings.reminderMinute.toString()
    }

    LaunchedEffect(Unit) {
        canScheduleExact = checkExactAlarmPermission(context)
    }

    LaunchedEffect(profileViewModel) {
        profileViewModel.events.collect { event ->
            when (event) {
                is ProfileUiEvent.NotificationSettingsSaved -> {
                    hasNotificationPermission = checkNotificationPermission(context)
                    canScheduleExact = checkExactAlarmPermission(context)

                    val message = if (event.enabled) {
                        context.getString(
                            R.string.profile_notifications_saved,
                            event.hour,
                            event.minute,
                        )
                    } else {
                        context.getString(R.string.profile_notifications_saved_off)
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    LaunchedEffect(authViewModel) {
        authViewModel.oneTimeEvents.collect { event ->
            when (event) {
                is AuthOneTimeEvent.ProfileSaved ->
                    snackbarHostState.showSnackbar(context.getString(R.string.profile_saved_ok))
                is AuthOneTimeEvent.ProfileSaveError ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            profile?.email?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            var name by rememberSaveable { mutableStateOf(profile?.name.orEmpty()) }
            val goalOptions = listOf("IELTS", "Communication", "Business", "Travel", "Other")
            val levelOptions = CefrLevels.ORDERED

            var goal by rememberSaveable { mutableStateOf(profile?.goal.orEmpty()) }
            var level by rememberSaveable { mutableStateOf(profile?.level.orEmpty()) }
            var dailyGoalText by rememberSaveable { mutableStateOf(profile?.dailyGoal?.toString().orEmpty()) }

            var goalExpanded by remember { mutableStateOf(false) }
            var levelExpanded by remember { mutableStateOf(false) }

            LaunchedEffect(profile) {
                name = profile?.name.orEmpty()
                val normalizedGoal = profile?.goal.orEmpty().takeIf { it in goalOptions } ?: CefrLevels.DEFAULT_GOAL
                val normalizedLevel = profile?.level.orEmpty()
                    .takeIf { it in levelOptions } ?: CefrLevels.DEFAULT_LEVEL
                goal = normalizedGoal
                level = normalizedLevel
                dailyGoalText = profile?.dailyGoal?.toString().orEmpty()
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_name)) },
                singleLine = true,
            )

            ExposedDropdownMenuBox(
                expanded = goalExpanded,
                onExpandedChange = { goalExpanded = it },
            ) {
                OutlinedTextField(
                    value = goal,
                    onValueChange = { },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    label = { Text(stringResource(R.string.field_goal)) },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalExpanded) },
                )
                ExposedDropdownMenu(
                    expanded = goalExpanded,
                    onDismissRequest = { goalExpanded = false },
                ) {
                    goalOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                goal = option
                                goalExpanded = false
                            },
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.field_goal_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = levelExpanded,
                onExpandedChange = { levelExpanded = it },
            ) {
                OutlinedTextField(
                    value = level,
                    onValueChange = { },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    label = { Text(stringResource(R.string.field_level)) },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                )
                ExposedDropdownMenu(
                    expanded = levelExpanded,
                    onDismissRequest = { levelExpanded = false },
                ) {
                    levelOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                level = option
                                levelExpanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = dailyGoalText,
                onValueChange = { dailyGoalText = it.filter { ch -> ch.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_daily_goal)) },
                singleLine = true,
            )

            Spacer(Modifier.height(8.dp))

            // ── Notifications section (collapsible) ──
            var showNotifications by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNotifications = !showNotifications },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.profile_notifications_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = if (showNotifications) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            }

            AnimatedVisibility(visible = showNotifications) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.profile_notifications))
                Switch(
                    checked = enabledText,
                    onCheckedChange = { newValue ->
                        if (newValue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            enabledText = newValue
                        }
                    },
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                PermissionWarningCard(
                    message = stringResource(R.string.profile_notification_permission_missing),
                    buttonText = stringResource(R.string.profile_notification_permission_grant),
                    onClick = {
                        notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact) {
                PermissionWarningCard(
                    message = stringResource(R.string.profile_exact_alarm_permission_missing),
                    buttonText = stringResource(R.string.profile_exact_alarm_permission_grant),
                    onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = hourText,
                    onValueChange = { hourText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.field_hour)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { minuteText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.field_minute)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            Button(
                onClick = {
                    val hour = hourText.toIntOrNull() ?: 9
                    val minute = minuteText.toIntOrNull() ?: 0
                    profileViewModel.saveNotificationSettings(enabledText, hour, minute)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(R.string.profile_save_settings))
            }
                } // Column inside AnimatedVisibility
            } // AnimatedVisibility

            val isLoading = uiState is AuthUiState.Loading

            Button(
                onClick = {
                    val uid = profile?.uid?.takeIf { it.isNotBlank() }
                        ?: currentUser?.uid.orEmpty()
                    val dailyGoal = dailyGoalText.toIntOrNull() ?: 10
                    authViewModel.updateProfile(
                        User(
                            uid = uid,
                            name = name.trim(),
                            email = profile?.email?.takeIf { it.isNotBlank() }
                                ?: currentUser?.email.orEmpty(),
                            goal = goal.trim().ifBlank { CefrLevels.DEFAULT_GOAL },
                            level = level.trim().ifBlank { CefrLevels.DEFAULT_LEVEL },
                            dailyGoal = dailyGoal,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentUser != null && !isLoading,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    if (isLoading) {
                        stringResource(R.string.profile_saving)
                    } else {
                        stringResource(R.string.profile_save_profile)
                    },
                )
            }

            OutlinedButton(
                onClick = { authViewModel.signOut() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_sign_out))
            }
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    val app = context.applicationContext
    return app.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun checkExactAlarmPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return alarmManager.canScheduleExactAlarms()
}

@Composable
private fun PermissionWarningCard(
    message: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onClick),
            )
        }
    }
}
