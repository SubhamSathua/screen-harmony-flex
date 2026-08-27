package com.prism.screenharmony.flex.ui.blocker

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.prism.screenharmony.flex.ui.theme.ScreenHarmonyFlexTheme
import kotlinx.coroutines.delay

class BlockedActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ScreenHarmony_LockWall"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        renderUI(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.i(TAG, "BlockedActivity onNewIntent")
        renderUI(intent)
    }

    private fun renderUI(intent: Intent) {
        val target = intent.getStringExtra("TARGET") ?: "App"
        val isWebsite = intent.getBooleanExtra("IS_WEBSITE", false)
        val customQuote = intent.getStringExtra("QUOTE")
        val delaySeconds = intent.getIntExtra("DELAY_SECONDS", 5).coerceAtLeast(0)

        Log.i(TAG, "BlockedActivity renderUI for target='$target', isWebsite=$isWebsite, delay=${delaySeconds}s")

        val (displayName, appIcon) = if (!isWebsite) {
            getAppDetails(target)
        } else {
            Pair(target, null)
        }

        setContent {
            ScreenHarmonyFlexTheme {
                BlockWallScreen(
                    appName = displayName,
                    appIcon = appIcon,
                    isWebsite = isWebsite,
                    customQuote = customQuote,
                    delaySeconds = delaySeconds,
                    onClose = { goHome() }
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

    private fun goHome() {
        Log.d(TAG, "User clicked Go Home in BlockedActivity")
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting home intent", e)
        }
        finishAndRemoveTask()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        goHome()
    }
}

@Composable
fun BlockWallScreen(
    appName: String,
    appIcon: ImageBitmap?,
    isWebsite: Boolean,
    customQuote: String?,
    delaySeconds: Int,
    onClose: () -> Unit
) {
    val quotes = remember {
        listOf(
            "Focus on what matters. This app doesn't.",
            "Your future self will thank you for closing this.",
            "Is this really how you want to spend your time?",
            "One step closer to your goals if you stop now.",
            "Breathe. Reset. Do something meaningful.",
            "The secret of getting ahead is getting started.",
            "Don't watch the clock; do what it does. Keep going.",
            "Action is the foundational key to all success."
        )
    }
    val quote = remember { customQuote ?: quotes.random() }

    val totalWaitTime = (delaySeconds * 1000L).coerceAtLeast(0L)
    val progressAnimatable = remember { Animatable(if (totalWaitTime == 0L) 1f else 0f) }
    var timeLeft by remember { mutableIntStateOf(delaySeconds) }

    LaunchedEffect(delaySeconds) {
        if (totalWaitTime > 0L) {
            progressAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = totalWaitTime.toInt(), easing = LinearEasing)
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

    val isButtonEnabled = timeLeft <= 0

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = appName,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Take a Breath",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = com.prism.screenharmony.flex.ui.theme.NunitoFontFamily,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = quote,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        fontFamily = com.prism.screenharmony.flex.ui.theme.NunitoFontFamily,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.94f else 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                label = "ButtonScale"
            )

            Button(
                onClick = onClose,
                enabled = isButtonEnabled,
                interactionSource = interactionSource,
                modifier = Modifier
                    .widthIn(min = 160.dp, max = 240.dp)
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isButtonEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(Icons.Rounded.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isButtonEnabled) "Go Home" else "Wait ${timeLeft}s",
                    fontFamily = if (isButtonEnabled) com.prism.screenharmony.flex.ui.theme.NunitoFontFamily else com.prism.screenharmony.flex.ui.theme.JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isButtonEnabled) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(6.dp)
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
