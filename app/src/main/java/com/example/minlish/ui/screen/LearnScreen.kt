package com.example.minlish.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minlish.R
import com.example.minlish.ui.components.Flashcard
import com.example.minlish.ui.components.TtsUnavailableSnackbarEffect
import com.example.minlish.ui.viewmodel.LearnUiState
import com.example.minlish.ui.viewmodel.WordViewModel
import kotlin.math.min

@Composable
fun LearnScreen(
    viewModel: WordViewModel,
    onOpenLibrary: () -> Unit,
    onGoHome: () -> Unit,
    onGoPractice: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAnswering by viewModel.isAnswering.collectAsState()
    val poolRemainingCount by viewModel.poolRemainingCount.collectAsState()
    val speakEnabled by viewModel.speakEnabled.collectAsState()
    val speakLoading by viewModel.speakLoading.collectAsState()
    val bonusBatch = min(viewModel.bonusBatchSize(), poolRemainingCount)
    val snackbarHostState = remember { SnackbarHostState() }

    TtsUnavailableSnackbarEffect(viewModel.uiEvents, snackbarHostState)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is LearnUiState.Loading -> {
                CircularProgressIndicator()
            }
            is LearnUiState.EmptyNoWords -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.learn_empty_no_words),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.learn_empty_no_words_hint),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onOpenLibrary) {
                        Text(stringResource(R.string.open_library))
                    }

                }
            }
            is LearnUiState.EmptyAllCaughtUp -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.learn_empty_caught_up),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.learn_empty_caught_up_hint),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (poolRemainingCount > 0) {
                        Button(onClick = { viewModel.startBonusSession() }) {
                            Text(stringResource(R.string.learn_study_more_batch, bonusBatch))
                        }
                    }
                    Button(onClick = onGoPractice) {
                        Text(stringResource(R.string.go_practice))
                    }
                    OutlinedButton(onClick = onGoHome) {
                        Text(stringResource(R.string.go_home))
                    }

                }
            }
            is LearnUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.learn_mode_srs),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = {
                                if (state.uniqueTotal <= 0) {
                                    0f
                                } else {
                                    state.uniqueIndex.toFloat() / state.uniqueTotal.toFloat()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(8.dp).padding(horizontal = 8.dp),
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = stringResource(
                                R.string.learn_unique_progress,
                                state.uniqueIndex,
                                state.uniqueTotal,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.cardTotal > state.uniqueTotal) {
                            Text(
                                text = stringResource(
                                    R.string.learn_card_progress,
                                    state.cardIndex,
                                    state.cardTotal,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        key(state.word.id) {
                            Flashcard(
                                word = state.word,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.learn_tap_flip),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        OutlinedButton(
                            onClick = { viewModel.onSpeakCurrentWord() },
                            enabled = speakEnabled,
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                        ) {
                            if (speakLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(stringResource(R.string.practice_speak))
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (isAnswering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(
                            text = stringResource(R.string.learn_rate_prompt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SRSButton(
                                stringResource(R.string.srs_again),
                                MaterialTheme.colorScheme.errorContainer,
                                enabled = !isAnswering,
                            ) { viewModel.onAnswer(0) }
                            SRSButton(
                                stringResource(R.string.srs_hard),
                                MaterialTheme.colorScheme.secondaryContainer,
                                enabled = !isAnswering,
                            ) { viewModel.onAnswer(3) }
                            SRSButton(
                                stringResource(R.string.srs_good),
                                MaterialTheme.colorScheme.primaryContainer,
                                enabled = !isAnswering,
                            ) { viewModel.onAnswer(4) }
                            SRSButton(
                                stringResource(R.string.srs_easy),
                                MaterialTheme.colorScheme.tertiaryContainer,
                                enabled = !isAnswering,
                            ) { viewModel.onAnswer(5) }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun RowScope.SRSButton(
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColorFor(containerColor)
        ),
        contentPadding = PaddingValues(0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
