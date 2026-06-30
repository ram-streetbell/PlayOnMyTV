package com.playonmytv.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.playonmytv.app.di.ServiceLocator
import com.playonmytv.data.remote.ManifestApiException
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val service = ServiceLocator.provideManifestSyncService(applicationContext)

        return try {
            val execution = service.synchronize()
            Log.i(
                TAG,
                "event=sync_worker_complete status=${execution.status} manifestVersion=${execution.manifestVersion ?: -1} queued=${execution.queuedDownloads} deleted=${execution.deletedMedia} pendingCleanup=${execution.pendingCleanup}"
            )
            Result.success()
        } catch (exception: ManifestApiException) {
            Log.e(
                TAG,
                "event=sync_worker_failed status=${exception.statusCode ?: -1} retryable=${exception.retryable} message=${exception.message}",
                exception,
            )
            if (exception.retryable) Result.retry() else Result.failure()
        } catch (exception: Exception) {
            Log.e(TAG, "event=sync_worker_failed status=unexpected message=${exception.message}", exception)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val PERIODIC_WORK_NAME = "device-manifest-sync-periodic"
        private const val IMMEDIATE_WORK_NAME = "device-manifest-sync-immediate"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(defaultConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(PERIODIC_WORK_NAME)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(defaultConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(IMMEDIATE_WORK_NAME)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        private fun defaultConstraints(): Constraints {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        }
    }
}
