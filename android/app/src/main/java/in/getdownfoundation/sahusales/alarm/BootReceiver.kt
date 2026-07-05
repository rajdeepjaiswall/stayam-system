package `in`.getdownfoundation.sahusales.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import `in`.getdownfoundation.sahusales.core.SessionStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in listOf(
                Intent.ACTION_BOOT_COMPLETED,
                "android.intent.action.QUICKBOOT_POWERON"
            )) return

        Log.d("SahuBootReceiver", "Boot completed — rescheduling alarms")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = SessionStore(context)
                val cached = store.getCachedReminders()
                val now = System.currentTimeMillis()

                for (r in cached) {
                    val fireAt = ReminderSyncer.parseTime(r.effectiveTime)
                    val label = "${r.eventTitle} — ${r.contactName ?: ""}"
                    if (fireAt > now) {
                        AlarmScheduler.schedule(context, r.id, fireAt, label)
                    }
                }
                Log.d("SahuBootReceiver", "Rescheduled ${cached.size} cached reminders")

                // Then sync fresh from server
                ReminderSyncer.sync(context)
            } catch (e: Exception) {
                Log.e("SahuBootReceiver", "Error on boot: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
