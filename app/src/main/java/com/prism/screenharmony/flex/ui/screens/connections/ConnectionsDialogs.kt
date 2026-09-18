package com.prism.screenharmony.flex.ui.screens.connections

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prism.screenharmony.flex.family.ByobConfig
import com.prism.screenharmony.flex.family.ByobConfigManager
import com.prism.screenharmony.flex.family.ParentAccountState
import com.prism.screenharmony.flex.family.ParentCloudAuthManager
import com.prism.screenharmony.flex.utils.MnemonicHelper

// =============================================================================
// PARENT CLOUD AUTH DIALOG (LOGIN / REGISTER WITH 12-WORD PHRASE)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentCloudAuthDialog(
    onDismiss: () -> Unit,
    onAuthSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    var isRegisterMode by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Username Availability State
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameCheckStatus by remember { mutableStateOf<String?>(null) }
    var isUsernameAvailable by remember { mutableStateOf(false) }

    // 12-Word Recovery Phrase State
    val generatedPhrase = remember { MnemonicHelper.generate12WordPhrase() }
    var enableRecoveryPhrase by remember { mutableStateOf(true) }
    var showSkipWarningDialog by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRegisterMode) Icons.Rounded.PersonAdd else Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isRegisterMode) "Create Parent Account" else "Parent Account Login",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (isRegisterMode) "Create a master account to manage child devices from anywhere." else "Log in to sync and control connected child devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mode Toggle (Sign In vs Register)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isRegisterMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isRegisterMode = false
                                errorMessage = null
                            }
                    ) {
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (!isRegisterMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isRegisterMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isRegisterMode = true
                                errorMessage = null
                            }
                    ) {
                        Text(
                            text = "Register",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRegisterMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }

                // Error Banner
                errorMessage?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Username Input with "Check" button
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            usernameCheckStatus = null
                            isUsernameAvailable = false
                        },
                        label = { Text("Unique Username") },
                        placeholder = { Text("e.g. alex_parent") },
                        leadingIcon = { Icon(Icons.Rounded.AlternateEmail, contentDescription = null) },
                        trailingIcon = {
                            if (isRegisterMode && username.trim().length >= 3) {
                                TextButton(
                                    onClick = {
                                        isCheckingUsername = true
                                        ParentCloudAuthManager.checkUsernameAvailability(username) { available, msg ->
                                            isCheckingUsername = false
                                            isUsernameAvailable = available
                                            usernameCheckStatus = msg
                                        }
                                    },
                                    enabled = !isCheckingUsername
                                ) {
                                    if (isCheckingUsername) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("Check", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    usernameCheckStatus?.let { status ->
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUsernameAvailable) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text("Min 6 characters") },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Email (Only in Register Mode)
                if (isRegisterMode) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email (Optional)") },
                        placeholder = { Text("For future password recovery") },
                        leadingIcon = { Icon(Icons.Rounded.MailOutline, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 12-Word Secret Recovery Phrase Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("12-Word Recovery Phrase", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }

                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Recovery Phrase", generatedPhrase.joinToString(" "))
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Recovery phrase copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 12.sp)
                                }
                            }

                            Text(
                                "Write down these 12 secret words in a safe place. You can use them to recover your account if you forget your password.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 12 Words Grid (3 columns x 4 rows)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (row in 0 until 4) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        for (col in 0 until 3) {
                                            val index = row * 3 + col
                                            val word = generatedPhrase[index]
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${index + 1}.",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = word,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Medium,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Skip Recovery Option Toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (enableRecoveryPhrase) {
                                            showSkipWarningDialog = true
                                        } else {
                                            enableRecoveryPhrase = true
                                        }
                                    }
                            ) {
                                Checkbox(
                                    checked = !enableRecoveryPhrase,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            showSkipWarningDialog = true
                                        } else {
                                            enableRecoveryPhrase = true
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "I don't want recovery phrase (Skip)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.trim().isBlank() || password.trim().length < 6) {
                        errorMessage = "Please enter a valid username and password (min 6 characters)."
                        return@Button
                    }
                    isSubmitting = true
                    errorMessage = null

                    if (isRegisterMode) {
                        ParentCloudAuthManager.registerParentAccount(
                            context = context,
                            username = username,
                            password = password,
                            email = email.ifBlank { null },
                            recoveryPhrase = if (enableRecoveryPhrase) generatedPhrase else null
                        ) { success, msg ->
                            isSubmitting = false
                            if (success) {
                                onAuthSuccess(msg)
                                onDismiss()
                            } else {
                                errorMessage = msg
                            }
                        }
                    } else {
                        ParentCloudAuthManager.loginParentAccount(
                            context = context,
                            usernameOrEmail = username,
                            password = password
                        ) { success, msg ->
                            isSubmitting = false
                            if (success) {
                                onAuthSuccess(msg)
                                onDismiss()
                            } else {
                                errorMessage = msg
                            }
                        }
                    }
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isRegisterMode) "Create Account" else "Sign In")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )

    // Skip Recovery Phrase Warning Disclaimer Modal
    if (showSkipWarningDialog) {
        var disclaimerAgreed by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSkipWarningDialog = false },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Disable Recovery Protection?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "If you disable recovery phrases and do not provide an email, your account CANNOT be recovered if you forget your password.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "You will lose parental access to manage or unlink connected child devices and will have to reset all controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { disclaimerAgreed = !disclaimerAgreed }
                    ) {
                        Checkbox(checked = disclaimerAgreed, onCheckedChange = { disclaimerAgreed = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("I understand and accept the risk", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        enableRecoveryPhrase = false
                        showSkipWarningDialog = false
                    },
                    enabled = disclaimerAgreed,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Skip Recovery")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipWarningDialog = false }) {
                    Text("Keep Phrase")
                }
            }
        )
    }
}

// =============================================================================
// BYOB (BRING YOUR OWN BACKEND) CONFIGURATION DIALOG
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ByobConfigDialog(
    onDismiss: () -> Unit,
    onConfigSaved: (ByobConfig) -> Unit
) {
    val context = LocalContext.current
    val currentConfig by ByobConfigManager.configFlow.collectAsState()

    var jsonInput by remember { mutableStateOf(if (currentConfig.isConfigured) ByobConfigManager.exportConfigJson(currentConfig) else "") }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isEnabled by remember { mutableStateOf(currentConfig.isEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("BYOB (Custom Backend)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "For advanced users: Connect your own self-hosted Firebase Realtime Database. Paste your project's configuration JSON below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Enable Custom Backend", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(if (isEnabled) "Active" else "Using default ScreenHarmony Cloud", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                    }
                }

                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = {
                        jsonInput = it
                        parseError = null
                    },
                    label = { Text("Firebase Config JSON") },
                    placeholder = {
                        Text(
                            "{\n  \"projectId\": \"my-family\",\n  \"databaseUrl\": \"https://my-family-default-rtdb.firebaseio.com\",\n  \"apiKey\": \"AIzaSy...\"\n}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    },
                    minLines = 6,
                    maxLines = 10,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                parseError?.let { err ->
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(10.dp))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Note: The exact same configuration JSON must also be imported on child devices when pairing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isEnabled) {
                        ByobConfigManager.disableByob(context)
                        Toast.makeText(context, "Reverted to default ScreenHarmony Cloud", Toast.LENGTH_SHORT).show()
                        onDismiss()
                        return@Button
                    }

                    if (jsonInput.trim().isBlank()) {
                        parseError = "Please paste your Firebase configuration JSON."
                        return@Button
                    }

                    val parseResult = ByobConfigManager.parseConfigJson(jsonInput)
                    parseResult.onSuccess { config ->
                        ByobConfigManager.saveConfig(
                            context = context,
                            projectId = config.projectId,
                            databaseUrl = config.databaseUrl,
                            apiKey = config.apiKey,
                            isEnabled = true
                        )
                        onConfigSaved(config)
                        Toast.makeText(context, "BYOB Custom Backend configured successfully!", Toast.LENGTH_LONG).show()
                        onDismiss()
                    }.onFailure { err ->
                        parseError = "Invalid JSON: ${err.message}"
                    }
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save & Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
