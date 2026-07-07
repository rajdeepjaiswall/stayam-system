package `in`.getdownfoundation.sahusales.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.getdownfoundation.sahusales.core.Contact
import `in`.getdownfoundation.sahusales.core.CreateEventRequest
import `in`.getdownfoundation.sahusales.core.CreateReminderInput
import `in`.getdownfoundation.sahusales.core.Event
import `in`.getdownfoundation.sahusales.core.EventTag
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.dashboard.formatTime
import `in`.getdownfoundation.sahusales.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventsScreen(viewModel: MainViewModel) {
    val events by viewModel.events.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val tags by viewModel.eventTags.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val filtered = remember(events, selectedStatus) {
        if (selectedStatus == null) events else events.filter { it.status == selectedStatus }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }, containerColor = Primary) {
                Icon(Icons.Default.Add, contentDescription = "Create Event", tint = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Events", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary,
                modifier = Modifier.padding(16.dp))

            ScrollableTabRow(
                selectedTabIndex = listOf(null, "upcoming", "completed", "cancelled")
                    .indexOf(selectedStatus).coerceAtLeast(0),
                containerColor = Surface,
                contentColor = Primary,
                edgePadding = 8.dp
            ) {
                listOf("All" to null, "Upcoming" to "upcoming", "Completed" to "completed", "Cancelled" to "cancelled")
                    .forEachIndexed { _, (label, status) ->
                        Tab(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status; viewModel.loadEvents(status) }
                        ) {
                            Text(label, modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp))
                        }
                    }
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(filtered) { e -> EventCard(e) }
            }
        }
    }

    if (showCreate) {
        CreateEventSheet(
            contacts = contacts,
            tags = tags,
            onDismiss = { showCreate = false },
            onSave = { body ->
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) { viewModel.api()?.createEvent(body) }
                        if (resp?.isSuccessful == true) {
                            viewModel.loadEvents()
                            viewModel.loadReminders()
                            showCreate = false
                        } else {
                            snackbarHostState.showSnackbar(resp?.errorBody()?.string() ?: "Failed")
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(e.message ?: "Error")
                    }
                }
            }
        )
    }
}

@Composable
fun EventCard(event: Event) {
    val tagColor = try {
        Color(android.graphics.Color.parseColor(event.tagColor ?: "#1565C0"))
    } catch (e: Exception) { Primary }

    val statusColor = when (event.status) {
        "completed" -> StatusArchived
        "cancelled" -> StatusOverdue
        else -> StatusUpcoming
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(5.dp).fillMaxHeight()
                .background(statusColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)))
            Column(Modifier.padding(12.dp).weight(1f)) {
                Row {
                    event.tagName?.let {
                        Surface(color = tagColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                            Text(it.uppercase(), fontSize = 10.sp, color = tagColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(event.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                event.contactName?.let { Text(it, fontSize = 14.sp, color = TextPrimary) }
                event.contactOrganisation?.let { Text(it, fontSize = 13.sp, color = TextSecondary) }
                if (event.reminders.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    event.reminders.take(2).forEach { r ->
                        Text("⏰ ${formatTime(r.remindAt)}", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

/** Formats an ISO-8601 UTC string like "2024-12-31T09:00:00Z" into "Dec 31, 2024  9:00 AM" */
private fun formatReminderDisplay(iso: String): String {
    return try {
        val parse = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        parse.timeZone = TimeZone.getTimeZone("UTC")
        val date = parse.parse(iso) ?: return iso
        val display = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US)
        display.timeZone = TimeZone.getDefault()
        display.format(date)
    } catch (_: Exception) { iso }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventSheet(
    contacts: List<Contact>,
    tags: List<EventTag>,
    onDismiss: () -> Unit,
    onSave: (CreateEventRequest) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var selectedTag by remember { mutableStateOf<EventTag?>(null) }
    var reminderTimes by remember { mutableStateOf(listOf("")) }
    var contactExpanded by remember { mutableStateOf(false) }
    var tagExpanded by remember { mutableStateOf(false) }

    // Date-time picker state
    var editingReminderIdx by remember { mutableStateOf(-1) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = 9, initialMinute = 0, is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title *") }, modifier = Modifier.fillMaxWidth()
                )

                // Contact picker
                ExposedDropdownMenuBox(expanded = contactExpanded, onExpandedChange = { contactExpanded = it }) {
                    OutlinedTextField(
                        value = selectedContact?.name ?: "Select Contact",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Contact") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contactExpanded) }
                    )
                    ExposedDropdownMenu(expanded = contactExpanded, onDismissRequest = { contactExpanded = false }) {
                        contacts.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) },
                                onClick = { selectedContact = c; contactExpanded = false })
                        }
                    }
                }

                // Tag picker
                ExposedDropdownMenuBox(expanded = tagExpanded, onExpandedChange = { tagExpanded = it }) {
                    OutlinedTextField(
                        value = selectedTag?.name ?: "Select Tag",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Tag") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) }
                    )
                    ExposedDropdownMenu(expanded = tagExpanded, onDismissRequest = { tagExpanded = false }) {
                        tags.forEach { t ->
                            DropdownMenuItem(text = { Text(t.name) },
                                onClick = { selectedTag = t; tagExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()
                )

                // Reminder pickers — tap button to open date → time picker
                Text("Reminders", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                reminderTimes.forEachIndexed { i, time ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { editingReminderIdx = i; showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (time.isBlank()) "Reminder ${i + 1} — tap to set"
                                       else formatReminderDisplay(time),
                                maxLines = 1,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                reminderTimes = reminderTimes.toMutableList().also { it.removeAt(i) }
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove reminder",
                                tint = TextSecondary)
                        }
                    }
                }
                TextButton(onClick = { reminderTimes = reminderTimes + "" }) {
                    Text("+ Add Reminder")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) return@Button
                val reminders = reminderTimes.filter { it.isNotBlank() }.map { CreateReminderInput(it) }
                onSave(CreateEventRequest(
                    title = title.trim(),
                    notes = notes.ifBlank { null },
                    contactId = selectedContact?.id,
                    tagId = selectedTag?.id,
                    reminders = reminders
                ))
            }) { Text("CREATE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )

    // ── Date picker dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next: Set Time →") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Time picker dialog ────────────────────────────────────────────────────
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set Time") },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Build ISO-8601 UTC string from picked date + time.
                    // DatePicker gives midnight UTC for the selected date — extract Y/M/D from that.
                    // Then set date+time on a LOCAL timezone calendar so IST (or any offset) is
                    // applied correctly before converting to UTC for storage.
                    val selectedMillis = datePickerState.selectedDateMillis
                        ?: System.currentTimeMillis()
                    val utcForDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    utcForDate.timeInMillis = selectedMillis
                    val localCal = Calendar.getInstance() // device timezone (e.g. IST = UTC+5:30)
                    localCal.set(
                        utcForDate.get(Calendar.YEAR),
                        utcForDate.get(Calendar.MONTH),
                        utcForDate.get(Calendar.DAY_OF_MONTH),
                        timePickerState.hour,
                        timePickerState.minute,
                        0
                    )
                    localCal.set(Calendar.MILLISECOND, 0)
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val iso = sdf.format(localCal.time)
                    if (editingReminderIdx >= 0 && editingReminderIdx < reminderTimes.size) {
                        reminderTimes = reminderTimes.toMutableList()
                            .also { it[editingReminderIdx] = iso }
                    }
                    showTimePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    showDatePicker = true   // go back to date picker
                }) { Text("← Back") }
            }
        )
    }
}
