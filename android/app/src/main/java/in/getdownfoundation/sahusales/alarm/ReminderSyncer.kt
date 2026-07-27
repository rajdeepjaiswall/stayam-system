package `in`.getdownfoundation.sahusales.alarm

import android.content.Context
import android.util.Log
import `in`.getdownfoundation.sahusales.core.Config
import `in`.getdownfoundation.sahusales.core.ReminderFeedItem
import `in`.getdownfoundation.sahusales.core.RetrofitClient
import `in`.getdownfoundation.sahusales.core.SessionStore
import `in`.getdownfoundation.sahusales.core.UpdateReminderRequest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object ReminderSyncer {
    private const val TAG = "SahuReminderSyncer"

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val isoFormat2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)

    fun parseTime(isoString: String): Long {
        return try {
            isoFormat.parse(isoString)?.time ?: isoFormat2.parse(isoString)?.time ?: 0L
        } catch (e: Exception) {
            try { isoFormat2.parse(isoString)?.time ?: 0L } catch (e2: Exception) { 0L }
        }
    }

    suspend fun sync(context: Context) {
        val store = SessionStore(context)
        val token = store.getToken() ?: run {
            Log.d(TAG, "Not logged in, skipping sync")
            return
        }

        try {
            val api = RetrofitClient.create(Config.BASE_URL, token)
            val resp = api.getReminders(1)
            if (!resp.isSuccessful) {
                Log.e(TAG, "Sync failed: ${resp.code()} ${resp.errorBody()?.string()}")
                return
            }

            val reminders = resp.body() ?: emptyList()
            Log.d(TAG, "Synced ${reminders.size} reminders")

            // Capture old IDs BEFORE overwriting the cache, so we cancel the right alarms
            val oldIds = store.getCachedReminders().map { it.id }

            // Cache new reminders
            store.saveReminders(reminders)

            // Cancel alarms for the previously-cached reminders
            AlarmScheduler.cancelAll(context, oldIds)

            // Schedule new alarms
            val now = System.currentTimeMillis()
            for (r in reminders) {
                val fireAt = parseTime(r.effectiveTime)
                val label = "${r.eventTitle} — ${r.contactName ?: ""}"

                if (fireAt <= now) {
                    // Past-due: fire immediately (1 second from now)
                    Log.d(TAG, "Past-due reminder ${r.id}, firing immediately")
                    AlarmScheduler.schedule(context, r.id, now + 1000L, label)
                } else {
                    AlarmScheduler.schedule(context, r.id, fireAt, label)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Sync exception: ${e.message}", e)
        }
    }

    suspend fun markTriggered(context: Context, reminderId: String) {
        val store = SessionStore(context)
        val token = store.getToken() ?: return
        val api = RetrofitClient.create(Config.BASE_URL, token)
        try {
            api.updateReminder(reminderId, UpdateReminderRequest(status = "triggered"))
            Log.d(TAG, "Marked reminder $reminderId as triggered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark triggered: ${e.message}")
        }
    }

    suspend fun snooze(context: Context, reminderId: String, snoozedUntilMs: Long) {
        val store = SessionStore(context)
        val token = store.getToken() ?: return
        val api = RetrofitClient.create(Config.BASE_URL, token)
        val snoozedUntil = isoFormat.format(java.util.Date(snoozedUntilMs))
        try {
            api.updateReminder(reminderId, UpdateReminderRequest(status = "snoozed", snoozedUntil = snoozedUntil))
            Log.d(TAG, "Snoozed reminder $reminderId until $snoozedUntil")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to snooze: ${e.message}")
        }
    }

    suspend fun end(context: Context, reminderId: String) {
        val store = SessionStore(context)
        val token = store.getToken() ?: return
        val api = RetrofitClient.create(Config.BASE_URL, token)
        try {
            api.updateReminder(reminderId, UpdateReminderRequest(status = "ended"))
            Log.d(TAG, "Ended reminder $reminderId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to end: ${e.message}")
        }
    }

    suspend fun getCachedReminder(context: Context, reminderId: String): ReminderFeedItem? {
        // Return a synthetic reminder for test alarms (no network/cache needed)
        if (reminderId.startsWith("mock_")) {
            return ReminderFeedItem(
                id = reminderId,
                eventId = "mock_event",
                remindAt = "",
                effectiveTime = "",
                status = "pending",
                eventTitle = "Test Reminder",
                eventNotes = "This is a mock alarm to verify the reminder system is working.",
                contactName = "Test Contact",
                contactOrganisation = "Test Organisation",
                contactMobile = null,
                contactWhatsapp = null,
                tagName = "TEST",
                tagColor = "#1565C0",
                eventStatus = null,
                snoozedUntil = null
            )
        }
        val store = SessionStore(context)
        return store.getCachedReminders().find { it.id == reminderId }
    }
}
