package `in`.getdownfoundation.sahusales.ui.settings

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.getdownfoundation.sahusales.alarm.AlarmScheduler
import `in`.getdownfoundation.sahusales.overlay.OverlayBubbleService
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(viewModel: MainViewModel, onLogout: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsState()
    val eventTags by viewModel.eventTags.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var fullName by remember(currentUser) { mutableStateOf(currentUser?.fullName ?: "") }
    var mobile by remember(currentUser) { mutableStateOf(currentUser?.mobile ?: "") }
    var orgName by remember(currentUser) { mutableStateOf(currentUser?.organisationName ?: "") }
    var vibrate by remember(currentUser) { mutableStateOf(currentUser?.vibrate ?: true) }

    val am = context.getSystemService(AlarmManager::class.java)
    val pm = context.getSystemService(PowerManager::class.java)
    val nm = context.getSystemService(NotificationManager::class.java)

    fun checkPermissions() = mapOf(
        "exactAlarms" to am.canScheduleExactAlarms(),
        "battery" to pm.isIgnoringBatteryOptimizations(context.packageName),
        "overlay" to Settings.canDrawOverlays(context),
        "notifications" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        else true,
        "fullScreen" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            nm.canUseFullScreenIntent()
        else true
    )

    var perms by remember { mutableStateOf(checkPermissions()) }
    var bubbleActive by remember { mutableStateOf(false) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary)

            // ── Profile ───────────────────────────────────────────────────────
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it },
                        label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it },
                        label = { Text("Mobile") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = orgName, onValueChange = { orgName = it },
                        label = { Text("Organisation") }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Vibrate on alarm")
                        Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                    }
                    Button(onClick = {
                        scope.launch {
                            try {
                                val resp = withContext(Dispatchers.IO) {
                                    viewModel.api()?.updateMe(mapOf(
                                        "full_name" to fullName,
                                        "mobile" to mobile,
                                        "organisation_name" to orgName
                                    ))
                                }
                                if (resp?.isSuccessful == true) snackbarHostState.showSnackbar("Profile updated")
                                else snackbarHostState.showSnackbar("Failed to update")
                            } catch (e: Exception) { snackbarHostState.showSnackbar(e.message ?: "Error") }
                        }
                    }) { Text("SAVE PROFILE") }
                }
            }

            // ── Permissions ───────────────────────────────────────────────────
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Alarm & Notification Permissions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Enable ALL of these so reminders fire as full-screen alerts even when the phone is locked or another app is open.",
                        fontSize = 12.sp, color = TextSecondary
                    )

                    // 1. Exact alarms
                    PermissionRow(
                        label = "Exact Alarms",
                        description = "Required to fire reminders at the exact time you set.",
                        granted = perms["exactAlarms"] == true,
                        onFix = {
                            if (am.canScheduleExactAlarms()) { perms = checkPermissions(); return@PermissionRow }
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}"))
                            )
                        }
                    )

                    // 2. Battery optimization
                    PermissionRow(
                        label = "Ignore Battery Optimization",
                        description = "Prevents Android from killing the alarm while the screen is off.",
                        granted = perms["battery"] == true,
                        onFix = {
                            if (pm.isIgnoringBatteryOptimizations(context.packageName)) { perms = checkPermissions(); return@PermissionRow }
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}"))
                            )
                        }
                    )

                    // 3. Overlay (display over other apps)
                    PermissionRow(
                        label = "Display Over Other Apps (Overlay)",
                        description = "Allows the reminder to pop up over whatever app is open on screen.",
                        granted = perms["overlay"] == true,
                        onFix = {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"))
                            )
                        }
                    )

                    // 4. Notifications (Android 13+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRow(
                            label = "Post Notifications",
                            description = "Required on Android 13+ to show any notification.",
                            granted = perms["notifications"] == true,
                            onFix = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }
                        )
                    }

                    // 5. Full-screen intent (Android 14+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        PermissionRow(
                            label = "Full-Screen Notifications",
                            description = "Required on Android 14+ to show a full-screen alarm even on the lock screen.",
                            granted = perms["fullScreen"] == true,
                            onFix = {
                                try {
                                    context.startActivity(
                                        Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT",
                                            Uri.parse("package:${context.packageName}"))
                                    )
                                } catch (_: Exception) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:${context.packageName}"))
                                    )
                                }
                            }
                        )
                    }

                    // OEM autostart
                    Text("OEM Autostart — tap if you use Xiaomi / Realme / Oppo / Vivo:",
                        fontSize = 13.sp, color = TextSecondary)
                    OutlinedButton(
                        onClick = {
                            val intents = listOf(
                                Intent().apply { component = android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity") },
                                Intent().apply { component = android.content.ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.FnStartupAppListActivity") },
                                Intent().apply { component = android.content.ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity") },
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                            )
                            for (i in intents) {
                                try { context.startActivity(i); break } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open Autostart Settings") }

                    // Refresh status after returning from system settings
                    Button(
                        onClick = { perms = checkPermissions() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("↻  Refresh Permission Status") }
                }
            }

            // ── Floating Bubble ───────────────────────────────────────────────
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Floating Bubble", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Shows a draggable bubble on screen while you use other apps. Tap the bubble to return to the app. Tap × to close it.",
                            fontSize = 12.sp, color = TextSecondary
                        )
                        if (!Settings.canDrawOverlays(context)) {
                            Text("⚠ Overlay permission required — grant it in Permissions above.",
                                fontSize = 11.sp, color = StatusOverdue)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = bubbleActive,
                        onCheckedChange = { on ->
                            if (!Settings.canDrawOverlays(context)) {
                                context.startActivity(
                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}"))
                                )
                            } else {
                                val svc = Intent(context, OverlayBubbleService::class.java)
                                if (on) context.startService(svc) else context.stopService(svc)
                                bubbleActive = on
                            }
                        }
                    )
                }
            }

            // ── Test Reminder ─────────────────────────────────────────────────
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Test Reminder", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Fires a mock full-screen alarm in 5 seconds so you can verify the reminder system is working. After tapping, minimize the app and wait.",
                        fontSize = 12.sp, color = TextSecondary
                    )
                    Button(
                        onClick = {
                            if (!am.canScheduleExactAlarms()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Grant 'Exact Alarms' permission first!")
                                }
                                return@Button
                            }
                            val mockId = "mock_${System.currentTimeMillis()}"
                            AlarmScheduler.schedule(
                                context, mockId,
                                System.currentTimeMillis() + 5_000L,
                                "Test Reminder"
                            )
                            scope.launch {
                                snackbarHostState.showSnackbar("⏱ Alarm fires in 5 sec — minimize the app now!")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("▶  Fire Test Alarm in 5 Seconds", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Event Tags (admin only) ───────────────────────────────────────
            if (currentUser?.role == "admin") {
                var showAddTag by remember { mutableStateOf(false) }
                var newTagName by remember { mutableStateOf("") }
                var selectedColor by remember { mutableStateOf("#1565C0") }

                val tagColorOptions = listOf(
                    "#1565C0" to "Blue",
                    "#2E7D32" to "Green",
                    "#C62828" to "Red",
                    "#E65100" to "Orange",
                    "#6A1B9A" to "Purple",
                    "#00838F" to "Teal",
                    "#4E342E" to "Brown",
                    "#37474F" to "Grey"
                )

                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Event Tags", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            IconButton(onClick = { showAddTag = !showAddTag }) {
                                Icon(if (showAddTag) Icons.Default.Close else Icons.Default.Add, null, tint = Primary)
                            }
                        }

                        if (showAddTag) {
                            OutlinedTextField(
                                value = newTagName,
                                onValueChange = { newTagName = it },
                                label = { Text("Tag Name (e.g. AMC, Call, Demo)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Text("Colour", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                tagColorOptions.forEach { (hex, _) ->
                                    val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Primary }
                                    val isSelected = selectedColor == hex
                                    Box(
                                        Modifier
                                            .size(if (isSelected) 36.dp else 28.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .then(Modifier.clickable { selectedColor = hex }),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    if (newTagName.isNotBlank()) {
                                        scope.launch {
                                            try {
                                                viewModel.api()?.createEventTag(
                                                    mapOf("name" to newTagName.trim(), "color" to selectedColor)
                                                )
                                                viewModel.loadEventTags()
                                                newTagName = ""
                                                showAddTag = false
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar(e.message ?: "Error")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("ADD TAG") }
                        }

                        if (eventTags.isEmpty()) {
                            Text("No tags yet. Add tags like AMC, Call, Demo, Follow-up…",
                                fontSize = 13.sp, color = TextSecondary)
                        } else {
                            eventTags.forEach { tag ->
                                val tagColor = try {
                                    Color(android.graphics.Color.parseColor(tag.color))
                                } catch (e: Exception) { Primary }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(14.dp).clip(CircleShape).background(tagColor))
                                        Spacer(Modifier.width(10.dp))
                                        Text(tag.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            try {
                                                viewModel.api()?.deleteEventTag(tag.id)
                                                viewModel.loadEventTags()
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar(e.message ?: "Error")
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.Close, null, tint = StatusOverdue, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Logout ────────────────────────────────────────────────────────
            OutlinedButton(
                onClick = { viewModel.logout { onLogout() } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusOverdue)
            ) { Text("LOGOUT", color = StatusOverdue, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun PermissionRow(
    label: String,
    description: String = "",
    granted: Boolean,
    onFix: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (description.isNotBlank())
                Text(description, fontSize = 11.sp, color = TextSecondary)
            Text(
                if (granted) "✅ Granted" else "❌ Not granted",
                fontSize = 12.sp,
                color = if (granted) StatusUpcoming else StatusOverdue
            )
        }
        Spacer(Modifier.width(8.dp))
        if (!granted) {
            OutlinedButton(onClick = onFix) { Text("FIX", fontSize = 12.sp) }
        }
    }
}
