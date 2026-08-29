package com.prism.screenharmony.flex.ui.screens.lock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.prism.screenharmony.flex.data.AppLockManager
import com.prism.screenharmony.flex.ui.components.CustomPinKeypad
import com.prism.screenharmony.flex.ui.components.PinDotsDisplay
import com.prism.screenharmony.flex.ui.components.SecureFlagEffect
import com.prism.screenharmony.flex.utils.BiometricHelper

@Composable
fun AppLockGateScreen(
    onUnlocked: () -> Unit
) {
    SecureFlagEffect()

    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var inputPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHintDialog by remember { mutableStateOf(false) }

    fun triggerBiometrics() {
        if (activity != null && AppLockManager.isBiometricsEnabled && BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = "Unlock ScreenHarmony",
                subtitle = "Touch the fingerprint sensor to continue",
                onSuccess = {
                    AppLockManager.unlockSession()
                    onUnlocked()
                },
                onError = { err ->
                    // user can fallback to PIN
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        triggerBiometrics()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter PIN to Unlock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                PinDotsDisplay(
                    pinLength = inputPin.length,
                    isError = isError
                )

                errorMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Biometrics & Hint Actions Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    if (AppLockManager.isBiometricsEnabled && BiometricHelper.isBiometricAvailable(context)) {
                        FilledTonalIconButton(
                            onClick = { triggerBiometrics() },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = "Biometric Unlock", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (AppLockManager.hasHint) {
                        TextButton(onClick = { showHintDialog = true }) {
                            Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Hint", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            CustomPinKeypad(
                onDigitPress = { digit ->
                    if (inputPin.length < 12) {
                        isError = false
                        errorMessage = null
                        inputPin += digit
                    }
                },
                onBackspace = {
                    if (inputPin.isNotEmpty()) {
                        inputPin = inputPin.dropLast(1)
                        isError = false
                        errorMessage = null
                    }
                },
                onSubmit = {
                    if (AppLockManager.verifyPin(inputPin)) {
                        onUnlocked()
                    } else {
                        isError = true
                        errorMessage = "Incorrect PIN"
                        inputPin = ""
                    }
                },
                isSubmitEnabled = inputPin.length >= 4
            )
        }
    }

    if (showHintDialog) {
        AlertDialog(
            onDismissRequest = { showHintDialog = false },
            icon = { Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("PIN Hint") },
            text = {
                Text(
                    text = AppLockManager.pinHint ?: "No hint was configured.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            },
            confirmButton = {
                TextButton(onClick = { showHintDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}
