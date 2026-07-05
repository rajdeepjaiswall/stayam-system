package `in`.getdownfoundation.sahusales

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.work.Configuration
import `in`.getdownfoundation.sahusales.core.Config
import `in`.getdownfoundation.sahusales.sync.SyncWorker

class SahuApplication : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        SyncWorker.schedule(this)
        Log.d("SahuApp", "Application started")
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            Config.ALARM_CHANNEL_ID,
            Config.ALARM_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Sahu Sales reminders and alarms"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(null, null)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
}
