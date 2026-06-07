package com.example.minlish.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.example.minlish.R
import com.example.minlish.ui.viewmodel.AnalyticsSkillLevel
import com.example.minlish.ui.viewmodel.AnalyticsUiState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun analyticsSkillLevelLabel(level: AnalyticsSkillLevel): String = when (level) {
    AnalyticsSkillLevel.Beginner -> stringResource(R.string.skill_beginner)
    AnalyticsSkillLevel.Intermediate -> stringResource(R.string.skill_intermediate)
    AnalyticsSkillLevel.Advanced -> stringResource(R.string.skill_advanced)
}

@Composable
fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    showTitle: Boolean = true,
) {
    val maxValue = (uiState.dailyActivity.maxOfOrNull { it.wordsReviewed } ?: 0).coerceAtLeast(1)
    val maxRetention = (uiState.dailyRetention.maxOfOrNull { it.retentionPercent } ?: 0).coerceAtLeast(1)
    val fmt = SimpleDateFormat("dd/MM", Locale.getDefault())
    val levelLabel = analyticsSkillLevelLabel(uiState.skillLevel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showTitle) {
            Text(stringResource(R.string.analytics_title), style = MaterialTheme.typography.headlineSmall)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.analytics_streak, uiState.streakDays),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.analytics_session_accuracy, uiState.accuracyPercent),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.analytics_objective_accuracy, uiState.objectiveAccuracyPercent),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.analytics_skill_level, levelLabel),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.analytics_chart_activity), style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.dailyActivity.forEach { item ->
                        val heightFrac = item.wordsReviewed.toFloat() / maxValue.toFloat()
                        val barHeight = (120.dp * heightFrac).coerceAtLeast(4.dp)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                            ) {
                            }
                            Text(
                                text = fmt.format(item.dayStart),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.analytics_chart_accuracy), style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.dailyRetention.forEach { item ->
                        val heightFrac = item.retentionPercent.toFloat() / maxRetention.toFloat()
                        val barHeight = (120.dp * heightFrac).coerceAtLeast(4.dp)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                    .background(MaterialTheme.colorScheme.secondary),
                            ) { }
                            Text(
                                text = fmt.format(item.dayStart),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        if (uiState.dailyObjectiveRetention.any { it.retentionPercent > 0 }) {
            val maxObjective = (uiState.dailyObjectiveRetention.maxOfOrNull { it.retentionPercent } ?: 0)
                .coerceAtLeast(1)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.analytics_chart_objective_accuracy),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.dailyObjectiveRetention.forEach { item ->
                            val heightFrac = item.retentionPercent.toFloat() / maxObjective.toFloat()
                            val barHeight = (120.dp * heightFrac).coerceAtLeast(4.dp)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                        .background(MaterialTheme.colorScheme.tertiary),
                                ) { }
                                Text(
                                    text = fmt.format(item.dayStart),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
