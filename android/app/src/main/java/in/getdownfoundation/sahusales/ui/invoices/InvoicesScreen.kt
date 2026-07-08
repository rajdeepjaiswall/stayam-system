package `in`.getdownfoundation.sahusales.ui.invoices

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
import `in`.getdownfoundation.sahusales.core.Invoice
import `in`.getdownfoundation.sahusales.core.CreateInvoiceRequest
import `in`.getdownfoundation.sahusales.core.CreateInvoiceItemRequest
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InvoicesScreen(viewModel: MainViewModel) {
    val invoices by viewModel.invoices.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val filtered = remember(invoices, selectedStatus) {
        if (selectedStatus == null) invoices else invoices.filter { it.status == selectedStatus }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }, containerColor = Primary) {
                Icon(Icons.Default.Add, contentDescription = "Create Invoice", tint = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Invoices", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary, modifier = Modifier.padding(16.dp))

            ScrollableTabRow(
                selectedTabIndex = listOf(null, "draft", "sent", "paid", "cancelled").indexOf(selectedStatus).coerceAtLeast(0),
                containerColor = Surface, contentColor = Primary, edgePadding = 8.dp
            ) {
                listOf("All" to null, "Draft" to "draft", "Sent" to "sent", "Paid" to "paid", "Cancelled" to "cancelled")
                    .forEach { (label, status) ->
                        Tab(selected = selectedStatus == status, onClick = { selectedStatus = status }) {
                            Text(label, modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp))
                        }
                    }
            }

            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(12.dp)) {
                items(filtered) { inv -> InvoiceCard(inv) }
            }
        }
    }

    if (showCreate) {
        CreateInvoiceDialog(
            contacts = contacts,
            onDismiss = { showCreate = false },
            onSave = { body: CreateInvoiceRequest ->
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) { viewModel.api()?.createInvoice(body) }
                        if (resp?.isSuccessful == true) {
                            viewModel.loadInvoices()
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
fun InvoiceCard(inv: Invoice) {
    val statusColor = when (inv.status) {
        "paid" -> StatusUpcoming
        "sent" -> Primary
        "cancelled" -> StatusOverdue
        else -> TextSecondary
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                    Text(inv.status.uppercase(), fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            inv.contactName?.let { Text(it, color = TextSecondary, fontSize = 14.sp) }
            Spacer(Modifier.height(4.dp))
            Text("₹${String.format("%.2f", inv.total)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Primary)
            Text("FY ${inv.financialYear}", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
fun CreateInvoiceDialog(
    contacts: List<`in`.getdownfoundation.sahusales.core.Contact>,
    onDismiss: () -> Unit,
    onSave: (CreateInvoiceRequest) -> Unit
) {
    var selectedContact by remember { mutableStateOf<`in`.getdownfoundation.sahusales.core.Contact?>(null) }
    var contactExpanded by remember { mutableStateOf(false) }

    data class LineItem(val name: String, val qty: String, val rate: String, val gst: String)
    var items by remember { mutableStateOf(listOf(LineItem("", "1", "0", "0"))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Invoice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            DropdownMenuItem(text = { Text(c.name) }, onClick = { selectedContact = c; contactExpanded = false })
                        }
                    }
                }

                Text("Line Items", fontWeight = FontWeight.Bold)
                items.forEachIndexed { i, item ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(value = item.name, onValueChange = { v -> items = items.toMutableList().also { it[i] = item.copy(name = v) } },
                            label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = item.qty, onValueChange = { v -> items = items.toMutableList().also { it[i] = item.copy(qty = v) } },
                                label = { Text("Qty") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = item.rate, onValueChange = { v -> items = items.toMutableList().also { it[i] = item.copy(rate = v) } },
                                label = { Text("Rate") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = item.gst, onValueChange = { v -> items = items.toMutableList().also { it[i] = item.copy(gst = v) } },
                                label = { Text("GST%") }, modifier = Modifier.weight(1f))
                        }
                        if (items.size > 1) {
                            TextButton(onClick = { items = items.toMutableList().also { it.removeAt(i) } }) { Text("Remove") }
                        }
                    }
                }
                TextButton(onClick = { items = items + LineItem("", "1", "0", "0") }) { Text("+ Add Line") }

                // Live total preview
                val subtotal = items.sumOf { (it.qty.toDoubleOrNull() ?: 0.0) * (it.rate.toDoubleOrNull() ?: 0.0) }
                val tax = items.sumOf {
                    val line = (it.qty.toDoubleOrNull() ?: 0.0) * (it.rate.toDoubleOrNull() ?: 0.0)
                    line * ((it.gst.toDoubleOrNull() ?: 0.0) / 100.0)
                }
                Text("Subtotal: ₹${String.format("%.2f", subtotal)} | GST: ₹${String.format("%.2f", tax)} | Total: ₹${String.format("%.2f", subtotal + tax)}",
                    fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Medium)
            }
        },
        confirmButton = {
            Button(onClick = {
                val invoiceItems = items.filter { it.name.isNotBlank() }.map {
                    CreateInvoiceItemRequest(
                        name = it.name,
                        qty = it.qty.toDoubleOrNull() ?: 1.0,
                        rate = it.rate.toDoubleOrNull() ?: 0.0,
                        gstPercent = it.gst.toDoubleOrNull() ?: 0.0
                    )
                }
                if (invoiceItems.isEmpty()) return@Button
                onSave(CreateInvoiceRequest(contactId = selectedContact?.id, items = invoiceItems))
            }) { Text("CREATE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
