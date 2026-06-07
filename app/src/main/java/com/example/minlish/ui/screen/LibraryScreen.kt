package com.example.minlish.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.minlish.R
import com.example.minlish.data.model.VocabularySet
import com.example.minlish.ui.viewmodel.SetUiEvent
import com.example.minlish.ui.viewmodel.SetViewModel

@Composable
fun LibraryScreen(
    setViewModel: SetViewModel,
    profileGoal: String,
    onGoLearn: () -> Unit,
) {
    val recommendedPackId = setViewModel.recommendedPackIdForGoal(profileGoal)
    val sets by setViewModel.sets.collectAsState()
    val selectedSetId by setViewModel.selectedSetId.collectAsState()
    val words by setViewModel.wordsInSelectedSet.collectAsState()

    var showCreateSet by remember { mutableStateOf(false) }
    var setTitle by remember { mutableStateOf("") }
    var setDescription by remember { mutableStateOf("") }
    var setTags by remember { mutableStateOf("") }

    var showEditSet by remember { mutableStateOf(false) }
    var editSet by remember { mutableStateOf<VocabularySet?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf("") }

    var showDeleteSet by remember { mutableStateOf(false) }
    var deleteSet by remember { mutableStateOf<VocabularySet?>(null) }

    val selectedId = selectedSetId
    if (selectedId != null) {
        val selectedSet = sets.firstOrNull { it.id == selectedId } ?: VocabularySet(
            id = selectedId,
            title = stringResource(R.string.set_fallback_title),
            description = null,
            tags = "",
            wordCount = 0,
            createdAt = 0L,
            userId = ""
        )

        SetDetailScreen(
            set = selectedSet,
            words = words,
            viewModel = setViewModel,
            onGoLearn = onGoLearn,
        )
        return
    }

    val installedPackIds by setViewModel.installedStarterPackIds.collectAsState()
    val starterPacks = setViewModel.starterPacks
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(setViewModel) {
        setViewModel.uiEvents.collect { event ->
            when (event) {
                is SetUiEvent.Snackbar -> snackbarHostState.showSnackbar(event.message)
                is SetUiEvent.ImportCompleted -> {
                    snackbarHostState.showSnackbar(
                        context.getString(
                            R.string.import_stats,
                            event.stats.inserted,
                            event.stats.merged,
                            event.stats.skipped,
                            event.stats.errors,
                        ),
                    )
                }
                SetUiEvent.ImportFailed ->
                    snackbarHostState.showSnackbar(context.getString(R.string.import_failed))
                SetUiEvent.PasteImportEmpty ->
                    snackbarHostState.showSnackbar(context.getString(R.string.import_paste_empty))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.library_starter_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.library_starter_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(starterPacks, key = { "starter-${it.id}" }) { pack ->
                val installed = pack.id in installedPackIds
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(pack.title, style = MaterialTheme.typography.titleMedium)
                        if (pack.id == recommendedPackId && !installed) {
                            Text(
                                text = stringResource(R.string.library_starter_recommended),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            pack.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        if (installed) {
                            Text(
                                text = stringResource(R.string.library_starter_installed),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Button(
                                onClick = { setViewModel.installStarterPack(pack.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.library_add_starter))
                            }
                        }
                    }
                }
            }

            if (sets.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.library_your_sets),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            items(sets, key = { it.id }) { set ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(set.title, style = MaterialTheme.typography.titleMedium)
                        if (!set.description.isNullOrBlank()) {
                            Text(
                                set.description!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val wordCount by setViewModel.wordCountForSet(set.id).collectAsState(initial = 0)
                        Text(
                            stringResource(R.string.library_word_count, wordCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { setViewModel.selectSet(set.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(stringResource(R.string.library_open))
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editSet = set
                                    editTitle = set.title
                                    editDescription = set.description.orEmpty()
                                    editTags = set.tags
                                    showEditSet = true
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.library_edit))
                            }
                            OutlinedButton(
                                onClick = {
                                    deleteSet = set
                                    showDeleteSet = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text(stringResource(R.string.library_delete))
                            }
                        }
                    }
                }
            }

            if (sets.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.library_empty_custom_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { showCreateSet = true }) {
                        Text(stringResource(R.string.library_create_set))
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
        )

        FloatingActionButton(
            onClick = { showCreateSet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.large,
        ) {
            Text("+", fontWeight = FontWeight.Bold)
        }
    }

    if (showCreateSet) {
        AlertDialog(
            onDismissRequest = { showCreateSet = false },
            title = { Text(stringResource(R.string.library_create_set_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = setTitle,
                        onValueChange = { setTitle = it },
                        label = { Text(stringResource(R.string.field_title)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = setDescription,
                        onValueChange = { setDescription = it },
                        label = { Text(stringResource(R.string.field_description)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    OutlinedTextField(
                        value = setTags,
                        onValueChange = { setTags = it },
                        label = { Text(stringResource(R.string.field_tags_csv)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (setTitle.isBlank()) return@Button
                        setViewModel.addSet(
                            title = setTitle,
                            description = setDescription.ifBlank { null },
                            tags = setTags
                        )
                        showCreateSet = false
                        setTitle = ""
                        setDescription = ""
                        setTags = ""
                    },
                ) {
                    Text(stringResource(R.string.action_create))
                }
            },
            dismissButton = { TextButton(onClick = { showCreateSet = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (showEditSet && editSet != null) {
        AlertDialog(
            onDismissRequest = { showEditSet = false },
            title = { Text(stringResource(R.string.library_edit_set_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text(stringResource(R.string.field_title)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text(stringResource(R.string.field_description)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    OutlinedTextField(
                        value = editTags,
                        onValueChange = { editTags = it },
                        label = { Text(stringResource(R.string.field_tags_csv)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = editSet ?: return@Button
                        if (editTitle.isBlank()) return@Button
                        setViewModel.updateSet(
                            current.copy(
                                title = editTitle.trim(),
                                description = editDescription.ifBlank { null },
                                tags = editTags.trim(),
                            )
                        )
                        showEditSet = false
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = { TextButton(onClick = { showEditSet = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (showDeleteSet && deleteSet != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSet = false },
            title = { Text(stringResource(R.string.library_delete_set_title)) },
            text = { Text(stringResource(R.string.library_delete_set_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        val current = deleteSet ?: return@Button
                        setViewModel.deleteSet(current)
                        showDeleteSet = false
                        deleteSet = null
                        if (selectedSetId == current.id) {
                            setViewModel.clearSelection()
                        }
                    },
                ) {
                    Text(stringResource(R.string.library_delete))
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteSet = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
