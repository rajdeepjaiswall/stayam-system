package `in`.getdownfoundation.sahusales.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.getdownfoundation.sahusales.core.Contact
import `in`.getdownfoundation.sahusales.core.Event
import `in`.getdownfoundation.sahusales.core.EventTag
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.dashboard.formatTime
import `in`.getdownfoundation.sahusales.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            Text("Events", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary, modifier = Modifier.padding(16.dp))

            // Status filter tabs
            ScrollableTabRow(
                selectedTabIndex = listOf(null, "upcoming", "completed", "cancelled").indexOf(selectedStatus).coerceAtLeast(0),
                containerColor = Surface,
                contentColor = Primary,
                edgePadding = 8.dp
            ) {
                listOf("All" to null, "Upcoming" to "upcoming", "Completed" to "completed", "Cancelled" to "cancelled")
                    .forEachIndexed { i, (label, status) ->
                        Tab(
                            selected = selectedStatus == status,
                            onClick = {
                                selectedStatus = status
                                viewModel.loadEvents(status)
                            }
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
            Box(Modifier.width(5.dp).fillMaxHeight().background(statusColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)))
            Column(Modifier.padding(12.dp).weight(1f)) {
                Row {
                    event.tagName?.let {
                        Surface(color = tagColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                            Text(it.uppercase(), fontSize = 10.sp, color = tagColor, fontWeight = FontWeight.Bold,
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

@Composable
fun CreateEventSheet(
    contacts: List<Contact>,
    tags: List<EventTag>,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any?>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var selectedTag by remember { mutableStateOf<EventTag?>(null) }
    var reminderTimes by remember { mutableStateOf(listOf("")) }
    var contactExpanded by remember { mutableStateOf(false) }
    var tagExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title *") }, modifier = Modifier.fillMaxWidth())

                // Contact picker
                ExposedDropdownMenuBox(expanded = contactExpanded, onExpandedChange = { contactExpanded = it }) {
                    OutlinedTextField(
                        value = selectedContact?.name ?: "Select Contact",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Contact") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contactExpanded) }
                    )
                    ExposedDropdownMenu(expanded = contactExpanded, onDismissRequest = { contactExpanded = false }) {
                        contacts.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) }, onClick = { selectedContact = c; contactExpanded = false })
                        }
                    }
                }

                // Tag picker
                ExposedDropdownMenuBox(expanded = tagExpanded, onExpandedChange = { tagExpanded = it }) {
                    OutlinedTextField(
                        value = selectedTag?.name ?: "Select Tag",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tag") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) }
                    )
                    ExposedDropdownMenu(expanded = tagExpanded, onDismissRequest = { tagExpanded = false }) {
                        tags.forEach { t ->
                            DropdownMenuItem(text = { Text(t.name) }, onClick = { selectedTag = t; tagExpanded = false })
                        }
                    }
                }

                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())

                Text("Reminders (format: 2024-12-31T09:00:00Z)", fontSize = 12.sp, color = TextSecondary)
                reminderTimes.forEachIndexed { i, time ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = time,
                            onValueChange = { v -> reminderTimes = reminderTimes.toMutableList().also { it[i] = v } },
                            label = { Text("Reminder ${i + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { reminderTimes = reminderTimes.toMutableList().also { it.removeAt(i) } }) {
                            Text("✕")
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
                val reminders = reminderTimes.filter { it.isNotBlank() }.map { mapOf("remind_at" to it) }
                onSave(mapOf(
                    "title" to title.trim(),
                    "notes" to notes.ifBlank { null },
                    "contact_id" to selectedContact?.id,
                    "tag_id" to selectedTag?.id,
                    "reminders" to reminders
                ))
            }) { Text("CREATE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
