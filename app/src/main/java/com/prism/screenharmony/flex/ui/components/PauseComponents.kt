package com.prism.screenharmony.flex.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prism.screenharmony.flex.data.PauseConfig
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun DelaySliderView(
    initialSeconds: Int,
    onSave: (Int) -> Unit,
    onBack: () -> Unit
) {
    var seconds by remember { mutableIntStateOf(initialSeconds) }

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Set Delay Duration", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${seconds}s",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Slider(
                value = seconds.toFloat(),
                onValueChange = { seconds = it.roundToInt() },
                valueRange = 3f..60f,
                steps = 19
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("3s", style = MaterialTheme.typography.labelSmall)
                Text("60s", style = MaterialTheme.typography.labelSmall)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(
                onClick = { onSave(seconds) },
                modifier = Modifier.weight(1f),
                shape = CircleShape
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
fun TypeTextConfigView(
    initialLength: Int,
    initialCount: Int,
    onSave: (Int, Int) -> Unit,
    onBack: () -> Unit
) {
    var length by remember { mutableIntStateOf(initialLength) }
    var count by remember { mutableIntStateOf(initialCount) }

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Type Text Challenge", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Word Length: $length characters", style = MaterialTheme.typography.titleMedium)
            Slider(value = length.toFloat(), onValueChange = { length = it.roundToInt() }, valueRange = 3f..12f)

            Text("Number of Words: $count words", style = MaterialTheme.typography.titleMedium)
            Slider(value = count.toFloat(), onValueChange = { count = it.roundToInt() }, valueRange = 1f..6f)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(
                onClick = { onSave(length, count) },
                modifier = Modifier.weight(1f),
                shape = CircleShape
            ) {
                Text("Save")
            }
        }
    }
}
