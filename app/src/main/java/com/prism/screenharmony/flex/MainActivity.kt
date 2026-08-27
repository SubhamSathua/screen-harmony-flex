package com.prism.screenharmony.flex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prism.screenharmony.flex.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

@Composable
fun ScreenHarmonyFlexApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.BLOCK) }

    // Fully custom navigation suite colors to eliminate default purple and use custom theme colors everywhere
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
                AppDestinations.BLOCK -> BlockTabScreen()
                AppDestinations.PARENTAL -> ParentalTabScreen()
                AppDestinations.SETTINGS -> SettingsTabScreen()
            }
        }
    }
}

// ==========================================
// 1. BLOCK TAB SCREEN (Clean App Blocker UI)
// ==========================================
data class DummyAppItem(
    val id: String,
    val name: String,
    val packageName: String,
    val iconVector: ImageVector,
    var isBlocked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockTabScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var apps by remember {
        mutableStateOf(
            listOf(
                DummyAppItem("1", "YouTube", "com.google.android.youtube", Icons.Rounded.PlayCircle, true),
                DummyAppItem("2", "Instagram", "com.instagram.android", Icons.Rounded.CameraAlt, true),
                DummyAppItem("3", "TikTok", "com.zhiliaoapp.musically", Icons.Rounded.MusicNote, true),
                DummyAppItem("4", "Roblox", "com.roblox.client", Icons.Rounded.SportsEsports, false),
                DummyAppItem("5", "Snapchat", "com.snapchat.android", Icons.Rounded.ChatBubble, false),
                DummyAppItem("6", "Chrome", "com.android.chrome", Icons.Rounded.Public, false),
                DummyAppItem("7", "Netflix", "com.netflix.mediaclient", Icons.Rounded.Movie, false),
                DummyAppItem("8", "Spotify", "com.spotify.music", Icons.Rounded.Headphones, false)
            )
        )
    }

    val filteredApps = apps.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.packageName.contains(searchQuery, ignoreCase = true)
    }

    val blockedCount = apps.count { it.isBlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Column {
                        Text(
                            text = "App Blocker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Usage Access active • Instant enforcement",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (blockedCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (blockedCount > 0) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (blockedCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$blockedCount blocked",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (blockedCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    singleLine = true
                )
            }

            // Quick Status Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simple 1-Tap Blocker",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Apps toggled ON will be blocked immediately on kid's phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Installed Apps (${filteredApps.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Grouped container for apps list
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        filteredApps.forEachIndexed { index, app ->
                            AppBlockRow(
                                app = app,
                                onToggle = { isChecked ->
                                    apps = apps.map {
                                        if (it.id == app.id) it.copy(isBlocked = isChecked) else it
                                    }
                                }
                            )
                            if (index < filteredApps.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AppBlockRow(
    app: DummyAppItem,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (app.isBlocked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = app.iconVector,
                contentDescription = app.name,
                tint = if (app.isBlocked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(10.dp)
                    .size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = app.isBlocked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onError,
                checkedTrackColor = MaterialTheme.colorScheme.error
            )
        )
    }
}

// ==========================================
// 2. PARENTAL TAB SCREEN (Kid / Cloud Sync UI)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalTabScreen() {
    var familyCode by remember { mutableStateOf("SH-7842") }
    var isConnected by remember { mutableStateOf(true) }

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
            // Pairing Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
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
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter this code on the Kid Phone to link instantly (100% Free Cloud Sync)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Connected Devices Grouped Container
            Text(
                text = "Connected Devices",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
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
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp)
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
                        IconButton(onClick = { /* Refresh */ }) {
                            Icon(Icons.Rounded.Sync, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Quick Actions Inside Grouped Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { /* Block All */ },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock All")
                        }
                        OutlinedButton(
                            onClick = { /* Unblock All */ },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unlock All")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. SETTINGS TAB SCREEN (Grouped Containers & Working Appearance)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTabScreen() {
    val themeState = LocalThemeState.current
    var isColorPaletteExpanded by remember { mutableStateOf(false) }

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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
            // -------------------------------------------------------------
            // SECTION 1: APPEARANCE (Theme Mode, Amoled, Expandable Colors)
            // -------------------------------------------------------------
            item {
                SectionHeader(title = "Appearance")
            }

            item {
                GroupedContainer {
                    // Card 1: Theme Mode (System / Light / Dark)
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

                    // Card 2: AMOLED Pure Black
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

                    // Card 3: Color Palette (Expandable with all M3 colors)
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
                                    modifier = Modifier
                                        .padding(7.dp)
                                        .size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Color Palette",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = themeState.palette.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = if (isColorPaletteExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Expandable Color Swatches Grid
                        AnimatedVisibility(
                            visible = isColorPaletteExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppColorPalette.entries.forEach { palette ->
                                    val isSelected = themeState.palette == palette
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { themeState.palette = palette }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = palette.primaryColor,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 0.dp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape
                                                    )
                                            ) {}
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = palette.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
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

            // -------------------------------------------------------------
            // SECTION 2: PERMISSIONS & SERVICE (Connected Container)
            // -------------------------------------------------------------
            item {
                SectionHeader(title = "Permissions & System")
            }

            item {
                GroupedContainer {
                    GroupedItemRow(
                        icon = Icons.Rounded.CheckCircle,
                        title = "Usage Access",
                        subtitle = "Required for 1-tap blocking without accessibility"
                    ) {
                        FilledTonalButton(
                            onClick = { /* Open Settings */ },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Open", fontSize = 12.sp)
                        }
                    }

                    ItemDivider()

                    GroupedItemRow(
                        icon = Icons.Rounded.BatteryChargingFull,
                        title = "Battery Optimization",
                        subtitle = "Keep service uninterrupted in the background"
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // SECTION 3: CLOUD & ABOUT (Connected Container)
            // -------------------------------------------------------------
            item {
                SectionHeader(title = "About & Sync")
            }

            item {
                GroupedContainer {
                    GroupedItemRow(
                        icon = Icons.Rounded.CloudQueue,
                        title = "Sync Protocol",
                        subtitle = "Firebase Spark (100% Free Cloud Tier)"
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = "FREE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    ItemDivider()

                    GroupedItemRow(
                        icon = Icons.Rounded.Info,
                        title = "App Version",
                        subtitle = "ScreenHarmony Flex v0.2.0"
                    ) {
                        Text(
                            text = "v0.2.0",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ==========================================
// REUSABLE GROUPED / CONNECTED CONTAINER UI
// ==========================================
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
fun GroupedContainer(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
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
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AppThemeMode.entries.forEach { mode ->
                val isSelected = selected == mode
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onSelect(mode) }
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