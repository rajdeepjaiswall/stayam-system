package `in`.getdownfoundation.sahusales.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.getdownfoundation.sahusales.core.Contact
import `in`.getdownfoundation.sahusales.core.CreateContactRequest
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ContactsScreen(viewModel: MainViewModel, onContactClick: (String) -> Unit) {
    val contacts by viewModel.contacts.collectAsState()
    var search by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val filtered = remember(contacts, search) {
        if (search.isBlank()) contacts
        else contacts.filter { c ->
            c.name.contains(search, true) || c.mobile?.contains(search, true) == true ||
            c.email?.contains(search, true) == true || c.organisation?.contains(search, true) == true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = Primary) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact", tint = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Contacts", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary, modifier = Modifier.padding(16.dp))
            OutlinedTextField(
                value = search, onValueChange = {
                    search = it
                    viewModel.loadContacts(it)
                },
                label = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(filtered) { c -> ContactRow(c) { onContactClick(c.id) } }
            }
        }
    }

    if (showAdd) {
        AddContactDialog(
            onDismiss = { showAdd = false },
            onSave = { data ->
                scope.launch {
                    try {
                        val req = CreateContactRequest(
                            name = data["name"] ?: "",
                            mobile = data["mobile"],
                            whatsapp = data["whatsapp"],
                            email = data["email"],
                            organisation = data["organisation"],
                            address = data["address"],
                            notes = data["notes"]
                        )
                        val resp = withContext(Dispatchers.IO) { viewModel.api()?.createContact(req) }
                        if (resp?.isSuccessful == true) {
                            viewModel.loadContacts()
                            showAdd = false
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
fun ContactRow(contact: Contact, onClick: () -> Unit) {
    val initial = contact.name.firstOrNull()?.uppercase() ?: "?"
    val colors = listOf(0xFF1565C0, 0xFF00897B, 0xFF6D4C41, 0xFF4527A0, 0xFF00695C)
    val colorIndex = contact.name.hashCode().let { if (it < 0) -it else it } % colors.size
    val avatarColor = Color(colors[colorIndex])

    Card(
        Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                contact.organisation?.let { Text(it, fontSize = 13.sp, color = TextSecondary) }
                contact.mobile?.let { Text(it, fontSize = 13.sp, color = TextSecondary) }
            }
        }
    }
}

@Composable
fun AddContactDialog(onDismiss: () -> Unit, onSave: (Map<String, String?>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var org by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Name *" to name to { v: String -> name = v },
                    "Mobile" to mobile to { v: String -> mobile = v },
                    "WhatsApp" to whatsapp to { v: String -> whatsapp = v },
                    "Email" to email to { v: String -> email = v },
                    "Organisation" to org to { v: String -> org = v },
                    "Address" to address to { v: String -> address = v },
                    "Notes" to notes to { v: String -> notes = v }
                ).forEach { (labelValue, onChange) ->
                    val (label, value) = labelValue
                    OutlinedTextField(value = value, onValueChange = onChange,
                        label = { Text(label) }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) return@Button
                onSave(mapOf(
                    "name" to name.trim(),
                    "mobile" to mobile.ifBlank { null },
                    "whatsapp" to whatsapp.ifBlank { null },
                    "email" to email.ifBlank { null },
                    "organisation" to org.ifBlank { null },
                    "address" to address.ifBlank { null },
                    "notes" to notes.ifBlank { null }
                ))
            }) { Text("SAVE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
