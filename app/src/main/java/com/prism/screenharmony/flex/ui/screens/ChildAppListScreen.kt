package com.prism.screenharmony.flex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prism.screenharmony.flex.family.ChildAppInfo
import com.prism.screenharmony.flex.ui.components.ExitConfirmationDialog
import com.prism.screenharmony.flex.ui.components.RemoteAppIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildAppListScreen(
    childName: String,
    installedApps: List<ChildAppInfo>,
    selectedApps: Set<String>,
    onDone: (Set<String>) -> Unit,
    onBack: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    isGridView: Boolean = false,
    onViewToggle: (Boolean) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
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

    BackHandler { handleBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Select Apps (${tempSelectedApps.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Installed on $childName's Phone",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onRefresh != null) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Fetch latest apps")
                        }
                    }
                    TextButton(
                        onClick = {
                            tempSelectedApps = if (tempSelectedApps.size == installedApps.size) {
                                emptySet()
                            } else {
                                installedApps.map { it.packageName }.toSet()
                            }
                        }
                    ) {
                        Text(
                            if (tempSelectedApps.size == installedApps.size) "Deselect All" else "Select All",
                            fontWeight = FontWeight.Bold
                        )
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
                    placeholder = { Text("Search child apps") },
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
                val categories = remember(tempSelectedApps.size) {
                    listOf("All", "Selected (${tempSelectedApps.size})", "Social", "Media", "Games", "Shopping", "Browser")
                }
                var selectedCategory by remember { mutableStateOf("All") }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = if (cat.startsWith("Selected")) {
                            selectedCategory.startsWith("Selected")
                        } else {
                            selectedCategory == cat
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = if (cat.startsWith("Selected")) "Selected" else cat },
                            label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                if (installedApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Text("No Apps Fetched Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Ask $childName's phone to scan and send its installed app list.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (onRefresh != null) {
                                Button(
                                    onClick = onRefresh,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fetch Installed Apps")
                                }
                            }
                        }
                    }
                } else {
                    val filteredApps = installedApps.filter { app ->
                        val matchesSearch = itMatchesSearch(app, searchQuery)
                        val matchesCategory = itMatchesCategory(app, selectedCategory, tempSelectedApps)
                        matchesSearch && matchesCategory
                    }

                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(100.dp),
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 88.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                ChildAppGridItem(
                                    app = app,
                                    isSelected = tempSelectedApps.contains(app.packageName),
                                    showPackageName = showPackageNames,
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
                                ChildAppListItem(
                                    app = app,
                                    isSelected = tempSelectedApps.contains(app.packageName),
                                    showPackageName = showPackageNames,
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
fun ChildAppListItem(
    app: ChildAppInfo,
    isSelected: Boolean,
    showPackageName: Boolean,
    onSelected: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onSelected() },
        leadingContent = {
            RemoteAppIcon(
                packageName = app.packageName,
                appName = app.name,
                size = 40.dp
            )
        },
        headlineContent = {
            Text(
                app.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = if (showPackageName) {
            {
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        trailingContent = {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelected() }
            )
        }
    )
}

@Composable
fun ChildAppGridItem(
    app: ChildAppInfo,
    isSelected: Boolean,
    showPackageName: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelected() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                RemoteAppIcon(
                    packageName = app.packageName,
                    appName = app.name,
                    size = 48.dp
                )
                if (isSelected) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showPackageName) {
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun itMatchesSearch(app: ChildAppInfo, query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim().lowercase()
    return app.name.lowercase().contains(q) || app.packageName.lowercase().contains(q)
}

private fun itMatchesCategory(app: ChildAppInfo, category: String, selectedApps: Set<String>): Boolean {
    if (category == "All") return true
    if (category.startsWith("Selected")) return selectedApps.contains(app.packageName)
    val p = app.packageName.lowercase()
    val n = app.name.lowercase()
    return when (category) {
        "Social" -> p.contains("instagram") || p.contains("facebook") || p.contains("tiktok") || p.contains("twitter") || p.contains("x.android") || p.contains("snapchat") || p.contains("whatsapp") || p.contains("telegram") || p.contains("reddit") || n.contains("social")
        "Media" -> p.contains("youtube") || p.contains("netflix") || p.contains("spotify") || p.contains("primevideo") || p.contains("disney") || p.contains("music") || p.contains("video")
        "Games" -> p.contains("game") || p.contains("roblox") || p.contains("minecraft") || p.contains("pubg") || p.contains("supercell") || p.contains("candy")
        "Shopping" -> p.contains("amazon") || p.contains("ebay") || p.contains("flipkart") || p.contains("shop") || p.contains("aliexpress")
        "Browser" -> p.contains("chrome") || p.contains("browser") || p.contains("firefox") || p.contains("opera") || p.contains("brave")
        else -> true
    }
}
