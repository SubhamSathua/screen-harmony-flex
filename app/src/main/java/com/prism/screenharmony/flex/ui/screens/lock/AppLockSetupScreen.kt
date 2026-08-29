package com.prism.screenharmony.flex.ui.screens.lock

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
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
import com.prism.screenharmony.flex.ui.components.CustomPinKeypad
import com.prism.screenharmony.flex.ui.components.PinDotsDisplay
import com.prism.screenharmony.flex.ui.components.SecureFlagEffect

enum class SetupStep {
    ENTER_PIN,
    RETYPE_PIN,
    HINT_PAGE
}

enum class HintOption {
    HINT,
    NONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSetupScreen(
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    // Screenshot & screen-recording protection
    SecureFlagEffect()

    var currentStep by remember { mutableStateOf(SetupStep.ENTER_PIN) }
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedHintOption by remember { mutableStateOf(HintOption.NONE) }
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
            SetupStep.HINT_PAGE -> {
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
                            SetupStep.HINT_PAGE -> "PIN Hint"
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
                            SetupStep.HINT_PAGE -> {
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
                .padding(innerPadding)
                .padding(bottom = 24.dp),
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
                        Spacer(modifier = Modifier.height(24.dp))

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
                        Spacer(modifier = Modifier.height(24.dp))

                        PinDotsDisplay(
                            pinLength = confirmPin.length,
                            isError = isError
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
                                currentStep = SetupStep.HINT_PAGE
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

                SetupStep.HINT_PAGE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .navigationBarsPadding()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = "Add a PIN Hint?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "A hint helps you remember your PIN if you forget it. Never write your actual PIN.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2 Radio Cards: Hint vs None
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Option 1: Hint
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedHintOption == HintOption.HINT) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (selectedHintOption == HintOption.HINT) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedHintOption = HintOption.HINT }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedHintOption == HintOption.HINT,
                                        onClick = { selectedHintOption = HintOption.HINT }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Hint", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Option 2: None
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedHintOption == HintOption.NONE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (selectedHintOption == HintOption.NONE) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedHintOption = HintOption.NONE }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedHintOption == HintOption.NONE,
                                        onClick = { selectedHintOption = HintOption.NONE }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("None", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = selectedHintOption == HintOption.HINT,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            OutlinedTextField(
                                value = hintText,
                                onValueChange = { hintText = it },
                                label = { Text("Hint (e.g. Grandma's birth year)") },
                                placeholder = { Text("Enter a clue...") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                val finalHint = if (selectedHintOption == HintOption.HINT) hintText else null
                                AppLockManager.savePin(firstPin, finalHint)
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
