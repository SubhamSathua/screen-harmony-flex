package com.prism.screenharmony.flex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.prism.screenharmony.flex.data.BlockRepository
import com.prism.screenharmony.flex.service.AppBlockerService
import com.prism.screenharmony.flex.ui.components.PermissionWarningBanner
import com.prism.screenharmony.flex.ui.screens.AppListScreen
import com.prism.screenharmony.flex.ui.screens.BlocksPage
import com.prism.screenharmony.flex.ui.screens.CreateBlockPage
import com.prism.screenharmony.flex.ui.screens.ParentalTabScreen
import com.prism.screenharmony.flex.ui.screens.SettingsTabScreen
import com.prism.screenharmony.flex.ui.theme.LocalThemeState
import com.prism.screenharmony.flex.ui.theme.ScreenHarmonyFlexTheme
import com.prism.screenharmony.flex.ui.theme.ThemeState
import com.prism.screenharmony.flex.ui.viewmodels.AppDestinations
import com.prism.screenharmony.flex.ui.viewmodels.MainViewModel
import com.prism.screenharmony.flex.ui.viewmodels.ScreenState

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize persistent disk repository
        BlockRepository.initialize(this)

        // Start background usage blocker engine
        AppBlockerService.start(this)

        setContent {
            val themeState = remember { ThemeState() }
            ScreenHarmonyFlexTheme(themeState = themeState) {
                ScreenHarmonyFlexApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHarmonyFlexApp(viewModel: MainViewModel) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val currentScreenState by viewModel.currentScreenState.collectAsState()
    val editingRule by viewModel.editingRule.collectAsState()
    val isAppListGridView by viewModel.isAppListGridView.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val rules by viewModel.rules.collectAsState()

    // Lifecycle observer to refresh permissions when user returns to app
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Unified Back Navigation
    BackHandler(enabled = true) {
        if (!viewModel.handleBack()) {
            (lifecycleOwner as? ComponentActivity)?.finish()
        }
    }

    when (currentScreenState) {
        ScreenState.SELECT_APPS -> {
            AppListScreen(
                selectedApps = editingRule.selectedApps,
                isGridView = isAppListGridView,
                onViewToggle = { viewModel.setAppListGridView(it) },
                onDone = { viewModel.onAppsSelected(it) },
                onBack = { viewModel.handleBack() }
            )
        }
        ScreenState.CREATE_OR_EDIT_BLOCK -> {
            CreateBlockPage(
                rule = editingRule,
                onRuleChanged = { viewModel.updateEditingRule(it) },
                onSelectApps = { viewModel.openSelectApps() },
                onSave = { viewModel.saveEditingRule() },
                onBack = { viewModel.handleBack() }
            )
        }
        ScreenState.MAIN_TABS -> {
            NavigationSuiteScaffold(
                navigationSuiteColors = NavigationSuiteDefaults.colors(
                    navigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationBarContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                navigationSuiteItems = {
                    AppDestinations.entries.forEach { destination ->
                        item(
                            icon = { Icon(imageVector = destination.icon, contentDescription = destination.label) },
                            label = {
                                Text(
                                    text = destination.label,
                                    fontWeight = if (currentDestination == destination) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = destination == currentDestination,
                            onClick = { viewModel.setDestination(destination) }
                        )
                    }
                }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentDestination) {
                        AppDestinations.BLOCK -> {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.background,
                                            titleContentColor = MaterialTheme.colorScheme.onBackground
                                        ),
                                        title = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "ScreenHarmony",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = "FLEX",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        },
                                        actions = {
                                            FilledTonalButton(
                                                onClick = { viewModel.openCreateRule() },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                modifier = Modifier.padding(end = 8.dp)
                                            ) {
                                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add Block", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    )
                                }
                            ) { innerPadding ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    PermissionWarningBanner(
                                        permissionState = permissionState,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )

                                    Box(modifier = Modifier.weight(1f)) {
                                        BlocksPage(
                                            rules = rules,
                                            onToggleRule = { rule, isEnabled -> viewModel.toggleRule(rule.id, isEnabled) },
                                            onEditRule = { rule -> viewModel.openEditRule(rule) },
                                            onDeleteRule = { rule -> viewModel.deleteRule(rule.id) },
                                            onPauseRule = { rule -> viewModel.togglePause(rule) }
                                        )
                                    }
                                }
                            }
                        }
                        AppDestinations.PARENTAL -> ParentalTabScreen()
                        AppDestinations.SETTINGS -> SettingsTabScreen(permissionState = permissionState)
                    }
                }
            }
        }
    }
}