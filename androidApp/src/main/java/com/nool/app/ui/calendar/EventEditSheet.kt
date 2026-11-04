package com.nool.app.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nool.app.ui.theme.Dimens
import com.nool.core.model.CalendarEvent
import com.nool.core.model.ParsedRrule
import com.nool.core.model.RecurrenceFrequency
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val EVENT_COLORS = listOf(
    "#6750A4", // Purple
    "#00639B", // Blue
    "#006874", // Cyan
    "#386A20", // Green
    "#825500", // Amber
    "#BA1A1A", // Red
    "#7D5260"  // Pink
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditSheet(
    event: CalendarEvent?,
    initialDate: Date = Date(),
    availableNotes: List<com.nool.app.data.index.NoteEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onCreateNoteForEvent: ((title: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialStartCal = remember {
        Calendar.getInstance().apply {
            if (event != null) {
                timeInMillis = event.startTime
            } else {
                time = initialDate
                // Default to top of next hour
                val hour = get(Calendar.HOUR_OF_DAY)
                set(Calendar.HOUR_OF_DAY, hour + 1)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
        }
    }

    val initialEndCal = remember {
        (initialStartCal.clone() as Calendar).apply {
            if (event != null) {
                timeInMillis = event.endTime
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
            }
        }
    }

    var title by remember { mutableStateOf(event?.title ?: "") }
    var description by remember { mutableStateOf(event?.description ?: "") }
    var calendarId by remember { mutableStateOf(event?.calendarId ?: "Personal") }
    var isAllDay by remember { mutableStateOf(event?.isAllDay ?: false) }
    var selectedColor by remember { mutableStateOf(event?.colorHex ?: EVENT_COLORS[0]) }
    var reminderMinutes by remember { mutableStateOf(event?.reminderMinutes ?: 15) }
    var linkedNotePath by remember { mutableStateOf(event?.linkedNotePath) }

    var startMillis by remember { mutableLongStateOf(initialStartCal.timeInMillis) }
    var endMillis by remember { mutableLongStateOf(initialEndCal.timeInMillis) }

    val initialRule = remember { ParsedRrule.parse(event?.rrule) }
    var recurrenceFreq by remember { mutableStateOf(initialRule.freq) }

    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenHorizontalMargin)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Dimens.Spacing32)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (event == null) "New Event" else "Edit Event",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (event != null && onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete(event.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(Dimens.IconSmall))
                        Spacer(modifier = Modifier.width(Dimens.Spacing4))
                        Text("Delete")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing16))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Event Title") },
                placeholder = { Text("Meeting, Project milestone, Gym...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            )

            Spacer(modifier = Modifier.height(Dimens.Spacing12))

            // Calendar Category (Personal / Work)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Personal", "Work", "Ideas").forEach { cat ->
                    FilterChip(
                        selected = calendarId == cat,
                        onClick = { calendarId = cat },
                        label = { Text(cat) },
                        leadingIcon = if (calendarId == cat) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing12))

            // All Day switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(Dimens.Spacing8))
                    Text("All day", style = MaterialTheme.typography.bodyLarge)
                }
                Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing12))

            // Date and Time Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Date Button
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = startMillis }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val updatedStart = Calendar.getInstance().apply {
                                    timeInMillis = startMillis
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                val diff = endMillis - startMillis
                                startMillis = updatedStart.timeInMillis
                                endMillis = startMillis + diff
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(Dimens.IconSmall))
                    Spacer(modifier = Modifier.width(Dimens.Spacing4))
                    Text(dateFormat.format(Date(startMillis)), style = MaterialTheme.typography.bodySmall)
                }

                // Time Range
                if (!isAllDay) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = startMillis }
                            TimePickerDialog(
                                context,
                                { _, h, min ->
                                    val updatedStart = Calendar.getInstance().apply {
                                        timeInMillis = startMillis
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, min)
                                    }
                                    startMillis = updatedStart.timeInMillis
                                    if (endMillis <= startMillis) {
                                        endMillis = startMillis + 3600_000L
                                    }
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                false
                            ).show()
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(timeFormat.format(Date(startMillis)), style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = endMillis }
                            TimePickerDialog(
                                context,
                                { _, h, min ->
                                    val updatedEnd = Calendar.getInstance().apply {
                                        timeInMillis = endMillis
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, min)
                                    }
                                    endMillis = updatedEnd.timeInMillis.coerceAtLeast(startMillis + 900_000L)
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                false
                            ).show()
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(timeFormat.format(Date(endMillis)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing16))

            // Color Palette Selector
            Text("Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(Dimens.Spacing4))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                EVENT_COLORS.forEach { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = selectedColor.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing16))

            // Recurrence
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(Dimens.Spacing8))
                Text("Repeat", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                listOf(
                    RecurrenceFrequency.NONE to "Never",
                    RecurrenceFrequency.DAILY to "Daily",
                    RecurrenceFrequency.WEEKLY to "Weekly",
                    RecurrenceFrequency.MONTHLY to "Monthly",
                    RecurrenceFrequency.YEARLY to "Yearly"
                ).forEach { (freq, label) ->
                    FilterChip(
                        selected = recurrenceFreq == freq,
                        onClick = { recurrenceFreq = freq },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing16))

            // Reminder Selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(Dimens.Spacing8))
                Text("Reminder", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                listOf(
                    0 to "None",
                    5 to "5 min",
                    15 to "15 min",
                    30 to "30 min",
                    60 to "1 hour",
                    1440 to "1 day"
                ).forEach { (mins, label) ->
                    FilterChip(
                        selected = reminderMinutes == mins,
                        onClick = { reminderMinutes = mins },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing16))

            // Linked Note Integration
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(Dimens.Spacing8))
                Text("Linked Vault Note", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
            if (linkedNotePath != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = Dimens.Spacing12, vertical = Dimens.Spacing8)
                    ) {
                        Text(
                            text = linkedNotePath ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        TextButton(onClick = { linkedNotePath = null }) {
                            Text("Unlink")
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8)) {
                    OutlinedButton(
                        onClick = {
                            if (title.isNotBlank() && onCreateNoteForEvent != null) {
                                onCreateNoteForEvent(title)
                                linkedNotePath = "Notes/${title.trim()}.md"
                            }
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(Dimens.Spacing4))
                        Text("Create note for this event")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing16))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description & Agenda") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            )

            Spacer(modifier = Modifier.height(Dimens.Spacing24))

            // Save Action
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val finalRrule = ParsedRrule(freq = recurrenceFreq).toRruleString()
                    val saved = (event ?: CalendarEvent(
                        title = title.trim(),
                        startTime = startMillis,
                        endTime = endMillis
                    )).copy(
                        title = title.trim(),
                        description = description.trim(),
                        calendarId = calendarId,
                        startTime = startMillis,
                        endTime = endMillis,
                        isAllDay = isAllDay,
                        colorHex = selectedColor,
                        rrule = finalRrule,
                        reminderMinutes = reminderMinutes,
                        linkedNotePath = linkedNotePath,
                        lastModified = System.currentTimeMillis()
                    )
                    onSave(saved)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.MinTouchTarget),
                shape = MaterialTheme.shapes.medium,
                enabled = title.isNotBlank()
            ) {
                Text("Save Event")
            }
        }
    }
}
