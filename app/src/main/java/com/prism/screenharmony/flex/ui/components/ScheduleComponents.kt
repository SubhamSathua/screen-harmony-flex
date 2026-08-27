package com.prism.screenharmony.flex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.prism.screenharmony.flex.data.DayBitmask
import com.prism.screenharmony.flex.data.TimeSlot
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleGraph(timeSlots: List<TimeSlot>) {
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val timeLabels = listOf("12am", "6am", "12pm", "6pm", "12am")

    val allSegments = timeSlots.flatMap { slot ->
        val startTotalMin = slot.startTime.hour * 60 + slot.startTime.minute
        val endTotalMin = slot.endTime.hour * 60 + slot.endTime.minute

        DayBitmask.toNames(slot.dayBitmask).flatMap { day ->
            if (startTotalMin < endTotalMin) {
                listOf(day to startTotalMin..endTotalMin)
            } else if (startTotalMin > endTotalMin) {
                val dayIndex = dayNames.indexOf(day)
                val nextDay = dayNames[(dayIndex + 1) % 7]
                listOf(
                    day to startTotalMin..1440,
                    nextDay to 0..endTotalMin
                )
            } else {
                listOf(day to 0..1440)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            timeLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dayNames.forEachIndexed { index, name ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val color = Color.Gray.copy(alpha = 0.15f)
                            (1..3).forEach { i ->
                                val y = size.height * (i * 0.25f)
                                drawLine(color, Offset(0f, y), Offset(size.width, y), 1f)
                            }
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val parentHeight = maxHeight
                            allSegments.filter { it.first == name }.forEach { segment ->
                                val start = segment.second.start.toFloat()
                                val end = segment.second.endInclusive.toFloat()
                                val total = 1440f

                                val heightFraction = ((end - start) / total).coerceAtLeast(0.02f)
                                val offsetFraction = start / total

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(heightFraction)
                                        .offset(y = parentHeight * offsetFraction)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = dayLabels[index],
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyScheduleCard(timeSlots: List<TimeSlot>, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = "Weekly Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (timeSlots.isEmpty()) "Set specific days and active hours" else "${timeSlots.size} schedule rules configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
            if (timeSlots.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                ScheduleGraph(timeSlots)
            }
        }
    }
}

@Composable
fun TimeSelectionCard(label: String, time: LocalTime, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val formatter = DateTimeFormatter.ofPattern("hh:mm a")
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = time.format(formatter), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimeDialog(onDismiss: () -> Unit, onAdd: (TimeSlot) -> Unit) {
    var selectedDays by remember { mutableStateOf(setOf("Mon", "Tue", "Wed", "Thu", "Fri")) }
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(17, 0)) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Add Time Period", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Repeat on", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        days.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable {
                                        selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.first().toString(),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeSelectionCard(label = "Start Time", time = startTime, onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f))
                    TimeSelectionCard(label = "End Time", time = endTime, onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onAdd(
                                TimeSlot(
                                    dayBitmask = DayBitmask.fromNames(selectedDays),
                                    startMinute = startTime.hour * 60 + startTime.minute,
                                    endMinute = endTime.hour * 60 + endTime.minute
                                )
                            )
                        }
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(initialTime = startTime, onDismiss = { showStartTimePicker = false }, onTimeSelected = { startTime = it })
    }
    if (showEndTimePicker) {
        TimePickerDialog(initialTime = endTime, onDismiss = { showEndTimePicker = false }, onTimeSelected = { endTime = it })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initialTime: LocalTime, onDismiss: () -> Unit, onTimeSelected: (LocalTime) -> Unit) {
    val timePickerState = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                )
                TimePicker(state = timePickerState)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        onDismiss()
                    }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
