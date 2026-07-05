package `in`.getdownfoundation.sahusales.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import `in`.getdownfoundation.sahusales.alarm.ReminderSyncer
import `in`.getdownfoundation.sahusales.core.Config
import java.util.concurrent.TimeUnit

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            Log.d("SahuSyncWorker", "Running background sync")
            ReminderSyncer.sync(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e("SahuSyncWorker", "Sync failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Config.SYNC_WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.d("SahuSyncWorker", "Scheduled 15-min sync worker")
        }
    }
}
