package com.prism.screenharmony.flex.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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

/**
 * Material 3 Expressive 4-sided rounded star shape
 */
val ExpressiveStarShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = minOf(cx, cy)
            val pinch = r * 0.28f // curve pinch factor for organic curved star

            moveTo(cx, cy - r)
            cubicTo(cx, cy - pinch, cx + pinch, cy, cx + r, cy)
            cubicTo(cx + pinch, cy, cx, cy + pinch, cx, cy + r)
            cubicTo(cx, cy + pinch, cx - pinch, cy, cx - r, cy)
            cubicTo(cx - pinch, cy, cx, cy - pinch, cx, cy - r)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun ExpressiveStarDot(
    isError: Boolean,
    size: Dp = 20.dp
) {
    var isEntered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isEntered = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "StarScale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isEntered) 0f else -45f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
        label = "StarRotation"
    )

    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Surface(
        shape = ExpressiveStarShape,
        color = color,
        modifier = Modifier
            .size(size)
            .scale(scale)
            .rotate(rotation)
    ) {}
}

@Composable
fun PinDotsDisplay(
    pinLength: Int,
    maxDigits: Int = 12,
    isError: Boolean = false,
    showCounter: Boolean = false,
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.offset(x = shakeOffset.value.dp)
    ) {
        Box(
            modifier = Modifier
                .height(36.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (pinLength > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until pinLength) {
                        key(i) {
                            ExpressiveStarDot(
                                isError = isError,
                                size = 22.dp
                            )
                        }
                    }
                }
            }
        }

        if (showCounter) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$pinLength / $maxDigits (min 4)",
                style = MaterialTheme.typography.labelSmall,
                color = if (pinLength >= 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
            .size(74.dp)
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
            .size(74.dp)
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
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
        modifier = Modifier
            .size(74.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "Confirm",
                tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
