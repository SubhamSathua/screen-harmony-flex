package com.prism.screenharmony.flex

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.prism.screenharmony.flex.data.BlockRepository
import com.prism.screenharmony.flex.data.BlockRule
import com.prism.screenharmony.flex.service.AppBlockerService
import com.prism.screenharmony.flex.ui.screens.AppListScreen
import com.prism.screenharmony.flex.ui.screens.BlocksPage
import com.prism.screenharmony.flex.ui.screens.CreateBlockPage
import com.prism.screenharmony.flex.ui.theme.*
import com.prism.screenharmony.flex.utils.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
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
                ScreenHarmonyFlexApp()
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    BLOCK("Block", Icons.Rounded.Block),
    PARENTAL("Parental", Icons.Rounded.FamilyRestroom),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

enum class ScreenState {
    MAIN_TABS,
    CREATE_OR_EDIT_BLOCK,
    SELECT_APPS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHarmonyFlexApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.BLOCK) }
    var currentScreenState by remember { mutableStateOf(ScreenState.MAIN_TABS) }
    var editingRule by remember { mutableStateOf(BlockRule()) }
    var isAppListGridView by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Live permission tracking (asynchronously queried on Dispatchers.IO)
    var isUsageGranted by remember { mutableStateOf(true) }
    var isOverlayGranted by remember { mutableStateOf(true) }
    var isBatteryIgnored by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch(Dispatchers.IO) {
                    val usage = PermissionHelper.isUsageAccessGranted(context)
                    val overlay = PermissionHelper.isOverlayGranted(context)
                    val battery = PermissionHelper.isBatteryOptimizationIgnored(context)
                    withContext(Dispatchers.Main) {
                        isUsageGranted = usage
                        isOverlayGranted = overlay
                        isBatteryIgnored = battery
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val hasCrucialPermissions = isUsageGranted && isOverlayGranted

    val navContainerColor = MaterialTheme.colorScheme.surfaceContainer
    val navContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val navSelectedIndicator = MaterialTheme.colorScheme.primaryContainer
    val navSelectedIcon = MaterialTheme.colorScheme.onPrimaryContainer
    val navSelectedText = MaterialTheme.colorScheme.primary

    val navSuiteColors = NavigationSuiteDefaults.colors(
        navigationBarContainerColor = navContainerColor,
        navigationBarContentColor = navContentColor,
        navigationRailContainerColor = navContainerColor,
        navigationRailContentColor = navContentColor,
        navigationDrawerContainerColor = navContainerColor,
        navigationDrawerContentColor = navContentColor
    )

    val itemColors: NavigationSuiteItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = navSelectedIcon,
            selectedTextColor = navSelectedText,
            indicatorColor = navSelectedIndicator,
            unselectedIconColor = navContentColor,
            unselectedTextColor = navContentColor
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = navSelectedIcon,
            selectedTextColor = navSelectedText,
            indicatorColor = navSelectedIndicator,
            unselectedIconColor = navContentColor,
            unselectedTextColor = navContentColor
        ),
        navigationDrawerItemColors = NavigationDrawerItemDefaults.colors(
            selectedIconColor = navSelectedIcon,
            selectedTextColor = navSelectedText,
            unselectedIconColor = navContentColor,
            unselectedTextColor = navContentColor
        )
    )

    when (currentScreenState) {
        ScreenState.SELECT_APPS -> {
            AppListScreen(
                selectedApps = editingRule.selectedApps,
                isGridView = isAppListGridView,
                onViewToggle = { isAppListGridView = it },
                onDone = { updatedApps ->
                    editingRule = editingRule.copy(selectedApps = updatedApps)
                    currentScreenState = ScreenState.CREATE_OR_EDIT_BLOCK
                },
                onBack = {
                    currentScreenState = ScreenState.CREATE_OR_EDIT_BLOCK
                }
            )
        }
        ScreenState.CREATE_OR_EDIT_BLOCK -> {
            CreateBlockPage(
                rule = editingRule,
                onRuleChanged = { editingRule = it },
                onSelectApps = { currentScreenState = ScreenState.SELECT_APPS },
                onSave = {
                    val finalRule = if (editingRule.name.isBlank()) editingRule.copy(name = "App Block") else editingRule
                    BlockRepository.addOrUpdateRule(finalRule)
                    currentScreenState = ScreenState.MAIN_TABS
                },
                onBack = { currentScreenState = ScreenState.MAIN_TABS }
            )
        }
        ScreenState.MAIN_TABS -> {
            NavigationSuiteScaffold(
                navigationSuiteColors = navSuiteColors,
                navigationSuiteItems = {
                    AppDestinations.entries.forEach { destination ->
                        item(
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    fontWeight = if (currentDestination == destination) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = destination == currentDestination,
                            onClick = { currentDestination = destination },
                            colors = itemColors
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
                            val rules by BlockRepository.rules.collectAsState()
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
                                                    fontFamily = PlayfairFontFamily,
                                                    style = MaterialTheme.typography.headlineSmall,
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
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    )
                                },
                                floatingActionButton = {
                                    ExtendedFloatingActionButton(
                                        onClick = {
                                            editingRule = BlockRule()
                                            currentScreenState = ScreenState.CREATE_OR_EDIT_BLOCK
                                        },
                                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                        text = { Text("Create a Block", fontWeight = FontWeight.Bold) },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            ) { innerPadding ->
                                Column(
                                    modifier = Modifier
                                        .padding(innerPadding)
                                        .fillMaxSize()
                                ) {
                                    // Background Permissions Setup Card (Visible until Crucial permissions are granted)
                                    if (!hasCrucialPermissions || !isBatteryIgnored) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Security,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = "Background Permissions Needed",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Text(
                                                    text = "To block apps seamlessly while you use other apps, grant the following permissions:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )

                                                // 1. Usage Access
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("1. Usage Access", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                                        Text("Detects open apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    if (isUsageGranted) {
                                                        Text("✓ Granted", style = MaterialTheme.typography.labelSmall, color = Color(0xFF34A853), fontWeight = FontWeight.Bold)
                                                    } else {
                                                        Button(
                                                            onClick = { PermissionHelper.openUsageAccessSettings(context) },
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                        ) {
                                                            Text("Grant", fontSize = 11.sp)
                                                        }
                                                    }
                                                }

                                                // 2. Display Over Other Apps (Overlay)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("2. Display Over Other Apps", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                                        Text("Shows lock wall over Chrome & apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    if (isOverlayGranted) {
                                                        Text("✓ Granted", style = MaterialTheme.typography.labelSmall, color = Color(0xFF34A853), fontWeight = FontWeight.Bold)
                                                    } else {
                                                        Button(
                                                            onClick = { PermissionHelper.openOverlaySettings(context) },
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                        ) {
                                                            Text("Grant", fontSize = 11.sp)
                                                        }
                                                    }
                                                }

                                                // 3. Battery Optimization
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("3. Unrestricted Battery", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                                        Text("Keeps service alive in background", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    if (isBatteryIgnored) {
                                                        Text("✓ Granted", style = MaterialTheme.typography.labelSmall, color = Color(0xFF34A853), fontWeight = FontWeight.Bold)
                                                    } else {
                                                        Button(
                                                            onClick = { PermissionHelper.openBatteryOptimizationSettings(context) },
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                        ) {
                                                            Text("Grant", fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        BlocksPage(
                                            rules = rules,
                                            onToggleRule = { rule, isEnabled -> BlockRepository.toggleRule(rule.id, isEnabled) },
                                            onEditRule = { rule ->
                                                editingRule = rule
                                                currentScreenState = ScreenState.CREATE_OR_EDIT_BLOCK
                                            },
                                            onDeleteRule = { rule -> BlockRepository.deleteRule(rule.id) },
                                            onPauseRule = { rule ->
                                                if (rule.isPaused()) {
                                                    BlockRepository.unpauseRule(rule.id)
                                                } else {
                                                    BlockRepository.pauseRule(rule.id, 5)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        AppDestinations.PARENTAL -> ParentalTabScreen()
                        AppDestinations.SETTINGS -> SettingsTabScreen()
                    }
                }
            }
        }
    }
}

// ==========================================
// PARENTAL TAB SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalTabScreen() {
    val familyCode by remember { mutableStateOf("SH-7842") }
    val isConnected by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Text(
                        text = "Parental Control",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Family Pairing Code",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = familyCode,
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter this code on Kid's Phone for 100% Free Remote Sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Text(
                text = "Connected Devices",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(10.dp).size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kid's Device (Pixel 7)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (isConnected) Color(0xFF34A853) else Color.Gray,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isConnected) "Active & Synced" else "Offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.Sync, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// ==========================================
// SETTINGS TAB SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTabScreen() {
    val themeState = LocalThemeState.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isColorPaletteExpanded by remember { mutableStateOf(false) }
    var isUsageGranted by remember { mutableStateOf(true) }
    var isOverlayGranted by remember { mutableStateOf(true) }
    var isBatteryIgnored by remember { mutableStateOf(true) }
    var isAccessibilityGranted by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch(Dispatchers.IO) {
                    val usage = PermissionHelper.isUsageAccessGranted(context)
                    val overlay = PermissionHelper.isOverlayGranted(context)
                    val battery = PermissionHelper.isBatteryOptimizationIgnored(context)
                    val accessibility = PermissionHelper.isAccessibilityGranted(context)
                    withContext(Dispatchers.Main) {
                        isUsageGranted = usage
                        isOverlayGranted = overlay
                        isBatteryIgnored = battery
                        isAccessibilityGranted = accessibility
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Text(
                        text = "Settings",
                        fontFamily = PlayfairFontFamily,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionHeader(title = "Appearance") }

            item {
                GroupedContainer {
                    GroupedItemRow(
                        icon = Icons.Rounded.DarkMode,
                        title = "Theme Mode",
                        subtitle = "Select system, light, or dark visual style"
                    ) {
                        SingleChoiceSegmentedRow(
                            selected = themeState.themeMode,
                            onSelect = { themeState.themeMode = it }
                        )
                    }

                    ItemDivider()

                    GroupedItemRow(
                        icon = Icons.Rounded.Contrast,
                        title = "AMOLED Black",
                        subtitle = "Pure pitch black background for OLED screens"
                    ) {
                        Switch(
                            checked = themeState.isAmoled,
                            onCheckedChange = { themeState.isAmoled = it }
                        )
                    }

                    ItemDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isColorPaletteExpanded = !isColorPaletteExpanded }
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = themeState.palette.primaryColor,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(7.dp).size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Color Palette", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(themeState.palette.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                imageVector = if (isColorPaletteExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = isColorPaletteExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppColorPalette.entries.forEach { palette ->
                                    val isSelected = themeState.palette == palette
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.fillMaxWidth().clickable { themeState.palette = palette }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = palette.primaryColor,
                                                modifier = Modifier.size(24.dp).border(if (isSelected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            ) {}
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = palette.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = "Permissions & Background Enforcement") }

            item {
                GroupedContainer {
                    // 1. Usage Access
                    GroupedItemRow(
                        icon = Icons.Rounded.QueryStats,
                        title = "Usage Access (Apps)",
                        subtitle = if (isUsageGranted) "Active • Detects foreground apps" else "Required • Tap to grant permission"
                    ) {
                        if (isUsageGranted) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        } else {
                            Button(
                                onClick = { PermissionHelper.openUsageAccessSettings(context) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    ItemDivider()

                    // 2. Display Over Other Apps (Overlay)
                    GroupedItemRow(
                        icon = Icons.Rounded.Layers,
                        title = "Display Over Other Apps",
                        subtitle = if (isOverlayGranted) "Active • Pops up lock screen over apps" else "Crucial • Allows lock screen to open in background"
                    ) {
                        if (isOverlayGranted) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        } else {
                            Button(
                                onClick = { PermissionHelper.openOverlaySettings(context) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    ItemDivider()

                    // 3. Battery Optimization
                    GroupedItemRow(
                        icon = Icons.Rounded.BatteryChargingFull,
                        title = "Unrestricted Battery",
                        subtitle = if (isBatteryIgnored) "Active • Never killed by OS" else "Recommended • Keeps background service alive"
                    ) {
                        if (isBatteryIgnored) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { PermissionHelper.openBatteryOptimizationSettings(context) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        }
                    }

                    ItemDivider()

                    // 4. Accessibility
                    GroupedItemRow(
                        icon = Icons.Rounded.Language,
                        title = "Accessibility (Websites)",
                        subtitle = if (isAccessibilityGranted) "Active • Inspecting browser URLs" else "Optional • Only needed for websites"
                    ) {
                        if (isAccessibilityGranted) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { PermissionHelper.openAccessibilitySettings(context) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Enable", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = "About & Sync") }

            item {
                GroupedContainer {
                    GroupedItemRow(
                        icon = Icons.Rounded.CloudQueue,
                        title = "Sync Protocol",
                        subtitle = "Firebase Spark (100% Free Cloud Tier)"
                    ) {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.padding(4.dp)) {
                            Text("FREE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    ItemDivider()

                    val versionName = remember {
                        try {
                            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            pInfo.versionName ?: "0.1.0"
                        } catch (e: Exception) {
                            "0.1.0"
                        }
                    }

                    GroupedItemRow(
                        icon = Icons.Rounded.Info,
                        title = "App Version",
                        subtitle = "ScreenHarmony Flex v$versionName"
                    ) {
                        Text(
                            text = "v$versionName",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
fun GroupedContainer(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun GroupedItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp).size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        trailingContent()
    }
}

@Composable
fun ItemDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun SingleChoiceSegmentedRow(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            AppThemeMode.entries.forEach { mode ->
                val isSelected = selected == mode
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable { onSelect(mode) }
                ) {
                    Text(
                        text = when (mode) {
                            AppThemeMode.SYSTEM -> "Auto"
                            AppThemeMode.LIGHT -> "Light"
                            AppThemeMode.DARK -> "Dark"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    val themeState = remember { ThemeState() }
    ScreenHarmonyFlexTheme(themeState = themeState) {
        ScreenHarmonyFlexApp()
    }
}