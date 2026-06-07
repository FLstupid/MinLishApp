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
import com.example.minlish.ui.viewmodel.QuizUiState

@Composable
fun QuizScreen(
    viewModel: PracticeViewModel,
    onNext: () -> Unit,
    onGoFlashcard: () -> Unit,
    onGoTyping: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val state by viewModel.quizState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
            OutlinedButton(onClick = onGoTyping) {
                Text(stringResource(R.string.practice_mode_typing))
            }
        }

        when (val s = state) {
            is QuizUiState.Loading -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.quiz_preparing))
            }
            is QuizUiState.Empty -> {
                Text(stringResource(R.string.quiz_empty))
                OutlinedButton(onClick = onOpenLibrary) {
                    Text(stringResource(R.string.open_library))
                }
            }
            is QuizUiState.Question -> {
                Text(
                    text = s.promptWord,
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (!s.promptPronunciation.isNullOrBlank()) {
                    Text(text = s.promptPronunciation!!, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                s.options.forEach { option ->
                    Button(
                        onClick = { viewModel.submitQuizAnswer(option.wordId) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = s.feedback == null,
                    ) {
                        Text(option.meaning)
                    }
                }

                if (s.feedback != null) {
                    Text(
                        text = stringResource(
                            if (s.feedback == true) R.string.quiz_correct else R.string.quiz_incorrect
                        ),
                        color = if (s.feedback == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    s.example?.let {
                        Text(
                            stringResource(R.string.quiz_example, it),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.quiz_next))
                    }
                }
            }
        }
    }
}
