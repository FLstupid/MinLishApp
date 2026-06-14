package com.example.minlish.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minlish.R
import com.example.minlish.ui.components.Flashcard
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashcardPractice(
    viewModel: PracticeViewModel,
    onGoQuiz: () -> Unit,
    onGoTyping: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val flashcard by viewModel.flashcardUiState.collectAsState()

    // Set selector state
    val availableSets by viewModel.availableSets.collectAsState()
    val selectedSetId by viewModel.selectedSetId.collectAsState()
    val currentSetName by viewModel.currentSetName.collectAsState()
    var setDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Set selector dropdown ──
            ExposedDropdownMenuBox(
                expanded = setDropdownExpanded,
                onExpandedChange = { setDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = currentSetName ?: "",
                    onValueChange = { },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    label = { Text(stringResource(R.string.practice_set_label)) },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = setDropdownExpanded) },
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = setDropdownExpanded,
                    onDismissRequest = { setDropdownExpanded = false },
                ) {
                    availableSets.forEach { set ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = set.title,
                                    color = if (set.id == selectedSetId) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            },
                            onClick = {
                                viewModel.switchPracticeSet(set.id)
                                setDropdownExpanded = false
                            },
                        )
                    }
                }
            }

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
                val word = flashcard.word ?: return@Column

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
                        Color(0xFFFFF3E0), // Orange-50 container
                        enabled = !flashcard.isReviewSaving,
                    ) { viewModel.onPracticeReview(3) }
                    PracticeSrsButton(
                        stringResource(R.string.srs_good),
                        Color(0xFFE8F5E9), // Green-50 container
                        enabled = !flashcard.isReviewSaving,
                    ) { viewModel.onPracticeReview(4) }
                    PracticeSrsButton(
                        stringResource(R.string.srs_easy),
                        Color(0xFFE3F2FD), // Blue-50 container
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
        modifier = Modifier
            .weight(1f)
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColorFor(containerColor),
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
