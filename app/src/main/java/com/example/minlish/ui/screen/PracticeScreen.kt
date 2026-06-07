package com.example.minlish.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minlish.R
import com.example.minlish.ui.components.Flashcard
import com.example.minlish.ui.components.TtsUnavailableSnackbarEffect
import com.example.minlish.ui.viewmodel.PracticeViewModel

enum class PracticeMode { Flashcard, Quiz, Typing }

@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel,
    onOpenLibrary: () -> Unit,
) {
    var mode by remember { mutableStateOf(PracticeMode.Flashcard) }

    when (mode) {
        PracticeMode.Flashcard -> {
            FlashcardPractice(
                viewModel = viewModel,
                onGoQuiz = { mode = PracticeMode.Quiz },
                onGoTyping = { mode = PracticeMode.Typing },
                onOpenLibrary = onOpenLibrary,
            )
        }
        PracticeMode.Quiz -> {
            QuizScreen(
                viewModel = viewModel,
                onNext = { viewModel.nextQuiz() },
                onGoFlashcard = { mode = PracticeMode.Flashcard },
                onGoTyping = { mode = PracticeMode.Typing },
                onOpenLibrary = onOpenLibrary,
            )
        }
        PracticeMode.Typing -> {
            TypingScreen(
                viewModel = viewModel,
                onNext = { viewModel.nextTyping() },
                onGoFlashcard = { mode = PracticeMode.Flashcard },
                onGoQuiz = { mode = PracticeMode.Quiz },
                onOpenLibrary = onOpenLibrary,
            )
        }
    }
}

@Composable
private fun FlashcardPractice(
    viewModel: PracticeViewModel,
    onGoQuiz: () -> Unit,
    onGoTyping: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val flashcard by viewModel.flashcardUiState.collectAsState()

    TtsUnavailableSnackbarEffect(viewModel.uiEvents, snackbarHostState)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.practice_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onGoQuiz) { Text(stringResource(R.string.practice_mode_quiz)) }
                Button(onClick = onGoTyping) { Text(stringResource(R.string.practice_mode_typing)) }
            }

            if (flashcard.word == null) {
                Text(stringResource(R.string.practice_empty))
                OutlinedButton(onClick = onOpenLibrary) {
                    Text(stringResource(R.string.open_library))
                }
            } else {
                val word = flashcard.word!!

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { viewModel.previousFlashcard() },
                        enabled = flashcard.canNavigate,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.practice_prev_word),
                        )
                    }
                    if (flashcard.cardTotal > 0) {
                        Text(
                            text = stringResource(
                                R.string.practice_flashcard_progress,
                                flashcard.cardOneBased,
                                flashcard.cardTotal,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.nextFlashcard() },
                        enabled = flashcard.canNavigate,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.practice_next_word),
                        )
                    }
                }

                Flashcard(
                    word = word,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.onSpeakFlashcardClicked() },
                        enabled = flashcard.speakEnabled,
                    ) {
                        if (flashcard.speakLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.practice_speak))
                        }
                    }
                }

                if (flashcard.isReviewSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }

                Text(
                    text = stringResource(R.string.practice_srs_schedule_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = stringResource(R.string.learn_rate_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PracticeSrsButton(
                        stringResource(R.string.srs_again),
                        MaterialTheme.colorScheme.errorContainer,
                        enabled = !flashcard.isReviewSaving,
                    ) { viewModel.onPracticeReview(0) }
                    PracticeSrsButton(
                        stringResource(R.string.srs_hard),
                        MaterialTheme.colorScheme.secondaryContainer,
                        enabled = !flashcard.isReviewSaving,
                    ) { viewModel.onPracticeReview(3) }
                    PracticeSrsButton(
                        stringResource(R.string.srs_good),
                        MaterialTheme.colorScheme.primaryContainer,
                        enabled = !flashcard.isReviewSaving,
                    ) { viewModel.onPracticeReview(4) }
                    PracticeSrsButton(
                        stringResource(R.string.srs_easy),
                        MaterialTheme.colorScheme.tertiaryContainer,
                        enabled = !flashcard.isReviewSaving,
                    ) { viewModel.onPracticeReview(5) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.PracticeSrsButton(
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColorFor(containerColor),
        ),
        contentPadding = PaddingValues(0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
