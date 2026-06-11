package com.example.minlish

import android.Manifest
import android.os.Bundle
import kotlinx.coroutines.flow.map
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.minlish.R
import com.example.minlish.logic.CefrLevels
import com.example.minlish.ui.navigation.AppDestinations
import com.example.minlish.ui.screen.AnalyticsScreen
import com.example.minlish.ui.screen.AuthScreen
import com.example.minlish.ui.screen.CheckpointScreen
import com.example.minlish.ui.screen.DashboardScreen
import com.example.minlish.ui.screen.LearnScreen
import com.example.minlish.ui.screen.LibraryScreen
import com.example.minlish.ui.screen.PracticeScreen
import com.example.minlish.ui.screen.ProfileScreen
import com.example.minlish.ui.theme.MinLishTheme
import com.example.minlish.ui.viewmodel.AnalyticsViewModel
import com.example.minlish.ui.viewmodel.AnalyticsViewModelFactory
import com.example.minlish.ui.viewmodel.AuthViewModel
import com.example.minlish.ui.viewmodel.AuthViewModelFactory
import com.example.minlish.ui.viewmodel.CheckpointViewModel
import com.example.minlish.ui.viewmodel.CheckpointViewModelFactory
import com.example.minlish.ui.viewmodel.DashboardViewModel
import com.example.minlish.ui.viewmodel.DashboardViewModelFactory
import com.example.minlish.ui.viewmodel.PracticeViewModel
import com.example.minlish.ui.viewmodel.PracticeViewModelFactory
import com.example.minlish.ui.viewmodel.ProfileViewModel
import com.example.minlish.ui.viewmodel.ProfileViewModelFactory
import com.example.minlish.ui.viewmodel.SetViewModel
import com.example.minlish.ui.viewmodel.SetViewModelFactory
import com.example.minlish.ui.viewmodel.WordViewModel
import com.example.minlish.ui.viewmodel.WordViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // KÃ­ch hoáº¡t hiá»ƒn thá»‹ trÃ n viá»n
        enableEdgeToEdge()

        val initialDestination = when (intent.getStringExtra(EXTRA_OPEN_DESTINATION)) {
            "LEARN" -> AppDestinations.LEARN
            else -> AppDestinations.DASHBOARD
        }
        setContent {
            MinLishTheme {
                NotificationPermissionEffect()
                MinLishRoot(initialDestination = initialDestination)
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_DESTINATION = "openDestination"
    }
}

@Composable
fun NotificationPermissionEffect() {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }
    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinLishRoot(initialDestination: AppDestinations = AppDestinations.DASHBOARD) {
    val context = LocalContext.current
    val app = context.applicationContext as MinLishApplication

    val webClientId = try {
        // `default_web_client_id` Ä‘Æ°á»£c táº¡o bá»Ÿi google-services plugin (khÃ´ng pháº£i luÃ´n náº±m trong `R.string`).
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName,
        )
        if (resId != 0) context.getString(resId) else ""
    } catch (_: Throwable) {
        ""
    }

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            firebaseAuth = app.firebaseAuth,
            userRepository = app.userRepository,
            userPreferencesRepository = app.userPreferencesRepository,
            vocabSetRepository = app.vocabSetRepository,
        )
    )

    val currentUser by authViewModel.currentUser.collectAsState()

    if (currentUser == null) {
        AuthScreen(viewModel = authViewModel, webClientId = webClientId)
        return
    }

    val wordViewModel: WordViewModel = viewModel(
        factory = WordViewModelFactory(
            app.repository,
            app.vocabSetRepository,
            app.studySessionRepository,
            app.userPreferencesRepository,
        )
    )

    val setViewModel: SetViewModel = viewModel(
        factory = SetViewModelFactory(
            app.vocabSetRepository,
            app.repository,
            app.userPreferencesRepository,
            app.starterPackInstaller,
        )
    )

    val practiceViewModel: PracticeViewModel = viewModel(
        factory = PracticeViewModelFactory(
            app.repository,
            app.vocabSetRepository,
            app.userPreferencesRepository,
            app.studySessionRepository,
        )
    )

    val analyticsViewModel: AnalyticsViewModel = viewModel(
        factory = AnalyticsViewModelFactory(
            app.studySessionRepository,
            app.repository,
            app.vocabSetRepository,
            app.userPreferencesRepository
        )
    )

    val checkpointViewModel: CheckpointViewModel = viewModel(
        factory = CheckpointViewModelFactory(
            app.repository,
            app.vocabSetRepository,
            app.userPreferencesRepository,
            app.studySessionRepository,
        )
    )

    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(app.userPreferencesRepository),
    )
    val profileLevelFlow = authViewModel.profile.map { it?.level }
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(wordViewModel, analyticsViewModel, profileLevelFlow),
    )

    MinLishTabs(
        wordViewModel = wordViewModel,
        authViewModel = authViewModel,
        setViewModel = setViewModel,
        practiceViewModel = practiceViewModel,
        analyticsViewModel = analyticsViewModel,
        checkpointViewModel = checkpointViewModel,
        profileViewModel = profileViewModel,
        dashboardViewModel = dashboardViewModel,
        initialDestination = initialDestination,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinLishTabs(
    wordViewModel: WordViewModel,
    authViewModel: AuthViewModel,
    setViewModel: SetViewModel,
    practiceViewModel: PracticeViewModel,
    analyticsViewModel: AnalyticsViewModel,
    checkpointViewModel: CheckpointViewModel,
    profileViewModel: ProfileViewModel,
    dashboardViewModel: DashboardViewModel,
    initialDestination: AppDestinations = AppDestinations.DASHBOARD,
) {
    var currentDestination by rememberSaveable { mutableStateOf(initialDestination) }
    var showAnalytics by rememberSaveable { mutableStateOf(false) }
    var skipNextLearnRefresh by remember { mutableStateOf(false) }
    var showCheckpoint by rememberSaveable { mutableStateOf(false) }
    var checkpointAutoStart by rememberSaveable { mutableStateOf(false) }

    val offerCheckpoint by wordViewModel.offerCheckpoint.collectAsState()

    val analyticsUiState by analyticsViewModel.uiState.collectAsState()
    val selectedSetId by setViewModel.selectedSetId.collectAsState()
    val sets by setViewModel.sets.collectAsState()
    val profile by authViewModel.profile.collectAsState()
    val dailyGoal = profile?.dailyGoal ?: 10
    val userLevel = profile?.level ?: CefrLevels.DEFAULT_LEVEL

    LaunchedEffect(dailyGoal) {
        wordViewModel.setDailyGoal(dailyGoal)
    }

    LaunchedEffect(userLevel) {
        wordViewModel.setUserLevel(userLevel)
    }

    LaunchedEffect(currentDestination) {
        if (currentDestination == AppDestinations.LEARN) {
            if (skipNextLearnRefresh) {
                skipNextLearnRefresh = false
            } else {
                wordViewModel.refreshDailyQueueIfIdle()
            }
        }
    }

    if (offerCheckpoint) {
        AlertDialog(
            onDismissRequest = {
                wordViewModel.consumeCheckpointOffer()
            },
            title = { Text(stringResource(R.string.checkpoint_offer_title)) },
            text = { Text(stringResource(R.string.checkpoint_offer_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        wordViewModel.consumeCheckpointOffer()
                        checkpointAutoStart = true
                        showCheckpoint = true
                    },
                ) {
                    Text(stringResource(R.string.checkpoint_offer_start))
                }
            },
            dismissButton = {
                TextButton(onClick = { wordViewModel.consumeCheckpointOffer() }) {
                    Text(stringResource(R.string.checkpoint_offer_skip))
                }
            },
        )
    }

    val showSetDetail = currentDestination == AppDestinations.VOCABULARY && selectedSetId != null
    val selectedSetTitle = if (selectedSetId != null) {
        sets.firstOrNull { it.id == selectedSetId }?.title ?: stringResource(R.string.set_fallback_title)
    } else {
        ""
    }

    NavigationSuiteScaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // TrÃ¡nh camera
            .navigationBarsPadding(), // TrÃ¡nh thanh Ä‘iá»u hÆ°á»›ng dÆ°á»›i
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.labelRes),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                    selected = destination == currentDestination,
                    onClick = {
                        currentDestination = destination
                        showAnalytics = false
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (showSetDetail) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = selectedSetTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { setViewModel.clearSelection() },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_close),
                                )
                            }
                        },
                    )
                } else if (currentDestination == AppDestinations.DASHBOARD && showAnalytics) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.analytics_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { showAnalytics = false },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_close),
                                )
                            }
                        },
                    )
                } else {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = stringResource(currentDestination.labelRes),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                if (showCheckpoint) {
                    CheckpointScreen(
                        viewModel = checkpointViewModel,
                        autoStart = checkpointAutoStart,
                        onClose = {
                            showCheckpoint = false
                            checkpointAutoStart = false
                        },
                        onOpenLibrary = {
                            showCheckpoint = false
                            checkpointAutoStart = false
                            currentDestination = AppDestinations.VOCABULARY
                        },
                    )
                } else when (currentDestination) {
                    AppDestinations.DASHBOARD -> {
                        if (showAnalytics) {
                            AnalyticsScreen(uiState = analyticsUiState, showTitle = false)
                        } else {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onStartLearning = {
                                    showAnalytics = false
                                    currentDestination = AppDestinations.LEARN
                                },
                                onStartBonusLearning = {
                                    showAnalytics = false
                                    skipNextLearnRefresh = true
                                    wordViewModel.startBonusSession()
                                    currentDestination = AppDestinations.LEARN
                                },
                                onOpenLibrary = {
                                    showAnalytics = false
                                    currentDestination = AppDestinations.VOCABULARY
                                },
                                onOpenAnalytics = { showAnalytics = true },
                                onOpenCheckpoint = {
                                    checkpointAutoStart = false
                                    showCheckpoint = true
                                },
                            )
                        }
                    }
                    AppDestinations.VOCABULARY -> LibraryScreen(
                        setViewModel = setViewModel,
                        profileGoal = profile?.goal.orEmpty(),
                        onGoLearn = {
                            showAnalytics = false
                            currentDestination = AppDestinations.LEARN
                        },
                    )
                    AppDestinations.LEARN -> LearnScreen(
                        viewModel = wordViewModel,
                        onOpenLibrary = {
                            showAnalytics = false
                            currentDestination = AppDestinations.VOCABULARY
                        },
                        onGoHome = {
                            showAnalytics = false
                            currentDestination = AppDestinations.DASHBOARD
                        },
                        onGoPractice = {
                            showAnalytics = false
                            currentDestination = AppDestinations.PRACTICE
                        },
                    )
                    AppDestinations.PRACTICE -> PracticeScreen(
                        viewModel = practiceViewModel,
                        onOpenLibrary = {
                            showAnalytics = false
                            currentDestination = AppDestinations.VOCABULARY
                        },
                    )
                    AppDestinations.PROFILE -> ProfileScreen(authViewModel, profileViewModel)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MinLishAppPreview() {
    MinLishTheme {
        MinLishRoot()
    }
}
