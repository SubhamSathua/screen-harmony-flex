package com.prism.screenharmony.flex.ui.screens.lock

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prism.screenharmony.flex.data.AppLockManager
import com.prism.screenharmony.flex.data.RecoveryConstants
import com.prism.screenharmony.flex.ui.components.SecureFlagEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverySettingsScreen(
    onBack: () -> Unit
) {
    SecureFlagEffect()
    val context = LocalContext.current
    val initialConfig = remember { AppLockManager.getRecoveryConfig() }

    var isSeedPhraseEnabled by remember { mutableStateOf(initialConfig.isSeedPhraseEnabled) }
    var seedPhrase by remember { mutableStateOf(initialConfig.seedPhrase ?: RecoveryConstants.generatePhrase(6)) }
    var isBiometricsRecoveryEnabled by remember { mutableStateOf(initialConfig.isBiometricsRecoveryEnabled) }
    var isSecurityQuestionEnabled by remember { mutableStateOf(initialConfig.isSecurityQuestionEnabled) }
    var selectedQuestion by remember { mutableStateOf(initialConfig.securityQuestion ?: RecoveryConstants.SECURITY_QUESTIONS[0]) }
    var securityAnswer by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password Methods", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Manage the methods you can use to reset your PIN if you forget it. All recovery credentials are securely encrypted on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    AppLockManager.saveRecoveryConfig(
                        isSeedEnabled = isSeedPhraseEnabled,
                        seedPhrase = if (isSeedPhraseEnabled) seedPhrase else null,
                        isBioEnabled = isBiometricsRecoveryEnabled,
                        isQuestionEnabled = isSecurityQuestionEnabled,
                        question = if (isSecurityQuestionEnabled) selectedQuestion else null,
                        answer = if (isSecurityQuestionEnabled && securityAnswer.isNotBlank()) securityAnswer else null
                    )
                    Toast.makeText(context, "Recovery methods updated", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
