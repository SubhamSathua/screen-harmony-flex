package com.prism.screenharmony.flex.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prism.screenharmony.flex.ui.viewmodels.PermissionState
import com.prism.screenharmony.flex.utils.PermissionHelper

@Composable
fun PermissionWarningBanner(
    permissionState: PermissionState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVisible = !permissionState.hasCrucialPermissions || !permissionState.isBatteryIgnored

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Background Permissions Needed",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Text(
                    text = "To block apps seamlessly while you use other apps, grant the following permissions:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                )

                // 1. Usage Access
                PermissionRow(
                    title = "1. Usage Access",
                    description = "Detects foreground open apps",
                    isGranted = permissionState.isUsageGranted,
                    onGrant = { PermissionHelper.openUsageAccessSettings(context) }
                )

                // 2. Display Over Other Apps (Overlay)
                PermissionRow(
                    title = "2. Display Over Other Apps",
                    description = "Shows lock wall over Chrome & apps",
                    isGranted = permissionState.isOverlayGranted,
                    onGrant = { PermissionHelper.openOverlaySettings(context) }
                )

                // 3. Battery Optimization
                PermissionRow(
                    title = "3. Unrestricted Battery",
                    description = "Keeps service alive in background",
                    isGranted = permissionState.isBatteryIgnored,
                    onGrant = { PermissionHelper.openBatteryOptimizationSettings(context) }
                )

                // 4. MIUI Background Pop-up (MIUI / HyperOS only)
                if (permissionState.isMiuiDevice) {
                    PermissionRow(
                        title = "4. MIUI Pop-up window permission",
                        description = "Enable 'Display pop-up windows while running in the background'",
                        isGranted = permissionState.isMiuiPopupGranted,
                        onGrant = { PermissionHelper.openMiuiOtherPermissions(context) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f)
            )
        }
        if (isGranted) {
            Text(
                text = "✓ Active",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF1B873F),
                fontWeight = FontWeight.Bold
            )
        } else {
            Button(
                onClick = onGrant,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
