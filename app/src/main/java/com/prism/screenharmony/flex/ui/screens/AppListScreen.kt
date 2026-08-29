package com.prism.screenharmony.flex.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prism.screenharmony.flex.ui.components.AppIcon
import com.prism.screenharmony.flex.ui.components.ExitConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val name: String,
    val packageName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    selectedApps: Set<String>,
    onDone: (Set<String>) -> Unit,
    onBack: () -> Unit,
    isGridView: Boolean = false,
    onViewToggle: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val pm = remember { context.packageManager }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showPackageNames by remember { mutableStateOf(false) }

    var tempSelectedApps by remember { mutableStateOf(selectedApps) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    val hasChanges = tempSelectedApps != selectedApps

    fun handleBack() {
        if (hasChanges) {
            showExitConfirmation = true
        } else {
            onBack()
        }
    }

    BackHandler {
        handleBack()
    }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedInfos = pm.queryIntentActivities(mainIntent, 0)
            resolvedInfos.asSequence()
                .map { resolveInfo ->
                    AppInfo(
                        name = resolveInfo.loadLabel(pm).toString(),
                        packageName = resolveInfo.activityInfo.packageName
                    )
                }
                .filter { it.packageName != context.packageName }
                .distinctBy { it.packageName }
                .sortedBy { it.name.lowercase() }
                .toList()
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Select Apps (${tempSelectedApps.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1f,
                label = "DoneBtnScale"
            )
            ExtendedFloatingActionButton(
                onClick = { onDone(tempSelectedApps) },
                modifier = Modifier
                    .padding(16.dp)
                    .scale(scale),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                interactionSource = interactionSource,
                icon = { Icon(Icons.Rounded.Done, contentDescription = null) },
                text = { Text("DONE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                // Grid/List toggle + Pkg switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = !isGridView,
                            onClick = { onViewToggle(false) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "List View", modifier = Modifier.size(18.dp))
                        }
                        SegmentedButton(
                            selected = isGridView,
                            onClick = { onViewToggle(true) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Icon(Icons.Rounded.GridView, contentDescription = "Grid View", modifier = Modifier.size(18.dp))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pkg", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = showPackageNames,
                            onCheckedChange = { showPackageNames = it },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }

                // Category Presets Filter Chips
                val categories = remember {
                    listOf("All", "Social", "Media", "Games", "Shopping", "Browser")
                }
                var selectedCategory by remember { mutableStateOf("All") }

                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val filteredApps = apps.filter { app ->
                        val matchesSearch = itMatchesSearch(app, searchQuery)
                        val matchesCategory = itMatchesCategory(app, selectedCategory)
                        matchesSearch && matchesCategory
                    }

                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(100.dp),
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 88.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                AppGridItem(
                                    app = app,
                                    isSelected = tempSelectedApps.contains(app.packageName),
                                    showPackageName = showPackageNames,
                                    pm = pm,
                                    onSelected = {
                                        tempSelectedApps = if (tempSelectedApps.contains(app.packageName)) {
                                            tempSelectedApps - app.packageName
                                        } else {
                                            tempSelectedApps + app.packageName
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                AppListItem(
                                    app = app,
                                    isSelected = tempSelectedApps.contains(app.packageName),
                                    showPackageName = showPackageNames,
                                    pm = pm,
                                    onSelected = {
                                        tempSelectedApps = if (tempSelectedApps.contains(app.packageName)) {
                                            tempSelectedApps - app.packageName
                                        } else {
                                            tempSelectedApps + app.packageName
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExitConfirmation) {
        ExitConfirmationDialog(
            onSave = {
                showExitConfirmation = false
                onDone(tempSelectedApps)
            },
            onDiscard = {
                showExitConfirmation = false
                onBack()
            },
            onDismiss = { showExitConfirmation = false }
        )
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    isSelected: Boolean,
    showPackageName: Boolean,
    pm: PackageManager,
    onSelected: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onSelected() },
        headlineContent = { Text(app.name, fontWeight = FontWeight.SemiBold) },
        supportingContent = if (showPackageName) {
            { Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        leadingContent = {
            AppIcon(packageName = app.packageName, pm = pm, size = 44)
        },
        trailingContent = {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    )
}

@Composable
fun AppGridItem(
    app: AppInfo,
    isSelected: Boolean,
    showPackageName: Boolean,
    pm: PackageManager,
    onSelected: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelected() }
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else Color.Transparent
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.TopEnd) {
                AppIcon(packageName = app.packageName, pm = pm, size = 52)
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(18.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.name,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showPackageName) {
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun itMatchesSearch(app: AppInfo, query: String): Boolean {
    if (query.isBlank()) return true
    return app.name.contains(query, ignoreCase = true) ||
            app.packageName.contains(query, ignoreCase = true)
}

private fun itMatchesCategory(app: AppInfo, category: String): Boolean {
    val pkg = app.packageName.lowercase()
    val name = app.name.lowercase()
    return when (category) {
        "All" -> true
        "Social" -> {
            pkg.contains("instagram") || pkg.contains("tiktok") || pkg.contains("facebook") ||
            pkg.contains("katana") || pkg.contains("twitter") || pkg.contains("snapchat") ||
            pkg.contains("reddit") || pkg.contains("telegram") || pkg.contains("whatsapp") ||
            pkg.contains("pinterest") || pkg.contains("threads") || name.contains("social")
        }
        "Media" -> {
            pkg.contains("youtube") || pkg.contains("netflix") || pkg.contains("spotify") ||
            pkg.contains("primevideo") || pkg.contains("twitch") || pkg.contains("disney") ||
            pkg.contains("hulu") || pkg.contains("hotstar") || name.contains("video") || name.contains("music")
        }
        "Games" -> {
            pkg.contains("game") || pkg.contains("roblox") || pkg.contains("supercell") ||
            pkg.contains("pubg") || pkg.contains("candycrush") || pkg.contains("mojang") ||
            pkg.contains("ea.gp") || pkg.contains("epicgames") || pkg.contains("riotgames")
        }
        "Shopping" -> {
            pkg.contains("amazon") || pkg.contains("ebay") || pkg.contains("flipkart") ||
            pkg.contains("shein") || pkg.contains("temu") || pkg.contains("aliexpress") ||
            pkg.contains("walmart") || pkg.contains("target") || name.contains("shop")
        }
        "Browser" -> {
            pkg.contains("chrome") || pkg.contains("firefox") || pkg.contains("opera") ||
            pkg.contains("browser") || pkg.contains("brave") || pkg.contains("edge") ||
            pkg.contains("sbrowser")
        }
        else -> true
    }
}
