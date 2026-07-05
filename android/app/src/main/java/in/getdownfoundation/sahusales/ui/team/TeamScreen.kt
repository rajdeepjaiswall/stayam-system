package `in`.getdownfoundation.sahusales.ui.team

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
import `in`.getdownfoundation.sahusales.core.User
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TeamScreen(viewModel: MainViewModel) {
    val team by viewModel.team.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
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
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(12.dp)) {
                items(team) { member -> TeamMemberCard(member, isAdmin = currentUser?.role == "admin", viewModel, scope, snackbarHostState) }
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
}

@Composable
fun TeamMemberCard(
    user: User,
    isAdmin: Boolean,
    viewModel: MainViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState
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

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(user.email, fontSize = 13.sp, color = TextSecondary)
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
                Text("Permissions", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                listOf(
                    "Contacts" to manageContacts to { v: Boolean -> manageContacts = v; save() },
                    "Events" to manageEvents to { v: Boolean -> manageEvents = v; save() },
                    "Invoices" to manageInvoices to { v: Boolean -> manageInvoices = v; save() }
                ).forEach { (labelValue, onChange) ->
                    val (label, value) = labelValue
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, fontSize = 14.sp)
                        Switch(checked = value, onCheckedChange = onChange)
                    }
                }
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
            }
        },
        confirmButton = {
            Button(onClick = {
                if (email.isBlank() || password.isBlank() || fullName.isBlank()) return@Button
                onSave(mapOf("email" to email.trim(), "password" to password, "full_name" to fullName.trim()))
            }) { Text("ADD") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
