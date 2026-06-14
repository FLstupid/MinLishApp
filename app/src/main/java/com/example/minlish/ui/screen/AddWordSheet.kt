package com.example.minlish.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.minlish.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordSheet(
    onDismiss: () -> Unit,
    onConfirm: (
        word: String,
        pronunciation: String?,
        meaning: String,
        descriptionEn: String?,
        example: String?,
        collocation: String?,
        relatedWords: String?,
        note: String?,
    ) -> Unit,
) {
    var word by remember { mutableStateOf("") }
    var pronunciation by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var descriptionEn by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var collocation by remember { mutableStateOf("") }
    var relatedWords by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showOptional by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    suspend fun dismissSheet() {
        sheetState.hide()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { scope.launch { dismissSheet() } },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 520.dp)
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.word_add_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            validationError?.let { msg ->
                Text(text = msg, color = MaterialTheme.colorScheme.error)
            }

            // ── Required fields ──
            OutlinedTextField(
                value = word,
                onValueChange = { word = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_word)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = meaning,
                onValueChange = { meaning = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_meaning)) },
                singleLine = false,
                minLines = 2,
            )

            // ── Optional toggle ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOptional = !showOptional },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.word_add_optional),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = if (showOptional) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            AnimatedVisibility(visible = showOptional) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = pronunciation,
                        onValueChange = { pronunciation = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.field_pronunciation)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = example,
                        onValueChange = { example = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.field_example)) },
                        singleLine = false,
                        minLines = 2,
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.field_note)) },
                        singleLine = false,
                        minLines = 2,
                    )

                    // ── Advanced toggle ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvanced = !showAdvanced },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.word_add_advanced),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    AnimatedVisibility(visible = showAdvanced) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = descriptionEn,
                                onValueChange = { descriptionEn = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.field_description_en)) },
                                singleLine = false,
                                minLines = 2,
                            )
                            OutlinedTextField(
                                value = collocation,
                                onValueChange = { collocation = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.field_collocation)) },
                                singleLine = false,
                                minLines = 1,
                            )
                            OutlinedTextField(
                                value = relatedWords,
                                onValueChange = { relatedWords = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.field_related_words)) },
                                singleLine = false,
                                minLines = 1,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    if (word.isBlank() || meaning.isBlank()) {
                        validationError = context.getString(R.string.add_word_validation_error)
                        return@Button
                    }
                    validationError = null
                    onConfirm(
                        word.trim(),
                        pronunciation.trim().ifBlank { null },
                        meaning.trim(),
                        descriptionEn.trim().ifBlank { null },
                        example.trim().ifBlank { null },
                        collocation.trim().ifBlank { null },
                        relatedWords.trim().ifBlank { null },
                        note.trim().ifBlank { null },
                    )
                    scope.launch { dismissSheet() }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
