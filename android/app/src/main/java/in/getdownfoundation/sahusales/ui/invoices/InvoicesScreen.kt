package `in`.getdownfoundation.sahusales.ui.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.getdownfoundation.sahusales.core.Contact
import `in`.getdownfoundation.sahusales.core.Invoice
import `in`.getdownfoundation.sahusales.core.CreateInvoiceRequest
import `in`.getdownfoundation.sahusales.core.CreateInvoiceItemRequest
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(viewModel: MainViewModel) {
    val invoices by viewModel.invoices.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var detailInvoice by remember { mutableStateOf<Invoice?>(null) }
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
            Text(
                "Invoices", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = Primary, modifier = Modifier.padding(16.dp)
            )

            ScrollableTabRow(
                selectedTabIndex = listOf(null, "draft", "sent", "paid", "cancelled")
                    .indexOf(selectedStatus).coerceAtLeast(0),
                containerColor = Surface, contentColor = Primary, edgePadding = 8.dp
            ) {
                listOf("All" to null, "Draft" to "draft", "Sent" to "sent", "Paid" to "paid", "Cancelled" to "cancelled")
                    .forEach { (label, status) ->
                        Tab(selected = selectedStatus == status, onClick = { selectedStatus = status }) {
                            Text(label, modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp))
                        }
                    }
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(filtered) { inv ->
                    InvoiceCard(inv, onClick = { detailInvoice = inv })
                }
            }
        }
    }

    // Invoice Detail Sheet
    detailInvoice?.let { inv ->
        InvoiceDetailSheet(
            invoice = inv,
            currentUser = currentUser,
            onDismiss = { detailInvoice = null },
            onStatusChange = { newStatus ->
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) {
                            viewModel.api()?.updateInvoice(inv.id, mapOf("status" to newStatus))
                        }
                        if (resp?.isSuccessful == true) {
                            viewModel.loadInvoices()
                            detailInvoice = null
                        } else {
                            snackbarHostState.showSnackbar("Failed to update status")
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(e.message ?: "Error")
                    }
                }
            }
        )
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
fun InvoiceCard(inv: Invoice, onClick: () -> Unit = {}) {
    val statusColor = when (inv.status) {
        "paid" -> StatusUpcoming
        "sent" -> Primary
        "cancelled" -> StatusOverdue
        else -> TextSecondary
    }
    Card(
        Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                    Text(
                        inv.status.uppercase(), fontSize = 11.sp, color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            inv.contactName?.let { Text(it, color = TextSecondary, fontSize = 14.sp) }
            inv.contactOrganisation?.let { Text(it, color = TextSecondary, fontSize = 12.sp) }
            Spacer(Modifier.height(4.dp))
            Text("₹${String.format("%.2f", inv.total)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Primary)
            Text("FY ${inv.financialYear}", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailSheet(
    invoice: Invoice,
    currentUser: `in`.getdownfoundation.sahusales.core.User?,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        sheetState = rememberModalBottomSheetState(skipPartialExpansion = true)
    ) {
        LazyColumn(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                // ── Header ──────────────────────────────────────────
                Box(
                    Modifier.fillMaxWidth()
                        .background(Primary, RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        val orgName = currentUser?.organisationName ?: "Sahu Sales"
                        Text(orgName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("INVOICE", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text(invoice.invoiceNumber, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("DATE", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text(
                                    formatInvoiceDate(invoice.createdAt),
                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("FY ${invoice.financialYear}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Bill To ─────────────────────────────────────────
                if (invoice.contactName != null) {
                    Surface(color = Surface, shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("BILL TO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            Text(invoice.contactName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            invoice.contactOrganisation?.let {
                                Text(it, color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── Status Badge ─────────────────────────────────────
                val statusColor = when (invoice.status) {
                    "paid" -> StatusUpcoming
                    "sent" -> Primary
                    "cancelled" -> StatusOverdue
                    else -> TextSecondary
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ", fontSize = 13.sp, color = TextSecondary)
                    Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                        Text(
                            invoice.status.uppercase(), fontSize = 12.sp, color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Line Items Header ────────────────────────────────
                Divider(color = Border)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ITEM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(2.5f))
                    Text("QTY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                    Text("GST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                    Text("TOTAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                }
                Divider(color = Border)
            }

            // ── Line Items ───────────────────────────────────────
            items(invoice.items) { item ->
                val lineAmt = item.lineTotal ?: (item.qty * item.rate)
                val lineTax = item.lineTax ?: (lineAmt * item.gstPercent / 100.0)
                val lineGross = lineAmt + lineTax
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(item.name, fontSize = 13.sp, modifier = Modifier.weight(2.5f))
                    Text(
                        formatQty(item.qty), fontSize = 13.sp,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                    )
                    Text(
                        "₹${formatAmt(item.rate)}", fontSize = 13.sp,
                        modifier = Modifier.weight(1.2f), textAlign = TextAlign.End
                    )
                    Text(
                        "${item.gstPercent.toInt()}%", fontSize = 13.sp, color = TextSecondary,
                        modifier = Modifier.weight(0.8f), textAlign = TextAlign.End
                    )
                    Text(
                        "₹${formatAmt(lineGross)}", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1.2f), textAlign = TextAlign.End
                    )
                }
                Divider(color = Border.copy(alpha = 0.5f))
            }

            item {
                Spacer(Modifier.height(8.dp))

                // ── Totals ───────────────────────────────────────────
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    TotalRow("Subtotal", "₹${formatAmt(invoice.subtotal)}")
                    TotalRow("GST", "₹${formatAmt(invoice.tax)}")
                    Divider(Modifier.width(200.dp), color = Border)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.width(200.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("GRAND TOTAL", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("₹${formatAmt(invoice.total)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Primary)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Action Buttons ───────────────────────────────────
                if (invoice.status == "draft") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onStatusChange("cancelled") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusOverdue)
                        ) { Text("CANCEL") }
                        Button(
                            onClick = { onStatusChange("sent") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) { Text("MARK SENT") }
                    }
                } else if (invoice.status == "sent") {
                    Button(
                        onClick = { onStatusChange("paid") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusUpcoming)
                    ) { Text("MARK PAID ✓", color = Color.White) }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TotalRow(label: String, value: String) {
    Row(
        Modifier.width(200.dp).padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp)
    }
}

fun formatQty(d: Double) = if (d == d.toLong().toDouble()) d.toLong().toString() else "%.2f".format(d)
fun formatAmt(d: Double) = "%,.2f".format(d)

fun formatInvoiceDate(iso: String?): String {
    if (iso == null) return ""
    return try {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        var date: Date? = null
        for (fmt in formats) {
            try {
                date = SimpleDateFormat(fmt, Locale.US).also {
                    it.timeZone = if (fmt.endsWith("'Z'")) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
                }.parse(iso)
                if (date != null) break
            } catch (_: Exception) {}
        }
        date?.let { SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(it) } ?: iso
    } catch (e: Exception) { iso }
}

@Composable
fun CreateInvoiceDialog(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onSave: (CreateInvoiceRequest) -> Unit
) {
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
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
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { v -> items = items.toMutableList().also { it[i] = item.copy(name = v) } },
                            label = { Text("Description") }, modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = item.qty,
                                onValueChange = { v -> items = items.toMutableList().also { it[i] = item.copy(qty = v) } },
                                label = { Text("Qty") }, modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = item.rate,
                                onValueChange = { v -> items = items.toMutableList().also { it[i] = item.copy(rate = v) } },
                                label = { Text("Rate") }, modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = item.gst,
                                onValueChange = { v -> items = items.toMutableList().also { it[i] = item.copy(gst = v) } },
                                label = { Text("GST%") }, modifier = Modifier.weight(1f)
                            )
                        }
                        if (items.size > 1) {
                            TextButton(onClick = { items = items.toMutableList().also { it.removeAt(i) } }) { Text("Remove") }
                        }
                    }
                }
                TextButton(onClick = { items = items + LineItem("", "1", "0", "0") }) { Text("+ Add Line") }

                val subtotal = items.sumOf { (it.qty.toDoubleOrNull() ?: 0.0) * (it.rate.toDoubleOrNull() ?: 0.0) }
                val tax = items.sumOf {
                    val line = (it.qty.toDoubleOrNull() ?: 0.0) * (it.rate.toDoubleOrNull() ?: 0.0)
                    line * ((it.gst.toDoubleOrNull() ?: 0.0) / 100.0)
                }
                Text(
                    "Sub: ₹${formatAmt(subtotal)}  GST: ₹${formatAmt(tax)}  Total: ₹${formatAmt(subtotal + tax)}",
                    fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Medium
                )
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
