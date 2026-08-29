package com.prism.screenharmony.flex.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SecureFlagEffect() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

@Composable
fun PinDotsDisplay(
    pinLength: Int,
    maxDigits: Int = 12,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isError) {
        if (isError) {
            coroutineScope.launch {
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 400
                        0f at 0
                        (-20f) at 50
                        20f at 100
                        (-15f) at 150
                        15f at 200
                        (-10f) at 250
                        10f at 300
                        (-5f) at 350
                        0f at 400
                    }
                )
            }
        }
    }

    Row(
        modifier = modifier
            .offset(x = shakeOffset.value.dp)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayCount = maxOf(4, minOf(pinLength + (if (pinLength < maxDigits) 1 else 0), 8))
        for (i in 0 until displayCount) {
            val isFilled = i < pinLength
            val dotColor = when {
                isError -> MaterialTheme.colorScheme.error
                isFilled -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
            Surface(
                shape = CircleShape,
                color = dotColor,
                modifier = Modifier
                    .size(if (isFilled) 16.dp else 12.dp)
                    .scale(if (isFilled) 1.1f else 1.0f)
            ) {}
        }
    }
}

@Composable
fun CustomPinKeypad(
    onDigitPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    isSubmitEnabled: Boolean = false,
    submitIcon: ImageVector = Icons.Rounded.Check,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("backspace", "0", "submit")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    when (key) {
                        "backspace" -> {
                            KeypadIconButton(
                                icon = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Backspace",
                                onClick = onBackspace
                            )
                        }
                        "submit" -> {
                            KeypadActionButton(
                                icon = submitIcon,
                                enabled = isSubmitEnabled,
                                onClick = onSubmit
                            )
                        }
                        else -> {
                            KeypadDigitButton(
                                digit = key,
                                onClick = { onDigitPress(key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadDigitButton(
    digit: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        shape = CircleShape,
        color = if (isPressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun KeypadIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        shape = CircleShape,
        color = if (isPressed) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun KeypadActionButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "Confirm",
                tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
