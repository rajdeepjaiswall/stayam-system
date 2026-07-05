package `in`.getdownfoundation.sahusales.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import `in`.getdownfoundation.sahusales.MainActivity

object AlarmScheduler {
    private const val TAG = "SahuAlarmScheduler"

    fun schedule(context: Context, reminderId: String, triggerMs: Long, title: String = "") {
        val am = context.getSystemService(AlarmManager::class.java)

        if (!am.canScheduleExactAlarms()) {
            Log.e(TAG, "Cannot schedule exact alarms — permission not granted")
            return
        }

        val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
        }
        val firePi = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPi = PendingIntent.getActivity(
            context, 0, showIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val info = AlarmManager.AlarmClockInfo(triggerMs, showPi)
        am.setAlarmClock(info, firePi)

        Log.d(TAG, "Scheduled alarm for reminder=$reminderId at $triggerMs (${java.util.Date(triggerMs)})")
    }

    fun cancel(context: Context, reminderId: String) {
        val am = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
        }
        val pi = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let {
            am.cancel(it)
            it.cancel()
            Log.d(TAG, "Cancelled alarm for reminder=$reminderId")
        }
    }

    fun cancelAll(context: Context, reminderIds: List<String>) {
        reminderIds.forEach { cancel(context, it) }
    }
}
