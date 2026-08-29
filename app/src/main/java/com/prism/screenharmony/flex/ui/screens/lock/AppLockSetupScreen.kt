package com.prism.screenharmony.flex.ui.screens.lock

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prism.screenharmony.flex.data.AppLockManager
import com.prism.screenharmony.flex.data.RecoveryConstants
import com.prism.screenharmony.flex.ui.components.CustomPinKeypad
import com.prism.screenharmony.flex.ui.components.PinDotsDisplay
import com.prism.screenharmony.flex.ui.components.SecureFlagEffect

enum class SetupStep {
    ENTER_PIN,
    RETYPE_PIN,
    RECOVERY_PAGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSetupScreen(
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    SecureFlagEffect()

    var currentStep by remember { mutableStateOf(SetupStep.ENTER_PIN) }
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Recovery config state
    var isSeedPhraseEnabled by remember { mutableStateOf(true) }
    var seedPhrase by remember { mutableStateOf(RecoveryConstants.generatePhrase(6)) }
    var isBiometricsRecoveryEnabled by remember { mutableStateOf(true) }
    var isSecurityQuestionEnabled by remember { mutableStateOf(false) }
    var selectedQuestion by remember { mutableStateOf(RecoveryConstants.SECURITY_QUESTIONS[0]) }
    var securityAnswer by remember { mutableStateOf("") }
    var hintText by remember { mutableStateOf("") }

    BackHandler {
        when (currentStep) {
            SetupStep.ENTER_PIN -> onCancel()
            SetupStep.RETYPE_PIN -> {
                confirmPin = ""
                isError = false
                errorMessage = null
                currentStep = SetupStep.ENTER_PIN
            }
            SetupStep.RECOVERY_PAGE -> {
                confirmPin = ""
                currentStep = SetupStep.RETYPE_PIN
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentStep) {
                            SetupStep.ENTER_PIN -> "Create PIN"
                            SetupStep.RETYPE_PIN -> "Confirm PIN"
                            SetupStep.RECOVERY_PAGE -> "Recovery Options"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (currentStep) {
                            SetupStep.ENTER_PIN -> onCancel()
                            SetupStep.RETYPE_PIN -> {
                                confirmPin = ""
                                isError = false
                                errorMessage = null
                                currentStep = SetupStep.ENTER_PIN
                            }
                            SetupStep.RECOVERY_PAGE -> {
                                confirmPin = ""
                                currentStep = SetupStep.RETYPE_PIN
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            when (currentStep) {
                SetupStep.ENTER_PIN -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Set a 4 to 12 digit PIN",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This PIN will be required to open ScreenHarmony and edit rules",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        PinDotsDisplay(
                            pinLength = firstPin.length,
                            isError = isError,
                            showCounter = true
                        )
                    }

                    CustomPinKeypad(
                        onDigitPress = { digit ->
                            if (firstPin.length < 12) {
                                isError = false
                                firstPin += digit
                            }
                        },
                        onBackspace = {
                            if (firstPin.isNotEmpty()) {
                                firstPin = firstPin.dropLast(1)
                                isError = false
                            }
                        },
                        onSubmit = {
                            if (firstPin.length >= 4) {
                                currentStep = SetupStep.RETYPE_PIN
                            }
                        },
                        isSubmitEnabled = firstPin.length >= 4,
                        submitIcon = Icons.AutoMirrored.Rounded.ArrowForward
                    )
                }

                SetupStep.RETYPE_PIN -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Re-enter your PIN",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please verify your PIN to continue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        PinDotsDisplay(
                            pinLength = confirmPin.length,
                            isError = isError,
                            showCounter = false
                        )

                        errorMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    CustomPinKeypad(
                        onDigitPress = { digit ->
                            if (confirmPin.length < 12) {
                                isError = false
                                errorMessage = null
                                confirmPin += digit
                            }
                        },
                        onBackspace = {
                            if (confirmPin.isNotEmpty()) {
                                confirmPin = confirmPin.dropLast(1)
                                isError = false
                                errorMessage = null
                            }
                        },
                        onSubmit = {
                            if (confirmPin == firstPin) {
                                currentStep = SetupStep.RECOVERY_PAGE
                            } else {
                                isError = true
                                errorMessage = "PINs do not match. Try again."
                                confirmPin = ""
                            }
                        },
                        isSubmitEnabled = confirmPin.length == firstPin.length,
                        submitIcon = Icons.AutoMirrored.Rounded.ArrowForward
                    )
                }

                SetupStep.RECOVERY_PAGE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Text(
                            text = "Account Recovery Methods",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Select recovery methods in case you forget your PIN. All credentials are encrypted on-device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        RecoveryMethodsCards(
                            isSeedPhraseEnabled = isSeedPhraseEnabled,
                            onSeedPhraseToggle = { isSeedPhraseEnabled = it },
                            seedPhrase = seedPhrase,
                            onRegeneratePhrase = { seedPhrase = RecoveryConstants.generatePhrase(6) },
                            isBiometricsRecoveryEnabled = isBiometricsRecoveryEnabled,
                            onBiometricsRecoveryToggle = { isBiometricsRecoveryEnabled = it },
                            isSecurityQuestionEnabled = isSecurityQuestionEnabled,
                            onSecurityQuestionToggle = { isSecurityQuestionEnabled = it },
                            selectedQuestion = selectedQuestion,
                            onQuestionSelect = { selectedQuestion = it },
                            securityAnswer = securityAnswer,
                            onAnswerChange = { securityAnswer = it }
                        )

                        // Optional PIN Hint
                        OutlinedTextField(
                            value = hintText,
                            onValueChange = { hintText = it },
                            label = { Text("Optional PIN Hint") },
                            placeholder = { Text("e.g. Favorite book year...") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                AppLockManager.savePin(firstPin, hintText.ifBlank { null })
                                AppLockManager.saveRecoveryConfig(
                                    isSeedEnabled = isSeedPhraseEnabled,
                                    seedPhrase = if (isSeedPhraseEnabled) seedPhrase else null,
                                    isBioEnabled = isBiometricsRecoveryEnabled,
                                    isQuestionEnabled = isSecurityQuestionEnabled,
                                    question = if (isSecurityQuestionEnabled) selectedQuestion else null,
                                    answer = if (isSecurityQuestionEnabled) securityAnswer else null
                                )
                                onComplete()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable App Lock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
