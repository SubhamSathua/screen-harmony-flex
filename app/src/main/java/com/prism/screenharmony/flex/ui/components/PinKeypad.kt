package com.prism.screenharmony.flex.ui.components

import android.app.Activity
import android.graphics.Matrix
import android.view.WindowManager
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
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
 * Material 3 Shape-Morphing Dot powered by official androidx.graphics.shapes:
 * Sequence: Sunny (Starburst) -> Pentagon -> Arrow -> Circle -> Sunny
 */
@Composable
fun M3ShapeMorphingDot(
    index: Int,
    isError: Boolean,
    size: Dp = 24.dp
) {
    // 1. Define the 4 target Material 3 polygons (normalized unit coordinate space)
    val sunnyShape = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.65f,
            rounding = CornerRounding(radius = 0.15f)
        )
    }

    val pentagonShape = remember {
        RoundedPolygon(
            numVertices = 5,
            rounding = CornerRounding(radius = 0.2f)
        )
    }

    val eightLeafShape = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.78f,
            rounding = CornerRounding(radius = 0.5f)
        )
    }

    val circleShape = remember {
        RoundedPolygon.circle(
            numVertices = 12
        )
    }

    // 2. Official AndroidX Morphs between adjacent stages
    val morphSunnyToPentagon = remember { Morph(sunnyShape, pentagonShape) }
    val morphPentagonToEightLeaf = remember { Morph(pentagonShape, eightLeafShape) }
    val morphEightLeafToCircle   = remember { Morph(eightLeafShape, circleShape) }

    // 3. One-shot shape morph sequence (Sunny -> Pentagon -> 8-Leaf Circle -> Circle) total duration = 1 sec (1000ms)
    val morphProgress = remember { Animatable(0f) }
    val entranceScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            entranceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f)
            )
        }
        launch {
            morphProgress.animateTo(
                targetValue = 3f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            )
        }
    }

    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .size(size)
            .scale(entranceScale.value)
    ) {
        val progress = morphProgress.value.coerceIn(0f, 3f)
        val segmentIndex = progress.toInt().coerceIn(0, 2)
        val segmentFraction = if (progress >= 3f) 1f else (progress - segmentIndex).coerceIn(0f, 1f)

        val currentMorph = when {
            progress >= 3f -> morphEightLeafToCircle
            segmentIndex == 0 -> morphSunnyToPentagon
            segmentIndex == 1 -> morphPentagonToEightLeaf
            else -> morphEightLeafToCircle
        }

        // Convert to Android graphics Path at evaluated progress fraction
        val androidPath = currentMorph.toPath(progress = segmentFraction)

        // Scale and center the unit path inside Canvas bounds
        val matrix = Matrix()
        val minDimension = this.size.minDimension
        matrix.postScale(minDimension / 2.2f, minDimension / 2.2f)
        matrix.postTranslate(this.size.width / 2f, this.size.height / 2f)
        androidPath.transform(matrix)

        // Draw natively on Compose Canvas
        drawPath(
            path = androidPath.asComposePath(),
            color = color
        )
    }
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

    val dotSize = when {
        pinLength > 8 -> 14.dp
        pinLength > 6 -> 18.dp
        else -> 22.dp
    }
    val dotSpacing = when {
        pinLength > 8 -> 6.dp
        pinLength > 6 -> 8.dp
        else -> 10.dp
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.offset(x = shakeOffset.value.dp)
    ) {
        Box(
            modifier = Modifier
                .height(34.dp)
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (pinLength > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dotSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until pinLength) {
                        key(i) {
                            M3ShapeMorphingDot(
                                index = i,
                                isError = isError,
                                size = dotSize
                            )
                        }
                    }
                }
            }
        }

        if (showCounter) {
            Spacer(modifier = Modifier.height(2.dp))
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        val availableWidth = maxWidth
        val isCompact = availableWidth < 340.dp
        val isUltraCompact = availableWidth < 280.dp

        val horizontalPad = when {
            isUltraCompact -> 6.dp
            isCompact -> 12.dp
            else -> 20.dp
        }
        val buttonSpacing = when {
            isUltraCompact -> 4.dp
            isCompact -> 8.dp
            else -> 12.dp
        }
        val rowSpacing = when {
            isUltraCompact -> 4.dp
            isCompact -> 8.dp
            else -> 12.dp
        }

        val calculatedButtonSize = ((availableWidth - (horizontalPad * 2) - (buttonSpacing * 2)) / 3)
            .coerceIn(40.dp, 72.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPad)
                .padding(bottom = if (isCompact) 6.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(rowSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        when (key) {
                            "backspace" -> {
                                KeypadIconButton(
                                    icon = Icons.AutoMirrored.Rounded.Backspace,
                                    contentDescription = "Backspace",
                                    size = calculatedButtonSize,
                                    onClick = onBackspace
                                )
                            }
                            "submit" -> {
                                KeypadActionButton(
                                    icon = submitIcon,
                                    enabled = isSubmitEnabled,
                                    size = calculatedButtonSize,
                                    onClick = onSubmit
                                )
                            }
                            else -> {
                                KeypadDigitButton(
                                    digit = key,
                                    size = calculatedButtonSize,
                                    onClick = { onDigitPress(key) }
                                )
                            }
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
    size: Dp = 72.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        shape = CircleShape,
        color = if (isPressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                style = if (size < 54.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,
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
    size: Dp = 72.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        shape = CircleShape,
        color = if (isPressed) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (size < 54.dp) 20.dp else 26.dp)
            )
        }
    }
}

@Composable
private fun KeypadActionButton(
    icon: ImageVector,
    enabled: Boolean,
    size: Dp = 72.dp,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "Confirm",
                tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(if (size < 54.dp) 20.dp else 26.dp)
            )
        }
    }
}
