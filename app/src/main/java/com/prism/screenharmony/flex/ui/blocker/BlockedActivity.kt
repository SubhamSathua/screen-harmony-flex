package com.prism.screenharmony.flex.ui.blocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.prism.screenharmony.flex.ui.theme.ScreenHarmonyFlexTheme
import kotlinx.coroutines.delay

class BlockedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val target = intent.getStringExtra("TARGET") ?: "App"
        val ruleName = intent.getStringExtra("RULE_NAME") ?: "App Block"
        val isWebsite = intent.getBooleanExtra("IS_WEBSITE", false)
        val showQuote = intent.getBooleanExtra("SHOW_QUOTE", false)
        val delaySeconds = intent.getIntExtra("DELAY_SECONDS", 0)

        val (displayName, appIcon) = if (!isWebsite) {
            getAppDetails(target)
        } else {
            Pair(target, null)
        }

        setContent {
            ScreenHarmonyFlexTheme {
                BlockedWallScreen(
                    targetName = displayName,
                    appIcon = appIcon,
                    ruleName = ruleName,
                    isWebsite = isWebsite,
                    showQuote = showQuote,
                    delaySeconds = delaySeconds,
                    onGoHome = { navigateHome() }
                )
            }
        }
    }

    private fun getAppDetails(packageName: String): Pair<String, ImageBitmap?> {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            val name = pm.getApplicationLabel(info).toString()
            val icon = pm.getApplicationIcon(info).toBitmap().asImageBitmap()
            Pair(name, icon)
        } catch (e: Exception) {
            Pair(packageName, null)
        }
    }

    private fun navigateHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        navigateHome()
    }
}

@Composable
fun BlockedWallScreen(
    targetName: String,
    appIcon: ImageBitmap?,
    ruleName: String,
    isWebsite: Boolean,
    showQuote: Boolean,
    delaySeconds: Int,
    onGoHome: () -> Unit
) {
    val quotes = remember {
        listOf(
            "Focus on what truly matters today.",
            "Your future self will thank you for closing this.",
            "Stay disciplined. Great things take time.",
            "Take a deep breath and reset your focus.",
            "Small daily choices lead to massive results."
        )
    }
    val quote = remember { quotes.random() }

    var timeLeft by remember { mutableIntStateOf(delaySeconds) }
    val isButtonEnabled = timeLeft <= 0

    val progressAnimatable = remember { Animatable(if (delaySeconds > 0) 0f else 1f) }

    LaunchedEffect(delaySeconds) {
        if (delaySeconds > 0) {
            progressAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = delaySeconds * 1000, easing = LinearEasing)
            )
        }
    }

    LaunchedEffect(delaySeconds) {
        if (delaySeconds > 0) {
            for (i in delaySeconds downTo 0) {
                timeLeft = i
                if (i > 0) delay(1000)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Target Icon
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = targetName,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(68.dp)
                ) {
                    Icon(
                        imageVector = if (isWebsite) Icons.Rounded.Language else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = targetName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Connected Lock Card (with or without Quote)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isWebsite) "Website Blocked" else "App Blocked",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Restricted under rule: $ruleName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (showQuote) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "\"$quote\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Animated Interaction Button
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.94f else 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                label = "BtnScale"
            )

            Button(
                onClick = onGoHome,
                enabled = isButtonEnabled,
                interactionSource = interactionSource,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(52.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isButtonEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(Icons.Rounded.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isButtonEnabled) "Go to Home Screen" else "Wait ${timeLeft}s",
                    fontWeight = FontWeight.Bold
                )
            }

            // Progress bar if delay countdown is active
            if (!isButtonEnabled) {
                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnimatable.value)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}
