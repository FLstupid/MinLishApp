package com.example.minlish

import android.Manifest
import android.os.Bundle
import kotlinx.coroutines.flow.map
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.minlish.R
import com.example.minlish.data.model.VocabularySet
import com.example.minlish.logic.CefrLevels
import com.example.minlish.ui.navigation.AppDestinations
import com.example.minlish.ui.navigation.Routes
import com.example.minlish.ui.screen.AnalyticsScreen
import com.example.minlish.ui.screen.AuthScreen
import com.example.minlish.ui.screen.CheckpointScreen
import com.example.minlish.ui.screen.DashboardScreen
import com.example.minlish.ui.screen.LearnScreen
import com.example.minlish.ui.screen.LibraryScreen
import com.example.minlish.ui.screen.PracticeScreen
import com.example.minlish.ui.screen.ProfileScreen
import com.example.minlish.ui.screen.SetDetailScreen
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
            "LEARN" -> Routes.LEARN
            else -> Routes.DASHBOARD
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
fun MinLishRoot(initialDestination: String = Routes.DASHBOARD) {
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
        initialRoute = initialDestination,
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
    initialRoute: String = Routes.DASHBOARD,
) {
    val navController = rememberNavController()
    val skipNextLearnRefresh = remember { mutableStateOf(false) }

    val offerCheckpoint by wordViewModel.offerCheckpoint.collectAsState()
    val analyticsUiState by analyticsViewModel.uiState.collectAsState()
    val profile by authViewModel.profile.collectAsState()
    val sets by setViewModel.sets.collectAsState()

    val dailyGoal = profile?.dailyGoal ?: 10
    val userLevel = profile?.level ?: CefrLevels.DEFAULT_LEVEL

    LaunchedEffect(dailyGoal) { wordViewModel.setDailyGoal(dailyGoal) }
    LaunchedEffect(userLevel) { wordViewModel.setUserLevel(userLevel) }

    // ── Observe destination changes to refresh learn queue ──
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        if (currentRoute == Routes.LEARN) {
            if (skipNextLearnRefresh.value) {
                skipNextLearnRefresh.value = false
            } else {
                wordViewModel.refreshDailyQueueIfIdle()
            }
        }
    }

    // ── Checkpoint auto-offer dialog ──
    if (offerCheckpoint) {
        AlertDialog(
            onDismissRequest = { wordViewModel.consumeCheckpointOffer() },
            title = { Text(stringResource(R.string.checkpoint_offer_title)) },
            text = { Text(stringResource(R.string.checkpoint_offer_message)) },
            confirmButton = {
                TextButton(onClick = {
                    wordViewModel.consumeCheckpointOffer()
                    navController.navigate(Routes.CHECKPOINT)
                }) {
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

    // ── Top bar title / back logic ──
    val isMainRoute = currentRoute in AppDestinations.entries.map { it.route }
    val showBackButton = currentRoute == Routes.ANALYTICS ||
        (currentRoute?.startsWith("library/") == true && currentRoute != Routes.LIBRARY)
    val topBarTitle = when {
        currentRoute?.startsWith("library/") == true && currentRoute != Routes.LIBRARY -> {
            val setId = currentRoute.removePrefix("library/").toLongOrNull()
            sets.firstOrNull { it.id == setId }?.title
                ?: stringResource(R.string.set_fallback_title)
        }
        currentRoute == Routes.ANALYTICS -> stringResource(R.string.analytics_title)
        else -> currentRoute?.let { route ->
            AppDestinations.entries.find { it.route == route }?.labelRes
        }?.let { stringResource(it) }.orEmpty()
    }

    NavigationSuiteScaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.labelRes),
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                    selected = currentRoute == destination.route,
                    onClick = {
                        if (currentRoute != destination.route) {
                            navController.navigate(destination.route) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                // Only show top bar for non-checkpoint routes
                if (currentRoute != Routes.CHECKPOINT) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = topBarTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        navigationIcon = {
                            if (showBackButton) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.action_close),
                                    )
                                }
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = initialRoute,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onStartLearning = {
                            navController.navigate(Routes.LEARN) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onStartBonusLearning = {
                            skipNextLearnRefresh.value = true
                            wordViewModel.startBonusSession()
                            navController.navigate(Routes.LEARN) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenLibrary = {
                            navController.navigate(Routes.LIBRARY) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenAnalytics = { navController.navigate(Routes.ANALYTICS) },
                        onOpenCheckpoint = { navController.navigate(Routes.CHECKPOINT) },
                    )
                }

                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        setViewModel = setViewModel,
                        profileGoal = profile?.goal.orEmpty(),
                        onGoLearn = {
                            navController.navigate(Routes.LEARN) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onSetSelected = { setId ->
                            navController.navigate(Routes.setDetail(setId))
                        },
                    )
                }

                composable(
                    route = Routes.SET_DETAIL,
                    arguments = listOf(navArgument("setId") { type = NavType.LongType }),
                ) { backStackEntry ->
                    val setId = backStackEntry.arguments?.getLong("setId") ?: return@composable
                    LaunchedEffect(setId) { setViewModel.selectSet(setId) }
                    DisposableEffect(Unit) { onDispose { setViewModel.clearSelection() } }

                    val words by setViewModel.wordsInSelectedSet.collectAsState()
                    val selectedSet = sets.firstOrNull { it.id == setId } ?: VocabularySet(
                        id = setId,
                        title = stringResource(R.string.set_fallback_title),
                        description = null,
                        tags = "",
                        wordCount = 0,
                        createdAt = 0L,
                        userId = "",
                    )

                    SetDetailScreen(
                        set = selectedSet,
                        words = words,
                        viewModel = setViewModel,
                        onGoLearn = {
                            navController.navigate(Routes.LEARN) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }

                composable(Routes.LEARN) {
                    LearnScreen(
                        viewModel = wordViewModel,
                        onOpenLibrary = {
                            navController.navigate(Routes.LIBRARY) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onGoHome = {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.DASHBOARD) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onGoPractice = {
                            navController.navigate(Routes.PRACTICE) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }

                composable(Routes.PRACTICE) {
                    PracticeScreen(
                        viewModel = practiceViewModel,
                        onOpenLibrary = {
                            navController.navigate(Routes.LIBRARY) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }

                composable(Routes.PROFILE) {
                    ProfileScreen(authViewModel, profileViewModel)
                }

                composable(Routes.ANALYTICS) {
                    AnalyticsScreen(uiState = analyticsUiState)
                }

                composable(Routes.CHECKPOINT) {
                    CheckpointScreen(
                        viewModel = checkpointViewModel,
                        autoStart = true,
                        onClose = { navController.popBackStack() },
                        onOpenLibrary = {
                            navController.popBackStack()
                            navController.navigate(Routes.LIBRARY) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
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
