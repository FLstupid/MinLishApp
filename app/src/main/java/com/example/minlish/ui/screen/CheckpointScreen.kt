package com.example.minlish.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.minlish.R
import com.example.minlish.ui.viewmodel.CheckpointUiState
import com.example.minlish.ui.viewmodel.CheckpointViewModel

@Composable
fun CheckpointScreen(
    viewModel: CheckpointViewModel,
    autoStart: Boolean,
    onClose: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(autoStart) {
        if (autoStart) {
            viewModel.startSession()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.checkpoint_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        when (val s = state) {
            is CheckpointUiState.Loading -> {
                if (!autoStart) {
                    Button(onClick = { viewModel.startSession() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.checkpoint_start))
                    }
                    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_cancel))
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
            is CheckpointUiState.NotEnoughWords -> {
                Text(
                    text = stringResource(R.string.checkpoint_not_enough),
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.open_library))
                }
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_close))
                }
            }
            is CheckpointUiState.Question -> {
                Text(
                    text = stringResource(R.string.checkpoint_progress, s.index, s.total),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = s.promptWord, style = MaterialTheme.typography.headlineSmall)
                if (!s.promptPronunciation.isNullOrBlank()) {
                    Text(text = s.promptPronunciation, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                s.options.forEach { option ->
                    Button(
                        onClick = { viewModel.submitAnswer(option.wordId) },
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
                        color = if (s.feedback == true) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    s.example?.let {
                        Text(
                            stringResource(R.string.quiz_example, it),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Button(onClick = { viewModel.nextQuestion() }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (s.index >= s.total) {
                                stringResource(R.string.checkpoint_finish)
                            } else {
                                stringResource(R.string.quiz_next)
                            }
                        )
                    }
                }
            }
            is CheckpointUiState.Finished -> {
                Text(
                    text = stringResource(R.string.checkpoint_result, s.correct, s.total),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                val percent = if (s.total == 0) 0 else (100 * s.correct / s.total)
                Text(
                    text = stringResource(R.string.checkpoint_result_percent, percent),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = {
                        viewModel.dismissFinished()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_close))
                }
            }
        }
    }
}
