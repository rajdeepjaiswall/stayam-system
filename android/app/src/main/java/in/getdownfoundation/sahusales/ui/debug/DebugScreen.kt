package `in`.getdownfoundation.sahusales.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.getdownfoundation.sahusales.alarm.AlarmScheduler
import `in`.getdownfoundation.sahusales.core.SessionStore
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.theme.Primary
import `in`.getdownfoundation.sahusales.ui.theme.StatusUpcoming
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun DebugScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SessionStore(context) }
    val token by viewModel.token.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    var lastSync by remember { mutableStateOf("Unknown") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        val ls = store.getLastSync()
        lastSync = if (ls != null) Date(ls).toString() else "Never"
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🛠 Debug Screen", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary)
            Text("(Hidden: tap logo 7× to access)", fontSize = 12.sp, color = Color(0xFF94A3B8))

            Divider()

            Text("Token: ${if (token != null) "✅ Present (${token!!.take(20)}...)" else "❌ None"}", fontSize = 13.sp)
            Text("Last Sync: $lastSync", fontSize = 13.sp)
            Text("Scheduled Reminders: ${reminders.size}", fontSize = 13.sp)

            Divider()

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TEST ALARM", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = StatusUpcoming)
                    Text("Schedules a fake alarm through the same AlarmReceiver path, firing in 2 minutes.",
                        fontSize = 13.sp, color = Color(0xFF475569))
                    Button(
                        onClick = {
                            val fireAt = System.currentTimeMillis() + 2 * 60 * 1000L
                            AlarmScheduler.schedule(
                                context,
                                "test-alarm-${System.currentTimeMillis()}",
                                fireAt,
                                "Test Reminder — Debug Mode"
                            )
                            scope.launch {
                                snackbarHostState.showSnackbar("Test alarm scheduled for ${Date(fireAt)}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusUpcoming),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("TEST ALARM IN 2 MIN", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Divider()

            Text("Active Reminders:", fontWeight = FontWeight.Bold)
            if (reminders.isEmpty()) {
                Text("None", color = Color(0xFF94A3B8))
            } else {
                reminders.forEach { r ->
                    Text("• ${r.eventTitle} — ${r.effectiveTime} [${r.status}]", fontSize = 12.sp)
                }
            }

            Divider()

            Button(
                onClick = {
                    scope.launch {
                        viewModel.loadReminders()
                        val ls = store.getLastSync()
                        lastSync = if (ls != null) Date(ls).toString() else "Never"
                        snackbarHostState.showSnackbar("Synced ${reminders.size} reminders")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("FORCE SYNC NOW") }
        }
    }
}
