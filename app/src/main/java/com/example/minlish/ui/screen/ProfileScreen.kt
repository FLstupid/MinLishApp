package com.example.minlish.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.minlish.R
import com.example.minlish.data.model.User
import com.example.minlish.ui.viewmodel.AuthUiState
import com.example.minlish.ui.viewmodel.AuthViewModel
import com.example.minlish.ui.viewmodel.ProfileViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProfileScreen(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
) {
    val profile = authViewModel.profile.collectAsState().value
    val notificationSettings by profileViewModel.notificationSettings.collectAsState()

    var enabledText by rememberSaveable { mutableStateOf(notificationSettings.notificationsEnabled) }
    var hourText by rememberSaveable { mutableStateOf(notificationSettings.reminderHour.toString()) }
    var minuteText by rememberSaveable { mutableStateOf(notificationSettings.reminderMinute.toString()) }

    LaunchedEffect(notificationSettings) {
        enabledText = notificationSettings.notificationsEnabled
        hourText = notificationSettings.reminderHour.toString()
        minuteText = notificationSettings.reminderMinute.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier.padding(24.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        profile?.email?.let { email ->
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        var name by rememberSaveable { mutableStateOf(profile?.name.orEmpty()) }
        val goalOptions = listOf("IELTS", "Communication", "Business", "Travel", "Other")
        val levelOptions = listOf("A1", "A2", "B1", "B2", "C1", "C2")

        var goal by rememberSaveable { mutableStateOf(profile?.goal.orEmpty()) }
        var level by rememberSaveable { mutableStateOf(profile?.level.orEmpty()) }
        var dailyGoalText by rememberSaveable { mutableStateOf(profile?.dailyGoal?.toString().orEmpty()) }

        var goalExpanded by remember { mutableStateOf(false) }
        var levelExpanded by remember { mutableStateOf(false) }

        LaunchedEffect(profile) {
            name = profile?.name.orEmpty()
            val normalizedGoal = profile?.goal.orEmpty().takeIf { it in goalOptions } ?: "IELTS"
            val normalizedLevel = profile?.level.orEmpty()
                .takeIf { it in levelOptions } ?: "B1"
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

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.profile_notifications_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.profile_notifications))
            androidx.compose.material3.Switch(
                checked = enabledText,
                onCheckedChange = { enabledText = it },
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

        val isLoading = authViewModel.uiState.collectAsState().value is AuthUiState.Loading

        Button(
            onClick = {
                val uid = profile?.uid.orEmpty()
                if (uid.isBlank()) return@Button
                val dailyGoal = dailyGoalText.toIntOrNull() ?: 10
                authViewModel.updateProfile(
                    User(
                        uid = uid,
                        name = name.trim(),
                        email = profile?.email.orEmpty(),
                        goal = goal.trim().ifBlank { "IELTS" },
                        level = level.trim().ifBlank { "B1" },
                        dailyGoal = dailyGoal,
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = profile != null && !isLoading,
            shape = MaterialTheme.shapes.large,
        ) {
            Text(if (isLoading) stringResource(R.string.profile_saving) else stringResource(R.string.profile_save_profile))
        }

        OutlinedButton(
            onClick = { authViewModel.signOut() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.profile_sign_out))
        }
    }
}
