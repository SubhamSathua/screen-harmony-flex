package com.prism.screenharmony.flex.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val fullPolicyText = """
ScreenHarmony Flex - Privacy Policy
Last Updated: September 2026
Lead Developer: Subham Kumar Sathua
License: Free and Open Source Software (FOSS) / GNU General Public License v3.0

1. Android Permissions & Local Processing:
- Usage Access (PACKAGE_USAGE_STATS): Queried strictly locally to detect foreground applications. App histories are never transmitted to external servers or ad networks.
- Display Over Other Apps (SYSTEM_ALERT_WINDOW): Used exclusively to display the lock screen over restricted applications.
- Accessibility Service: Inspects browser address bar locally in memory to enforce website block rules. Browsing history is NEVER logged, stored, or transmitted.
- Exact Alarms & Watchdog (SCHEDULE_EXACT_ALARM): Schedules precise start and end times for scheduled blocks and wakes the background service if killed.
- Notifications (POST_NOTIFICATIONS): Keeps the persistent foreground service alive.

2. Family Sync & Cloud Protocol:
- Firebase Spark Free Cloud Infrastructure: Synchronization only occurs when you explicitly create or join a family group using a 6-digit PIN.
- Anonymized Identifiers: Device pairing uses random IDs. We do not collect names, emails, phone numbers, IMEI, MAC address, or GPS location.
- Synced Data: Only parent-defined rules (package names, time slots) and daily screen time totals are synced between paired devices.

3. Local Storage & Data Deletion:
- On-Device Storage: Rules and settings are stored locally in SQLite database and EncryptedSharedPreferences.
- Complete Data Purging: Unlinking a family group or uninstalling the app permanently purges all local and cloud sync documents.

4. Open Source Verification & License:
- ScreenHarmony Flex is 100% Free and Open Source Software (FOSS) licensed under GNU General Public License v3.0.
- Public Repository: https://github.com/SubhamSathua/screen-harmony-flex
    """.trimIndent()

    fun copyPolicy() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("ScreenHarmony Privacy Policy", fullPolicyText))
        Toast.makeText(context, "Privacy Policy copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = ::copyPolicy) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Policy")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                }
                            }
                            Column {
                                Text("ScreenHarmony Flex Privacy Policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Last Updated: September 2026 • Selectable & Copyable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ScreenHarmony Flex is built with a zero-compromise privacy-first approach. We believe your digital habits and device telemetry belong solely to you. We do not collect, sell, or share personal information with third parties or advertisers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Section 1: Permissions
                PolicySection(
                    icon = Icons.Rounded.Key,
                    title = "1. Android Permissions & Local Processing",
                    items = listOf(
                        PolicyItem(
                            title = "Usage Access (PACKAGE_USAGE_STATS)",
                            description = "Queried locally to detect which application is active in the foreground. Your app history and usage logs are never uploaded to our servers or sent to any advertising network."
                        ),
                        PolicyItem(
                            title = "Display Over Other Apps (SYSTEM_ALERT_WINDOW)",
                            description = "Used exclusively to draw the lock wall over restricted applications when an active block rule is triggered."
                        ),
                        PolicyItem(
                            title = "Accessibility Service",
                            description = "Inspects the address bar of supported web browsers purely to enforce website block rules. Analyzed in-memory; web browsing histories and search queries are NEVER logged, stored, or transmitted."
                        ),
                        PolicyItem(
                            title = "Exact Alarms & Watchdog (SCHEDULE_EXACT_ALARM)",
                            description = "Schedules precise start and end times for scheduled blocks and wakes up the background service if terminated by aggressive OEM battery optimizers."
                        ),
                        PolicyItem(
                            title = "Notifications (POST_NOTIFICATIONS)",
                            description = "Enables the persistent foreground notification required by Android to maintain continuous monitoring."
                        )
                    )
                )

                // Section 2: Family Sync & Cloud Communication
                PolicySection(
                    icon = Icons.Rounded.CloudSync,
                    title = "2. Family Sync & Cloud Protocol",
                    items = listOf(
                        PolicyItem(
                            title = "Firebase Cloud Infrastructure",
                            description = "Parent-Child synchronization operates over Google Firebase Firestore (Spark Free Tier). Synchronization only occurs when you explicitly create or join a family group using a 6-digit PIN."
                        ),
                        PolicyItem(
                            title = "Anonymized Identifiers",
                            description = "Device pairing uses pseudorandom device IDs. We do not collect or store your real name, email, phone number, IMEI, MAC address, or precise GPS location."
                        ),
                        PolicyItem(
                            title = "Synchronized Data",
                            description = "Only parent-defined block rules (app package names, schedule time slots) and high-level daily screen time totals are synced between paired devices for parental dashboard visualization."
                        )
                    )
                )

                // Section 3: Data Storage & Deletion
                PolicySection(
                    icon = Icons.Rounded.Storage,
                    title = "3. Local Storage & Data Deletion",
                    items = listOf(
                        PolicyItem(
                            title = "On-Device Storage",
                            description = "Your rules, lock settings, and quotes are stored locally in SQLite database and EncryptedSharedPreferences."
                        ),
                        PolicyItem(
                            title = "Complete Data Purging",
                            description = "Unlinking a family group from the Settings tab or uninstalling the app permanently purges all associated local data and removes cloud sync documents."
                        )
                    )
                )

                // Section 4: Security, Open Source & License
                PolicySection(
                    icon = Icons.Rounded.Shield,
                    title = "4. Open Source Verification & License Terms",
                    items = listOf(
                        PolicyItem(
                            title = "GNU GPL v3.0 / Free and Open Source Software (FOSS)",
                            description = "ScreenHarmony Flex is released under the GNU General Public License v3.0 (GPLv3). You are free to run, study, audit, modify, fork, and distribute this software in accordance with the copyleft open-source license."
                        ),
                        PolicyItem(
                            title = "Community Auditing",
                            description = "The complete source code is publicly accessible on GitHub (https://github.com/SubhamSathua/screen-harmony-flex) for complete transparency."
                        ),
                        PolicyItem(
                            title = "Lead Developer & Contact",
                            description = "Developed and maintained by Subham Kumar Sathua. For security disclosures, issues, or contributions, please visit the GitHub project repository."
                        )
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private data class PolicyItem(
    val title: String,
    val description: String
)

@Composable
private fun PolicySection(
    icon: ImageVector,
    title: String,
    items: List<PolicyItem>
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
