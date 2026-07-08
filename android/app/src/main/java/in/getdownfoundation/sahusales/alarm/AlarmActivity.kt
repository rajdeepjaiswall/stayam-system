package `in`.getdownfoundation.sahusales.alarm

import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import `in`.getdownfoundation.sahusales.MainActivity
import `in`.getdownfoundation.sahusales.core.ReminderFeedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {
    private val TAG = "SahuAlarmActivity"
    private var ringtone: android.media.Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen and turn screen on
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val reminderId = intent.getStringExtra(AlarmReceiver.EXTRA_REMINDER_ID) ?: run {
            Log.e(TAG, "No reminder_id")
            finish()
            return
        }

        startRingtone()
        startVibration()

        lifecycleScope.launch {
            val reminder = withContext(Dispatchers.IO) {
                ReminderSyncer.getCachedReminder(this@AlarmActivity, reminderId)
            }
            setContent {
                AlarmScreen(
                    reminder = reminder,
                    reminderId = reminderId,
                    onEnd = {
                        lifecycleScope.launch(Dispatchers.IO) {
                            ReminderSyncer.end(this@AlarmActivity, reminderId)
                        }
                        stopAll()
                        finish()
                    },
                    onSnooze = { deltaMs ->
                        val snoozedUntil = System.currentTimeMillis() + deltaMs
                        AlarmScheduler.schedule(this@AlarmActivity, reminderId, snoozedUntil, reminder?.eventTitle ?: "Reminder")
                        lifecycleScope.launch(Dispatchers.IO) {
                            ReminderSyncer.snooze(this@AlarmActivity, reminderId, snoozedUntil)
                        }
                        stopAll()
                        finish()
                    },
                    onCall = { phone ->
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    },
                    onWhatsApp = { phone ->
                        val clean = phone.replace(Regex("[^0-9]"), "")
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$clean")))
                        } catch (e: Exception) {
                            Log.e(TAG, "WhatsApp not available: ${e.message}")
                        }
                    },
                    onOpenApp = {
                        startActivity(Intent(this@AlarmActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        })
                        finish()
                    }
                )
            }
        }
    }

    private fun startRingtone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(this, uri)?.also {
                it.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                it.isLooping = true
                it.play()
                Log.d(TAG, "Ringtone started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ringtone: ${e.message}")
        }
    }

    private fun startVibration() {
        try {
            vibrator = getSystemService(Vibrator::class.java)
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun stopAll() {
        try { ringtone?.stop() } catch (e: Exception) { }
        try { vibrator?.cancel() } catch (e: Exception) { }
    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Don't allow back press to dismiss
    }
}

@Composable
fun AlarmScreen(
    reminder: ReminderFeedItem?,
    reminderId: String,
    onEnd: () -> Unit,
    onSnooze: (Long) -> Unit,
    onCall: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    onOpenApp: () -> Unit
) {
    val tagColor = try {
        Color(android.graphics.Color.parseColor(reminder?.tagColor ?: "#1565C0"))
    } catch (e: Exception) {
        Color(0xFF1565C0)
    }
    val darkColor = tagColor.copy(alpha = 0.55f)
    val timeStr = remember {
        try {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            sdf.format(Date())
        } catch (e: Exception) { "" }
    }

    var showCustomSnooze by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tagColor, darkColor)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            // ── Tag badge ─────────────────────────────────────────────────────
            reminder?.tagName?.let { tag ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = tag.uppercase(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Contact info card ─────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = reminder?.contactName ?: "Reminder",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    reminder?.contactOrganisation?.let {
                        Text(
                            it,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Event info card ───────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = reminder?.eventTitle ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    reminder?.eventNotes?.let {
                        Text(
                            it,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        timeStr,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Quick-action buttons (Call / WA / Open App) ───────────────────
            val phone = reminder?.contactMobile
            val whatsapp = reminder?.contactWhatsapp ?: phone
            if (phone != null || whatsapp != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (phone != null) {
                        OutlinedButton(
                            onClick = { onCall(phone) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                        ) { Text("📞 Call", color = Color.White, fontSize = 13.sp) }
                    }
                    if (whatsapp != null) {
                        OutlinedButton(
                            onClick = { onWhatsApp(whatsapp) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                        ) { Text("💬 WhatsApp", color = Color.White, fontSize = 13.sp) }
                    }
                    OutlinedButton(
                        onClick = onOpenApp,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                    ) { Text("App", color = Color.White, fontSize = 13.sp) }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Snooze section ────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.13f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "SNOOZE",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Row 1: 5 min, 15 min, 30 min
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("5 min" to 5L, "15 min" to 15L, "30 min" to 30L).forEach { (label, mins) ->
                            OutlinedButton(
                                onClick = { onSnooze(mins * 60 * 1000) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                            ) { Text(label, color = Color.White, fontSize = 12.sp) }
                        }
                    }
                    // Row 2: 1 hour, 3 hours, Custom
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1 hour" to 60L, "3 hours" to 180L).forEach { (label, mins) ->
                            OutlinedButton(
                                onClick = { onSnooze(mins * 60 * 1000) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                            ) { Text(label, color = Color.White, fontSize = 12.sp) }
                        }
                        OutlinedButton(
                            onClick = { showCustomSnooze = !showCustomSnooze },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                        ) { Text("Custom", color = Color.White, fontSize = 12.sp) }
                    }

                    if (showCustomSnooze) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = customMinutes,
                                onValueChange = { customMinutes = it.filter { c -> c.isDigit() } },
                                label = { Text("Minutes", color = Color.White) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val mins = customMinutes.toLongOrNull() ?: 5L
                                    onSnooze(mins * 60 * 1000)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) { Text("GO", color = tagColor, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── END REMINDER button ───────────────────────────────────────────
            Button(
                onClick = onEnd,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("END REMINDER", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
