package `in`.getdownfoundation.sahusales.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.getdownfoundation.sahusales.core.Event
import `in`.getdownfoundation.sahusales.core.User
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.dashboard.formatTime
import `in`.getdownfoundation.sahusales.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(viewModel: MainViewModel) {
    val team by viewModel.team.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<User?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (currentUser?.role == "admin") {
                FloatingActionButton(onClick = { showAdd = true }, containerColor = Primary) {
                    Icon(Icons.Default.Add, contentDescription = "Add Member", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Team", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary, modifier = Modifier.padding(16.dp))
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(team) { member ->
                    TeamMemberCard(
                        user = member,
                        isAdmin = currentUser?.role == "admin",
                        viewModel = viewModel,
                        scope = scope,
                        snackbarHostState = snackbarHostState,
                        onClick = { selectedMember = member }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddMemberDialog(
            onDismiss = { showAdd = false },
            onSave = { data ->
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) { viewModel.api()?.createTeamMember(data) }
                        if (resp?.isSuccessful == true) { viewModel.loadTeam(); showAdd = false }
                        else snackbarHostState.showSnackbar(resp?.errorBody()?.string() ?: "Failed")
                    } catch (e: Exception) { snackbarHostState.showSnackbar(e.message ?: "Error") }
                }
            }
        )
    }

    selectedMember?.let { member ->
        TeamMemberDetailSheet(
            member = member,
            viewModel = viewModel,
            onDismiss = { selectedMember = null }
        )
    }
}

@Composable
fun TeamMemberCard(
    user: User,
    isAdmin: Boolean,
    viewModel: MainViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onClick: () -> Unit
) {
    var disabled by remember(user) { mutableStateOf(user.isDisabled) }
    var manageContacts by remember(user) { mutableStateOf(user.permissions["manage_contacts"] ?: true) }
    var manageEvents by remember(user) { mutableStateOf(user.permissions["manage_events"] ?: true) }
    var manageInvoices by remember(user) { mutableStateOf(user.permissions["manage_invoices"] ?: true) }

    fun save() {
        scope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    viewModel.api()?.updateTeamMember(user.id, mapOf(
                        "permissions" to mapOf("manage_contacts" to manageContacts, "manage_events" to manageEvents,
                            "manage_invoices" to manageInvoices, "view_team" to true),
                        "is_disabled" to disabled
                    ))
                }
                if (resp?.isSuccessful == true) viewModel.loadTeam()
                else snackbarHostState.showSnackbar(resp?.errorBody()?.string() ?: "Failed")
            } catch (e: Exception) { snackbarHostState.showSnackbar(e.message ?: "Error") }
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(user.email, fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Surface(color = if (user.role == "admin") Primary.copy(alpha = 0.15f) else Surface, shape = RoundedCornerShape(50)) {
                        Text(user.role.uppercase(), fontSize = 11.sp, color = if (user.role == "admin") Primary else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                if (isAdmin && user.role != "admin") {
                    Switch(checked = !disabled, onCheckedChange = { disabled = !it; save() })
                }
            }
            if (isAdmin && user.role != "admin") {
                Spacer(Modifier.height(8.dp))
                Text("Permissions", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Contacts" to manageContacts to { v: Boolean -> manageContacts = v; save() },
                        "Events" to manageEvents to { v: Boolean -> manageEvents = v; save() },
                        "Invoices" to manageInvoices to { v: Boolean -> manageInvoices = v; save() }
                    ).forEach { (labelValue, onChange) ->
                        val (label, value) = labelValue
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = 11.sp, color = TextSecondary)
                            Switch(checked = value, onCheckedChange = onChange, modifier = Modifier.size(40.dp))
                        }
                    }
                }
                Text("Tap card to view their events & reminders", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamMemberDetailSheet(member: User, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var memberEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(member.id) {
        loading = true
        try {
            val resp = withContext(Dispatchers.IO) {
                viewModel.api()?.getEvents(assignedTo = member.id)
            }
            if (resp?.isSuccessful == true) memberEvents = resp.body() ?: emptyList()
        } catch (_: Exception) { }
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Member header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(member.fullName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(member.email, fontSize = 13.sp, color = TextSecondary)
                }
                Surface(color = Primary.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                    Text(member.role.uppercase(), fontSize = 11.sp, color = Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stats row
            val upcoming = memberEvents.filter { it.status == "upcoming" }
            val completed = memberEvents.filter { it.status == "completed" }
            val todayCount = upcoming.count { event ->
                event.reminders.any { r ->
                    try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        val t = sdf.parse(r.remindAt)?.time ?: 0L
                        val now = System.currentTimeMillis()
                        t >= now && t <= now + 24 * 60 * 60 * 1000L
                    } catch (_: Exception) { false }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("${upcoming.size}", "Upcoming", Primary)
                StatChip("${todayCount}", "Due Today", StatusOverdue)
                StatChip("${completed.size}", "Completed", StatusUpcoming)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text("Assigned Events", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))

            if (loading) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(28.dp))
                }
            } else if (memberEvents.isEmpty()) {
                Text("No events assigned to ${member.fullName} yet.",
                    fontSize = 14.sp, color = TextSecondary)
            } else {
                LazyColumn(
                    Modifier.heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memberEvents) { event ->
                        MemberEventRow(event)
                    }
                }
            }
        }
    }
}

@Composable
fun StatChip(value: String, label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.wrapContentWidth()
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
            Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun MemberEventRow(event: Event) {
    val tagColor = try {
        Color(android.graphics.Color.parseColor(event.tagColor ?: "#1565C0"))
    } catch (_: Exception) { Primary }
    val statusColor = when (event.status) {
        "completed" -> StatusArchived
        "cancelled" -> StatusOverdue
        else -> StatusUpcoming
    }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(4.dp).fillMaxHeight()
                .background(statusColor, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)))
            Column(Modifier.padding(10.dp).weight(1f)) {
                event.tagName?.let {
                    Surface(color = tagColor.copy(alpha = 0.13f), shape = RoundedCornerShape(50)) {
                        Text(it.uppercase(), fontSize = 10.sp, color = tagColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(Modifier.height(2.dp))
                }
                Text(event.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                event.contactName?.let { Text(it, fontSize = 12.sp, color = TextSecondary) }
                if (event.reminders.isNotEmpty()) {
                    Text("⏰ ${formatTime(event.reminders.first().remindAt)}", fontSize = 11.sp, color = TextSecondary)
                }
            }
            Surface(
                color = statusColor.copy(alpha = 0.13f),
                shape = RoundedCornerShape(topEnd = 10.dp),
                modifier = Modifier.padding(4.dp)
            ) {
                Text(event.status.uppercase(), fontSize = 9.sp, color = statusColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
            }
        }
    }
}

@Composable
fun AddMemberDialog(onDismiss: () -> Unit, onSave: (Map<String, Any?>) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Team Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password *") }, modifier = Modifier.fillMaxWidth())
                Text("This creates a login for this team member. They use the same app login screen.",
                    fontSize = 12.sp, color = TextSecondary)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (email.isBlank() || password.isBlank() || fullName.isBlank()) return@Button
                onSave(mapOf("email" to email.trim(), "password" to password, "full_name" to fullName.trim()))
            }) { Text("CREATE LOGIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
