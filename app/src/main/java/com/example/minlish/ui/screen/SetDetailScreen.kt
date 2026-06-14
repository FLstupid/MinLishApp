package com.example.minlish.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.minlish.R
import com.example.minlish.data.model.VocabularySet
import com.example.minlish.data.model.Word
import com.example.minlish.ui.viewmodel.SetUiEvent
import com.example.minlish.ui.viewmodel.SetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetDetailScreen(
    set: VocabularySet,
    words: List<Word>,
    viewModel: SetViewModel,
    onGoLearn: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var csvText by remember { mutableStateOf("") }
    var showGoLearnCta by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
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
                    if (event.showGoLearnCta) showGoLearnCta = true
                }
                SetUiEvent.ImportFailed ->
                    snackbarHostState.showSnackbar(context.getString(R.string.import_failed))
                SetUiEvent.PasteImportEmpty ->
                    snackbarHostState.showSnackbar(context.getString(R.string.import_paste_empty))
                SetUiEvent.WordAdded ->
                    snackbarHostState.showSnackbar(context.getString(R.string.word_saved_ok))
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val mime = context.contentResolver.getType(uri).orEmpty()
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: byteArrayOf()
                if (bytes.isEmpty()) {
                    snackbarHostState.showSnackbar(context.getString(R.string.import_failed))
                    return@launch
                }
                val isXlsx = mime.contains("spreadsheet") ||
                    uri.toString().lowercase().endsWith(".xlsx")
                if (isXlsx) {
                    viewModel.importXlsxBytes(bytes)
                } else {
                    viewModel.importCsvBytes(bytes)
                }
            }
        },
    )

    fun launchImportFile() {
        importFileLauncher.launch(
            arrayOf(
                "text/csv",
                "application/csv",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel",
            ),
        )
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    val csv = viewModel.buildExportCsv(words)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(csv.toByteArray(Charsets.UTF_8))
                        }
                    }
                    snackbarHostState.showSnackbar(context.getString(R.string.export_csv_ok))
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, context.getString(R.string.share_csv)),
                    )
                } catch (_: Throwable) {
                    snackbarHostState.showSnackbar(context.getString(R.string.export_csv_fail))
                }
            }
        },
    )

    val exportXlsxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        ),
        onResult = { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    val xlsx = viewModel.buildExportXlsx(words)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(xlsx)
                        }
                    }
                    snackbarHostState.showSnackbar(context.getString(R.string.export_xlsx_ok))
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, context.getString(R.string.share_xlsx)),
                    )
                } catch (_: Throwable) {
                    snackbarHostState.showSnackbar(context.getString(R.string.export_xlsx_fail))
                }
            }
        },
    )

    if (showAdd) {
        AddWordSheet(
            onDismiss = { showAdd = false },
            onConfirm = { word, pronunciation, meaning, descriptionEn, example, collocation, relatedWords, note ->
                viewModel.addWordToSelectedSet(
                    word = word,
                    pronunciation = pronunciation,
                    meaning = meaning,
                    descriptionEn = descriptionEn,
                    example = example,
                    collocation = collocation,
                    relatedWords = relatedWords,
                    note = note,
                )
            },
        )
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text(stringResource(R.string.import_csv_title)) },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(scrollState)
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.import_csv_help))
                    OutlinedTextField(
                        value = csvText,
                        onValueChange = { csvText = it },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        minLines = 6,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (csvText.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.import_paste_empty),
                                )
                            }
                            return@Button
                        }
                        viewModel.importCsvText(csvText)
                        showImport = false
                        csvText = ""
                    },
                ) { Text(stringResource(R.string.action_import)) }
            },
            dismissButton = {
                TextButton(onClick = { showImport = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { showImport = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.set_paste_csv))
                    }
                    Button(
                        onClick = { launchImportFile() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.set_import_file))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { exportCsvLauncher.launch("${set.title}.csv") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.set_export_csv))
                    }
                    Button(
                        onClick = { exportXlsxLauncher.launch("${set.title}.xlsx") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.set_export_xlsx))
                    }
                }

                Button(onClick = { showAdd = true }) {
                    Text(stringResource(R.string.set_add_word))
                }

                if (showGoLearnCta) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onGoLearn,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.set_learn_now))
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (words.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.set_no_words))
                    OutlinedButton(onClick = { showAdd = true }) {
                        Text(stringResource(R.string.set_add_word))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(words, key = { it.id }) { word ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = word.word,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = word.meaning,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (!word.example.isNullOrBlank()) {
                                        Text(
                                            text = word.example.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteWord(word) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = stringResource(R.string.cd_delete_word),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
