package com.example.minlish.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.minlish.R

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 520.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.word_add_title))

            OutlinedTextField(
                value = word,
                onValueChange = { word = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_word)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = pronunciation,
                onValueChange = { pronunciation = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_pronunciation)) },
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
            OutlinedTextField(
                value = descriptionEn,
                onValueChange = { descriptionEn = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_description_en)) },
                singleLine = false,
                minLines = 2,
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
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_note)) },
                singleLine = false,
                minLines = 2,
            )

            Button(
                onClick = {
                    if (word.isBlank() || meaning.isBlank()) return@Button
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
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
