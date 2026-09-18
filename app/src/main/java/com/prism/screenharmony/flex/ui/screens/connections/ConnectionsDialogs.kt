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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
// PARENT CLOUD AUTH DIALOG (MULTI-STEP REGISTRATION & LOGIN)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentCloudAuthDialog(
    isInitialRegister: Boolean = false,
    onDismiss: () -> Unit,
    onAuthSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    var isRegisterMode by remember { mutableStateOf(isInitialRegister) }
    var registrationStep by remember { mutableStateOf(1) } // 1: Info, 2: Recovery

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
    var hasCopiedPhrase by remember { mutableStateOf(false) }
    var hasDownloadedPhrase by remember { mutableStateOf(false) }
    var hasConfirmedSaved by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = {
            if (!isSubmitting) onDismiss()
        },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSubmitting,
            dismissOnClickOutside = !isSubmitting
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 480.dp)
                .heightIn(max = 700.dp)
                .padding(vertical = 12.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isCompact = maxWidth < 340.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isCompact) 14.dp else 22.dp)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(if (isCompact) 36.dp else 42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (!isRegisterMode) {
                                            Icons.Rounded.AccountCircle
                                        } else if (registrationStep == 1) {
                                            Icons.Rounded.PersonAdd
                                        } else {
                                            Icons.Rounded.Key
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = if (!isRegisterMode) {
                                        "Parent Login"
                                    } else if (registrationStep == 1) {
                                        "Create Account"
                                    } else {
                                        "Recovery Phrase"
                                    },
                                    style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (!isRegisterMode) {
                                        "Sign in to sync family rules"
                                    } else if (registrationStep == 1) {
                                        "Step 1/2: Account credentials"
                                    } else {
                                        "Step 2/2: Security keys"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!isSubmitting) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                // Mode Toggle (Sign In vs Register) - Only on Step 1
                if (registrationStep == 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
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
                                text = "Create Account",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isRegisterMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                // Error Message Banner
                errorMessage?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // =====================================================
                // BODY: SIGN IN OR REGISTER STEP 1 OR REGISTER STEP 2
                // =====================================================

                if (!isRegisterMode) {
                    // SIGN IN FORM
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username or Email") },
                            placeholder = { Text("e.g. parent_alex or alex@mail.com") },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            placeholder = { Text("Your password") },
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

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                if (username.trim().isBlank() || password.trim().isBlank()) {
                                    errorMessage = "Please enter both username and password."
                                    return@Button
                                }
                                isSubmitting = true
                                errorMessage = null
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
                            },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Signing in...")
                            } else {
                                Text("Sign In to Account", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (registrationStep == 1) {
                    // REGISTER STEP 1: USERNAME, PASSWORD, EMAIL
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Username Field with "Check"
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = {
                                    username = it
                                    usernameCheckStatus = null
                                    isUsernameAvailable = false
                                },
                                label = { Text("Username") },
                                placeholder = { Text("e.g. parent_alex") },
                                leadingIcon = { Icon(Icons.Rounded.AlternateEmail, contentDescription = null) },
                                trailingIcon = {
                                    if (username.trim().length >= 3) {
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

                        // Password Field
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

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            placeholder = { Text("e.g. alex@example.com") },
                            leadingIcon = { Icon(Icons.Rounded.MailOutline, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Next Button
                        Button(
                            onClick = {
                                val cleanUser = username.trim()
                                val cleanPass = password.trim()
                                val cleanEmail = email.trim()

                                if (cleanUser.length < 3) {
                                    errorMessage = "Username must be at least 3 characters."
                                    return@Button
                                }
                                if (cleanPass.length < 6) {
                                    errorMessage = "Password must be at least 6 characters."
                                    return@Button
                                }
                                if (cleanEmail.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                                    errorMessage = "Please enter a valid email address."
                                    return@Button
                                }

                                errorMessage = null
                                registrationStep = 2
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Next: Security & Recovery Key", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    // REGISTER STEP 2: 12-WORD RECOVERY PHRASE WITH COPY & DOWNLOAD
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Write down, copy, or download your 12-word secret recovery phrase. You will need it to restore parental access if you lose your password.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // 12-Words Grid (2 columns x 6 rows for maximum clarity and responsive widths)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (row in 0 until 6) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            for (col in 0 until 2) {
                                                val index = row * 2 + col
                                                val word = generatedPhrase[index]
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${(index + 1).toString().padStart(2, '0')}.",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = word,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // COPY & DOWNLOAD BUTTONS ROW
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Copy Button
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Recovery Phrase", generatedPhrase.joinToString(" "))
                                            clipboard.setPrimaryClip(clip)
                                            hasCopiedPhrase = true
                                            hasConfirmedSaved = true
                                            Toast.makeText(context, "12-word recovery phrase copied to clipboard! ✅", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = if (isCompact) 4.dp else 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (hasCopiedPhrase) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (hasCopiedPhrase) "Copied! ✅" else if (isCompact) "Copy" else "Copy Phrase",
                                            maxLines = 1,
                                            fontSize = if (isCompact) 11.sp else 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Download Button
                                    OutlinedButton(
                                        onClick = {
                                            downloadOrExportRecoveryPhrase(
                                                context = context,
                                                username = username,
                                                email = email,
                                                phrase = generatedPhrase
                                            )
                                            hasDownloadedPhrase = true
                                            hasConfirmedSaved = true
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = if (isCompact) 4.dp else 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (hasDownloadedPhrase) "Downloaded" else if (isCompact) "Download" else "Download Key",
                                            maxLines = 1,
                                            fontSize = if (isCompact) 11.sp else 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Confirmation Checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { hasConfirmedSaved = !hasConfirmedSaved }
                        ) {
                            Checkbox(
                                checked = hasConfirmedSaved,
                                onCheckedChange = { hasConfirmedSaved = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "I have safely copied or downloaded my recovery phrase.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Bottom Actions: Back & Create Account
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { registrationStep = 1 },
                                enabled = !isSubmitting,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.4f)
                                    .height(50.dp)
                            ) {
                                Text("Back")
                            }

                            Button(
                                onClick = {
                                    isSubmitting = true
                                    errorMessage = null
                                    ParentCloudAuthManager.registerParentAccount(
                                        context = context,
                                        username = username,
                                        password = password,
                                        email = email.ifBlank { null },
                                        recoveryPhrase = generatedPhrase
                                    ) { success, msg ->
                                        isSubmitting = false
                                        if (success) {
                                            onAuthSuccess(msg)
                                            onDismiss()
                                        } else {
                                            errorMessage = msg
                                        }
                                    }
                                },
                                enabled = !isSubmitting && hasConfirmedSaved,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.6f)
                                    .height(50.dp)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Creating...")
                                } else {
                                    Text("Create Account", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Saves and exports parent recovery credentials into a text file and launches Android share sheet.
 */
fun downloadOrExportRecoveryPhrase(
    context: Context,
    username: String,
    email: String,
    phrase: List<String>
) {
    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
    val content = buildString {
        appendLine("=======================================================")
        appendLine("SCREENHARMONY FLEX - PARENT MASTER RECOVERY KEY")
        appendLine("=======================================================")
        appendLine("Username  : @${username.ifBlank { "parent" }}")
        appendLine("Email     : ${if (email.isNotBlank()) email else "Not specified"}")
        appendLine("Generated : $dateStr")
        appendLine()
        appendLine("12-WORD RECOVERY PHRASE:")
        phrase.forEachIndexed { idx, word ->
            appendLine("  ${(idx + 1).toString().padStart(2, '0')}. $word")
        }
        appendLine()
        appendLine("RAW PHRASE (FOR IMPORT):")
        appendLine(phrase.joinToString(" "))
        appendLine()
        appendLine("=======================================================")
        appendLine("IMPORTANT:")
        appendLine("Keep this document safe and private. This phrase grants")
        appendLine("master administrative control over all paired child devices.")
        appendLine("=======================================================")
    }

    try {
        val fileName = "screenharmony-recovery-${username.ifBlank { "parent" }}-${System.currentTimeMillis() / 1000}.txt"
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
            val file = java.io.File(downloadsDir, fileName)
            file.writeText(content)
        }

        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "ScreenHarmony Flex - Recovery Key")
            putExtra(android.content.Intent.EXTRA_TEXT, content)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = android.content.Intent.createChooser(shareIntent, "Save or Share Recovery Key").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        Toast.makeText(context, "Recovery key generated & ready to save", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Recovery Phrase", content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Recovery credentials copied to clipboard!", Toast.LENGTH_SHORT).show()
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
