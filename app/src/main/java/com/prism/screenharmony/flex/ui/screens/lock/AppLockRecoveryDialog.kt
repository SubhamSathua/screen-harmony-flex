package com.prism.screenharmony.flex.ui.screens.lock

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.prism.screenharmony.flex.data.AppLockManager
import com.prism.screenharmony.flex.ui.components.CustomPinKeypad
import com.prism.screenharmony.flex.ui.components.PinDotsDisplay
import com.prism.screenharmony.flex.ui.components.SecureFlagEffect
import com.prism.screenharmony.flex.utils.BiometricHelper

enum class RecoveryFlowStep {
    SELECT_OR_INPUT_METHOD,
    ENTER_NEW_PIN,
    CONFIRM_NEW_PIN
}

@Composable
fun AppLockRecoveryDialog(
    onRecoverySuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    SecureFlagEffect()

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val recoveryConfig = remember { AppLockManager.getRecoveryConfig() }

    var currentStep by remember { mutableStateOf(RecoveryFlowStep.SELECT_OR_INPUT_METHOD) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Seed phrase input state
    var inputSeedPhrase by remember { mutableStateOf("") }
    var seedError by remember { mutableStateOf<String?>(null) }

    // Security question state
    var inputAnswer by remember { mutableStateOf("") }
    var questionError by remember { mutableStateOf<String?>(null) }

    // Reset PIN state
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentStep) {
                RecoveryFlowStep.SELECT_OR_INPUT_METHOD -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recover App Access",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close")
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Text(
                            text = "Choose a recovery method to verify your identity and set a new PIN.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        // 1. Device Credential Method (if enabled)
                        if (recoveryConfig.isBiometricsRecoveryEnabled) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Device Passcode / Biometrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "Verify using your phone's screen lock (PIN, pattern, or fingerprint).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = {
                                            if (activity != null) {
                                                BiometricHelper.showDeviceCredentialPrompt(
                                                    activity = activity,
                                                    onSuccess = {
                                                        currentStep = RecoveryFlowStep.ENTER_NEW_PIN
                                                    },
                                                    onError = { /* stay on screen */ }
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Verify Device Passcode")
                                    }
                                }
                            }
                        }

                        // 2. BIP-39 Seed Phrase Method (if enabled)
                        if (recoveryConfig.isSeedPhraseEnabled) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("BIP-39 Seed Phrase", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "Enter your 6-word backup phrase separated by spaces.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = inputSeedPhrase,
                                        onValueChange = {
                                            inputSeedPhrase = it
                                            seedError = null
                                        },
                                        placeholder = { Text("e.g. apple orbit quantum...") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    seedError?.let {
                                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            if (AppLockManager.verifySeedPhrase(inputSeedPhrase)) {
                                                currentStep = RecoveryFlowStep.ENTER_NEW_PIN
                                            } else {
                                                seedError = "Incorrect seed phrase. Please check and try again."
                                            }
                                        },
                                        enabled = inputSeedPhrase.isNotBlank(),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Verify Seed Phrase")
                                    }
                                }
                            }
                        }

                        // 3. Security Question Method (if enabled)
                        if (recoveryConfig.isSecurityQuestionEnabled && recoveryConfig.securityQuestion != null) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Security Question", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = recoveryConfig.securityQuestion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    OutlinedTextField(
                                        value = inputAnswer,
                                        onValueChange = {
                                            inputAnswer = it
                                            questionError = null
                                        },
                                        placeholder = { Text("Enter your answer...") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    questionError?.let {
                                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            if (AppLockManager.verifySecurityAnswer(inputAnswer)) {
                                                currentStep = RecoveryFlowStep.ENTER_NEW_PIN
                                            } else {
                                                questionError = "Incorrect answer. Please try again."
                                            }
                                        },
                                        enabled = inputAnswer.isNotBlank(),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Verify Answer")
                                    }
                                }
                            }
                        }

                        // Fallback Hint Card (if configured)
                        if (AppLockManager.hasHint) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("PIN Hint", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text(AppLockManager.pinHint ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                RecoveryFlowStep.ENTER_NEW_PIN -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Enter New PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Set a new 4 to 12 digit PIN", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))

                            PinDotsDisplay(
                                pinLength = newPin.length,
                                isError = pinError,
                                showCounter = true
                            )
                        }

                        CustomPinKeypad(
                            onDigitPress = { digit ->
                                if (newPin.length < 12) {
                                    pinError = false
                                    newPin += digit
                                }
                            },
                            onBackspace = {
                                if (newPin.isNotEmpty()) {
                                    newPin = newPin.dropLast(1)
                                    pinError = false
                                }
                            },
                            onSubmit = {
                                if (newPin.length >= 4) {
                                    currentStep = RecoveryFlowStep.CONFIRM_NEW_PIN
                                }
                            },
                            isSubmitEnabled = newPin.length >= 4,
                            submitIcon = Icons.AutoMirrored.Rounded.ArrowForward
                        )
                    }
                }

                RecoveryFlowStep.CONFIRM_NEW_PIN -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Confirm New PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Re-type your new PIN to confirm", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))

                            PinDotsDisplay(
                                pinLength = confirmNewPin.length,
                                isError = pinError,
                                showCounter = false
                            )

                            pinErrorMessage?.let {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }

                        CustomPinKeypad(
                            onDigitPress = { digit ->
                                if (confirmNewPin.length < 12) {
                                    pinError = false
                                    pinErrorMessage = null
                                    confirmNewPin += digit
                                }
                            },
                            onBackspace = {
                                if (confirmNewPin.isNotEmpty()) {
                                    confirmNewPin = confirmNewPin.dropLast(1)
                                    pinError = false
                                    pinErrorMessage = null
                                }
                            },
                            onSubmit = {
                                if (confirmNewPin == newPin) {
                                    AppLockManager.resetPin(newPin)
                                    onRecoverySuccess()
                                } else {
                                    pinError = true
                                    pinErrorMessage = "PINs do not match. Try again."
                                    confirmNewPin = ""
                                }
                            },
                            isSubmitEnabled = confirmNewPin.length == newPin.length,
                            submitIcon = Icons.Rounded.Check
                        )
                    }
                }
            }
        }
    }
}
