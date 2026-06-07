package com.example.minlish.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.minlish.R
import com.example.minlish.ui.viewmodel.PracticeViewModel
import com.example.minlish.ui.viewmodel.TypingFeedbackKind

@Composable
fun TypingScreen(
    viewModel: PracticeViewModel,
    onNext: () -> Unit,
    onGoFlashcard: () -> Unit,
    onGoQuiz: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val state by viewModel.typingState.collectAsState()
    val targetWord = state.targetWord

    val inputValue = state.userInput

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.practice_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onGoFlashcard) {
                Text(stringResource(R.string.practice_mode_flashcard))
            }
            OutlinedButton(onClick = onGoQuiz) {
                Text(stringResource(R.string.practice_mode_quiz))
            }
        }

        if (targetWord == null) {
            if (state.feedbackKind != null) {
                Text(
                    text = stringResource(R.string.practice_empty),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onOpenLibrary) {
                    Text(stringResource(R.string.open_library))
                }
            } else {
                CircularProgressIndicator()
                Text(stringResource(R.string.typing_preparing))
            }
        } else {
            Text(stringResource(R.string.typing_meaning_label), style = MaterialTheme.typography.titleLarge)
            Text(targetWord.meaning, style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = inputValue,
                onValueChange = { viewModel.onTypingInputChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.typing_input_label)) },
                singleLine = true,
            )

            when (state.feedbackKind) {
                TypingFeedbackKind.Correct -> {
                    Text(
                        text = stringResource(R.string.quiz_correct),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                TypingFeedbackKind.TryAgain -> {
                    Text(
                        text = stringResource(R.string.typing_try_again),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(stringResource(R.string.typing_attempts_left, state.attemptsLeft))
                }
                TypingFeedbackKind.OutOfAttempts -> {
                    Text(
                        text = stringResource(R.string.typing_out_of_attempts, targetWord.word),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                null -> {
                    Text(stringResource(R.string.typing_attempts_left, state.attemptsLeft))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.submitTypingAnswer() },
                    enabled = state.attemptsLeft > 0 && !state.isCorrect,
                ) {
                    Text(stringResource(R.string.typing_check))
                }
                Button(onClick = { onNext() }) {
                    Text(stringResource(R.string.practice_next))
                }
            }
        }
    }
}
