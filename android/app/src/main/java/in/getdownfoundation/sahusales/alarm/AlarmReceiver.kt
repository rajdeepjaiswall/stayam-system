package `in`.getdownfoundation.sahusales.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import `in`.getdownfoundation.sahusales.core.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "SahuAlarmReceiver"
        const val ACTION_FIRE = "in.getdownfoundation.sahusales.ALARM_FIRE"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val NOTIFICATION_ID_BASE = 10000
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return

        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: run {
            Log.e(TAG, "No reminder_id in intent")
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Sahu Reminder"

        Log.d(TAG, "Alarm fired! reminderId=$reminderId title=$title")

        // Create notification channel
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            Config.ALARM_CHANNEL_ID,
            Config.ALARM_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Sahu Sales reminder notifications"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(null, null) // AlarmActivity handles sound
        }
        nm.createNotificationChannel(channel)

        // Full-screen intent to launch AlarmActivity
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Config.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Sahu Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPi, true)
            .build()

        nm.notify(NOTIFICATION_ID_BASE + reminderId.hashCode(), notification)

        // Also start the activity directly
        try {
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AlarmActivity directly: ${e.message}")
        }

        // Best-effort: mark as triggered in background
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderSyncer.markTriggered(context, reminderId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark triggered: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
