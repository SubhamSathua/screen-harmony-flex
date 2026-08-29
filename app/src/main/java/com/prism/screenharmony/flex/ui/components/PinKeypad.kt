package com.prism.screenharmony.flex.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 * Material 3 Expressive Shape Morphing Dot:
 * Continuously morphs from a Circle (progress = 0) -> Squircle -> 4-Sided Rounded Star (progress = 1)
 * via cubic Bézier control point interpolation with spring dynamics.
 */
@Composable
fun ExpressiveMorphingStarDot(
    isError: Boolean,
    size: Dp = 22.dp
) {
    var isEntered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isEntered = true
    }

    // Morph progress: 0f (Circle) -> 1f (4-Sided Expressive Star)
    val morphProgress by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 520f),
        label = "ShapeMorphProgress"
    )

    // Bouncy scale entrance
    val scale by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0.2f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "StarScale"
    )

    // Dynamic rotational entrance
    val rotation by animateFloatAsState(
        targetValue = if (isEntered) 0f else -45f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
        label = "StarRotation"
    )

    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .rotate(rotation)
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) / 2f

        // Circle cubic handle constant
        val kCircle = 0.55228475f * r
        // Star inner pinch control coordinate
        val kStar = 0.26f * r

        // Interpolate control handles between Circle (t=0) and Expressive Star (t=1)
        val t = morphProgress.coerceIn(0f, 1f)

        // Curve 1: Top (0, -r) to Right (r, 0)
        val cp1x_c1 = lerp(cx + kCircle, cx, t)
        val cp1y_c1 = lerp(cy - r, cy - kStar, t)
        val cp2x_c1 = lerp(cx + r, cx + kStar, t)
        val cp2y_c1 = lerp(cy - kCircle, cy, t)

        // Curve 2: Right (r, 0) to Bottom (0, r)
        val cp1x_c2 = lerp(cx + r, cx + kStar, t)
        val cp1y_c2 = lerp(cy + kCircle, cy, t)
        val cp2x_c2 = lerp(cx + kCircle, cx, t)
        val cp2y_c2 = lerp(cy + r, cy + kStar, t)

        // Curve 3: Bottom (0, r) to Left (-r, 0)
        val cp1x_c3 = lerp(cx - kCircle, cx, t)
        val cp1y_c3 = lerp(cy + r, cy + kStar, t)
        val cp2x_c3 = lerp(cx - r, cx - kStar, t)
        val cp2y_c3 = lerp(cy + kCircle, cy, t)

        // Curve 4: Left (-r, 0) to Top (0, -r)
        val cp1x_c4 = lerp(cx - r, cx - kStar, t)
        val cp1y_c4 = lerp(cy - kCircle, cy, t)
        val cp2x_c4 = lerp(cx - kCircle, cx, t)
        val cp2y_c4 = lerp(cy - r, cy - kStar, t)

        val path = Path().apply {
            moveTo(cx, cy - r)
            cubicTo(cp1x_c1, cp1y_c1, cp2x_c1, cp2y_c1, cx + r, cy)
            cubicTo(cp1x_c2, cp1y_c2, cp2x_c2, cp2y_c2, cx, cy + r)
            cubicTo(cp1x_c3, cp1y_c3, cp2x_c3, cp2y_c3, cx - r, cy)
            cubicTo(cp1x_c4, cp1y_c4, cp2x_c4, cp2y_c4, cx, cy - r)
            close()
        }

        drawPath(path = path, color = color)
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
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
                            ExpressiveMorphingStarDot(
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
