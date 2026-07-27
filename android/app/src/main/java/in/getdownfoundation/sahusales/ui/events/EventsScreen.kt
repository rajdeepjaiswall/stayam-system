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
import `in`.getdownfoundation.sahusales.core.User
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.dashboard.formatTime
import `in`.getdownfoundation.sahusales.ui.dashboard.parseMillis
import `in`.getdownfoundation.sahusales.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// Sort events: upcoming reminder date ASC (soonest first), no-reminder events last by created_at DESC
fun sortEventsByReminder(events: List<Event>): List<Event> {
    val now = System.currentTimeMillis()
    return events.sortedWith(compareBy(
        { event ->
            event.reminders
                .filter { it.status == "pending" || it.status == "snoozed" }
                .mapNotNull { r -> parseMillis(r.remindAt).takeIf { it > 0 } }
                .minOrNull() ?: Long.MAX_VALUE
        },
        { event -> -(parseMillis(event.createdAt ?: "0")) }
    ))
}

@Composable
fun EventsScreen(viewModel: MainViewModel) {
    val events by viewModel.events.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val tags by viewModel.eventTags.collectAsState()
    val team by viewModel.team.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val tabs = listOf("ALL", "UPCOMING", "COMPLETED", "CANCELLED")

    // Reload on tab switch
    LaunchedEffect(selectedTab) {
        val statusFilter = when (tabs[selectedTab]) {
            "UPCOMING" -> "upcoming"
            "COMPLETED" -> "completed"
            "CANCELLED" -> "cancelled"
            else -> null
        }
        viewModel.loadEvents(statusFilter)
    }

    val filtered = remember(events, selectedTab) {
        val list = when (tabs[selectedTab]) {
            "UPCOMING" -> events.filter { it.status == "upcoming" }
            "COMPLETED" -> events.filter { it.status == "completed" }
            "CANCELLED" -> events.filter { it.status == "cancelled" }
            else -> events
        }
        sortEventsByReminder(list)
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
            Text(
                "Events", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = Primary, modifier = Modifier.padding(16.dp)
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Surface,
                contentColor = Primary
            ) {
                tabs.forEachIndexed { i, label ->
                    val count = when (label) {
                        "UPCOMING" -> events.count { it.status == "upcoming" }
                        "COMPLETED" -> events.count { it.status == "completed" }
                        "CANCELLED" -> events.count { it.status == "cancelled" }
                        else -> events.size
                    }
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i }) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(label, fontSize = 11.sp)
                            if (count > 0) {
                                Text(
                                    "$count", fontSize = 10.sp,
                                    color = if (selectedTab == i) Primary else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when (tabs[selectedTab]) {
                                "UPCOMING" -> "📅"
                                "COMPLETED" -> "✅"
                                "CANCELLED" -> "❌"
                                else -> "📋"
                            },
                            fontSize = 40.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No ${tabs[selectedTab].lowercase()} events",
                            color = TextSecondary, fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    items(filtered) { e -> EventCard(e) }
                }
            }
        }
    }

    if (showCreate) {
        CreateEventSheet(
            contacts = contacts,
            tags = tags,
            team = team,
            currentUser = currentUser,
            onDismiss = { showCreate = false },
            onSave = { body ->
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) { viewModel.api()?.createEvent(body) }
                        if (resp?.isSuccessful == true) {
                            // Optimistic update: immediately show the new event in the list
                            resp.body()?.let { viewModel.onEventCreated(it) }
                            viewModel.loadEvents()
                            viewModel.loadReminders()
                            showCreate = false
                            val msg = if (body.reminders.isNotEmpty())
                                "Reminder saved successfully"
                            else
                                "Event saved successfully"
                            snackbarHostState.showSnackbar(msg)
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
        "completed" -> StatusUpcoming
        "cancelled" -> StatusOverdue
        else -> Primary
    }

    val now = System.currentTimeMillis()
    val nextReminder = event.reminders
        .filter { it.status == "pending" || it.status == "snoozed" }
        .mapNotNull { r -> parseMillis(r.remindAt).takeIf { it > 0 } }
        .minOrNull()

    val isOverdue = nextReminder != null && nextReminder < now && event.status == "upcoming"
    val reminderColor = when {
        isOverdue -> StatusOverdue
        nextReminder != null -> StatusUpcoming
        else -> TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            // Left color bar
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(statusColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Column(Modifier.padding(12.dp).weight(1f)) {
                // Tag + status row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    event.tagName?.let {
                        Surface(color = tagColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                            Text(
                                it.uppercase(), fontSize = 10.sp, color = tagColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } ?: Spacer(Modifier.width(1.dp))

                    Surface(
                        color = statusColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            event.status.uppercase(), fontSize = 9.sp, color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Title
                Text(event.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)

                // Contact
                event.contactName?.let {
                    Text(it, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                }
                event.contactOrganisation?.let {
                    Text(it, fontSize = 12.sp, color = TextSecondary)
                }

                // Assignee
                event.assigneeName?.let {
                    Spacer(Modifier.height(2.dp))
                    Text("👤 Assigned to $it", fontSize = 12.sp, color = Primary.copy(alpha = 0.8f))
                }

                // Reminders — show all, highlight overdue
                if (event.reminders.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    event.reminders.forEach { r ->
                        val rMs = parseMillis(r.remindAt)
                        val rIsOverdue = rMs < now && r.status == "pending"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "⏰ ${formatTime(r.remindAt)}",
                                fontSize = 12.sp,
                                color = when {
                                    rIsOverdue -> StatusOverdue
                                    r.status == "snoozed" -> StatusSnoozed
                                    rMs > now -> StatusUpcoming
                                    else -> TextSecondary
                                },
                                fontWeight = if (rIsOverdue) FontWeight.Bold else FontWeight.Normal
                            )
                            if (rIsOverdue) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = StatusOverdue.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        "OVERDUE", fontSize = 9.sp, color = StatusOverdue,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (r.status == "snoozed") {
                                Spacer(Modifier.width(6.dp))
                                Text("💤", fontSize = 12.sp)
                            }
                        }
                    }
                } else if (event.status == "upcoming") {
                    Spacer(Modifier.height(4.dp))
                    Text("No reminder set", fontSize = 12.sp, color = TextSecondary)
                }

                // Notes
                event.notes?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
                }
            }
        }
    }
}

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
    team: List<User> = emptyList(),
    currentUser: User? = null,
    onDismiss: () -> Unit,
    onSave: (CreateEventRequest) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var selectedTag by remember { mutableStateOf<EventTag?>(null) }
    var selectedAssignee by remember { mutableStateOf<User?>(null) }
    var reminderTimes by remember { mutableStateOf(listOf("")) }
    var contactExpanded by remember { mutableStateOf(false) }
    var tagExpanded by remember { mutableStateOf(false) }
    var assigneeExpanded by remember { mutableStateOf(false) }

    var editingReminderIdx by remember { mutableStateOf(-1) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val timePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = false)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title *") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )

                ExposedDropdownMenuBox(expanded = contactExpanded, onExpandedChange = { contactExpanded = it }) {
                    OutlinedTextField(
                        value = selectedContact?.name ?: "Select Contact",
                        onValueChange = {}, readOnly = true, label = { Text("Contact") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contactExpanded) }
                    )
                    ExposedDropdownMenu(expanded = contactExpanded, onDismissRequest = { contactExpanded = false }) {
                        contacts.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) }, onClick = { selectedContact = c; contactExpanded = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = tagExpanded, onExpandedChange = { tagExpanded = it }) {
                    OutlinedTextField(
                        value = selectedTag?.name ?: "Select Tag",
                        onValueChange = {}, readOnly = true, label = { Text("Tag") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) }
                    )
                    ExposedDropdownMenu(expanded = tagExpanded, onDismissRequest = { tagExpanded = false }) {
                        tags.forEach { t ->
                            DropdownMenuItem(text = { Text(t.name) }, onClick = { selectedTag = t; tagExpanded = false })
                        }
                    }
                }

                if (team.isNotEmpty() || currentUser != null) {
                    ExposedDropdownMenuBox(expanded = assigneeExpanded, onExpandedChange = { assigneeExpanded = it }) {
                        OutlinedTextField(
                            value = selectedAssignee?.fullName ?: "Assign To (optional)",
                            onValueChange = {}, readOnly = true, label = { Text("Assign To") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assigneeExpanded) }
                        )
                        ExposedDropdownMenu(expanded = assigneeExpanded, onDismissRequest = { assigneeExpanded = false }) {
                            currentUser?.let { me ->
                                DropdownMenuItem(
                                    text = { Text("${me.fullName} (me)") },
                                    onClick = { selectedAssignee = me; assigneeExpanded = false }
                                )
                            }
                            team.filter { it.id != currentUser?.id }.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member.fullName) },
                                    onClick = { selectedAssignee = member; assigneeExpanded = false }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()
                )

                Text("Reminders", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                reminderTimes.forEachIndexed { i, time ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { editingReminderIdx = i; showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (time.isBlank()) "Reminder ${i + 1} — tap to set"
                                       else formatReminderDisplay(time),
                                maxLines = 1, fontSize = 13.sp
                            )
                        }
                        IconButton(onClick = {
                            reminderTimes = reminderTimes.toMutableList().also { it.removeAt(i) }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary)
                        }
                    }
                }
                TextButton(onClick = { reminderTimes = reminderTimes + "" }) { Text("+ Add Reminder") }
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
                    assignedTo = selectedAssignee?.id,
                    reminders = reminders
                ))
            }) { Text("CREATE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { Button(onClick = { showDatePicker = false; showTimePicker = true }) { Text("Next: Set Time →") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set Time") },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } },
            confirmButton = {
                Button(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    utcCal.timeInMillis = selectedMillis
                    val localCal = Calendar.getInstance()
                    localCal.set(
                        utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH),
                        utcCal.get(Calendar.DAY_OF_MONTH),
                        timePickerState.hour, timePickerState.minute, 0
                    )
                    localCal.set(Calendar.MILLISECOND, 0)
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val iso = sdf.format(localCal.time)
                    if (editingReminderIdx >= 0 && editingReminderIdx < reminderTimes.size) {
                        reminderTimes = reminderTimes.toMutableList().also { it[editingReminderIdx] = iso }
                    }
                    showTimePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false; showDatePicker = true }) { Text("← Back") } }
        )
    }
}
