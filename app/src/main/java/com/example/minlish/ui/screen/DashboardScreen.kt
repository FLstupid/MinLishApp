package com.example.minlish.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.minlish.R
import com.example.minlish.ui.viewmodel.DashboardPrimaryCta
import com.example.minlish.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onStartLearning: () -> Unit,
    onStartBonusLearning: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenCheckpoint: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.dashboard_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_daily_progress),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (state.hasDailyQueue) {
                        stringResource(R.string.dashboard_new_review, state.newAvailableCount, state.reviewCount)
                    } else {
                        stringResource(R.string.dashboard_all_caught_up)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Button(
                    onClick = {
                        when (state.primaryCta) {
                            DashboardPrimaryCta.StartDaily -> onStartLearning()
                            DashboardPrimaryCta.StudyMore -> onStartBonusLearning()
                            DashboardPrimaryCta.OpenLibrary -> onOpenLibrary()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    enabled = true
                ) {
                    Text(
                        text = when (state.primaryCta) {
                            DashboardPrimaryCta.StartDaily -> stringResource(R.string.dashboard_start_learning)
                            DashboardPrimaryCta.StudyMore ->
                                stringResource(R.string.dashboard_study_more_batch, state.bonusBatch)
                            DashboardPrimaryCta.OpenLibrary -> stringResource(R.string.dashboard_add_words)
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                if (state.canStudyMore) {
                    OutlinedButton(
                        onClick = onOpenLibrary,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.dashboard_add_words))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatItem(
                label = stringResource(R.string.stat_streak),
                value = "${state.streakDays}${stringResource(R.string.stat_days_suffix)}",
                modifier = Modifier.weight(1f),
                onClick = onOpenAnalytics,
            )
            StatItem(
                label = stringResource(R.string.stat_learned),
                value = stringResource(R.string.stat_learned_in_set, state.learnedCount, state.setWordCount),
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatItem(
                label = stringResource(R.string.stat_accuracy),
                value = "${state.accuracyPercent}%",
                modifier = Modifier.weight(1f),
                onClick = onOpenAnalytics,
            )
            StatItem(
                label = stringResource(R.string.stat_objective_accuracy),
                value = "${state.objectiveAccuracyPercent}%",
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatItem(
                label = stringResource(R.string.stat_level),
                value = state.profileLevelLabel.ifEmpty { analyticsSkillLevelLabel(state.skillLevel) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onOpenAnalytics,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(R.string.view_analytics))
            }
            if (state.showCheckpoint) {
                OutlinedButton(
                    onClick = onOpenCheckpoint,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(R.string.dashboard_checkpoint))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
