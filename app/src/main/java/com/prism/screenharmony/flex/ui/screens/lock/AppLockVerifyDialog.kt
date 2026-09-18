package com.prism.screenharmony.flex.ui.screens.lock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prism.screenharmony.flex.data.AppLockManager
import com.prism.screenharmony.flex.ui.components.CustomPinKeypad
import com.prism.screenharmony.flex.ui.components.PinDotsDisplay
import com.prism.screenharmony.flex.ui.components.SecureFlagEffect

@Composable
fun AppLockVerifyDialog(
    title: String = "Verify PIN",
    subtitle: String = "Enter your current PIN to turn off App Lock",
    onVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    var inputPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SecureFlagEffect()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isCompactHeight = maxHeight < 560.dp
                val isCompactWidth = maxWidth < 340.dp
                val isSmallScreen = isCompactHeight || isCompactWidth

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isSmallScreen) 12.dp else 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(if (isSmallScreen) 32.dp else 48.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = if (isSmallScreen) 8.dp else 16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(if (isSmallScreen) 44.dp else 64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.LockOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(if (isSmallScreen) 22.dp else 32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(if (isSmallScreen) 6.dp else 16.dp))
                        Text(
                            text = title,
                            style = if (isSmallScreen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (!isSmallScreen) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 20.dp))

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
                                onVerified()
                            } else {
                                isError = true
                                errorMessage = "Incorrect PIN. Try again."
                                inputPin = ""
                            }
                        },
                        isSubmitEnabled = inputPin.length >= 4
                    )
                }
            }
        }
    }
}
