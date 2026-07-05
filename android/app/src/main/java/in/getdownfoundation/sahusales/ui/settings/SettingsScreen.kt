package `in`.getdownfoundation.sahusales.ui.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(viewModel: MainViewModel, onLogout: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var fullName by remember(currentUser) { mutableStateOf(currentUser?.fullName ?: "") }
    var mobile by remember(currentUser) { mutableStateOf(currentUser?.mobile ?: "") }
    var orgName by remember(currentUser) { mutableStateOf(currentUser?.organisationName ?: "") }
    var vibrate by remember(currentUser) { mutableStateOf(currentUser?.vibrate ?: true) }

    // Permission checks
    val am = context.getSystemService(AlarmManager::class.java)
    val pm = context.getSystemService(PowerManager::class.java)
    var canExactAlarms by remember { mutableStateOf(am.canScheduleExactAlarms()) }
    var batteryOptIgnored by remember { mutableStateOf(pm.isIgnoringBatteryOptimizations(context.packageName)) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary)

            // Profile section
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = orgName, onValueChange = { orgName = it }, label = { Text("Organisation") }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Vibrate on alarm")
                        Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                    }
                    Button(onClick = {
                        scope.launch {
                            try {
                                val resp = withContext(Dispatchers.IO) {
                                    viewModel.api()?.updateMe(mapOf("full_name" to fullName, "mobile" to mobile, "organisation_name" to orgName))
                                }
                                if (resp?.isSuccessful == true) snackbarHostState.showSnackbar("Profile updated")
                                else snackbarHostState.showSnackbar("Failed to update")
                            } catch (e: Exception) { snackbarHostState.showSnackbar(e.message ?: "Error") }
                        }
                    }) { Text("SAVE PROFILE") }
                }
            }

            // Permissions checklist
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Alarm Permissions", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    PermissionRow(
                        label = "Exact Alarms",
                        granted = canExactAlarms,
                        onFix = {
                            canExactAlarms = am.canScheduleExactAlarms()
                            if (!canExactAlarms) {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}")))
                            }
                        }
                    )

                    PermissionRow(
                        label = "Battery Optimization (must be OFF)",
                        granted = batteryOptIgnored,
                        onFix = {
                            batteryOptIgnored = pm.isIgnoringBatteryOptimizations(context.packageName)
                            if (!batteryOptIgnored) {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")))
                            }
                        }
                    )

                    Text("OEM Autostart (for Xiaomi/Realme/Oppo/Vivo):", fontSize = 13.sp, color = TextSecondary)
                    OutlinedButton(onClick = {
                        val intents = listOf(
                            Intent().apply { component = android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity") },
                            Intent().apply { component = android.content.ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.FnStartupAppListActivity") },
                            Intent().apply { component = android.content.ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity") },
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        )
                        for (i in intents) {
                            try { context.startActivity(i); break } catch (_: Exception) {}
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Open Autostart Settings") }
                }
            }

            // Logout
            OutlinedButton(
                onClick = { viewModel.logout { onLogout() } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusOverdue)
            ) { Text("LOGOUT", color = StatusOverdue, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun PermissionRow(label: String, granted: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp)
            Text(if (granted) "✅ Granted" else "❌ Not granted", fontSize = 12.sp,
                color = if (granted) StatusUpcoming else StatusOverdue)
        }
        if (!granted) {
            OutlinedButton(onClick = onFix) { Text("FIX", fontSize = 12.sp) }
        }
    }
}
