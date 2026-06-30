package com.playonmytv.data.repository

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.playonmytv.data.local.dao.MediaDao
import com.playonmytv.data.local.entities.MediaEntity
import com.playonmytv.domain.model.DownloadProgress
import com.playonmytv.domain.model.DownloadResult
import com.playonmytv.domain.model.DownloadStatus
import com.playonmytv.domain.model.MediaDownloadRequest
import com.playonmytv.domain.model.MediaEntityModel
import com.playonmytv.domain.repository.MediaRepository
import com.playonmytv.player.download.DownloadManager
import com.playonmytv.worker.DownloadWorker
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadRepository(
    private val appContext: Context,
    private val mediaDao: MediaDao,
    private val downloadManager: DownloadManager,
    private val workManager: WorkManager = WorkManager.getInstance(appContext),
) : MediaRepository {
    private val progressCallbacks = ConcurrentHashMap<Long, suspend (DownloadProgress) -> Unit>()

    override suspend fun enqueueDownload(request: MediaDownloadRequest) {
        upsertQueued(request)
        Log.i(TAG, "event=download_queued mediaId=${request.id} filename=${request.filename} checksum=${request.checksum}")

        val work = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(request.toWorkData())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(DownloadWorker.workTag(request.id))
            .build()

        workManager.enqueueUniqueWork(uniqueWorkName(request.id), ExistingWorkPolicy.REPLACE, work)
    }

    override suspend fun processDownload(
        request: MediaDownloadRequest,
        onProgress: suspend (DownloadProgress) -> Unit,
    ): DownloadResult {
        val registeredCallback = progressCallbacks[request.id]
        val existing = mediaDao.findById(request.id)
        mediaDao.updateStatus(request.id, DownloadStatus.DOWNLOADING.name)

        return try {
            val result = downloadManager.download(request) { progress ->
                registeredCallback?.invoke(progress)
                onProgress(progress)
            }

            mediaDao.upsert(
                MediaEntity(
                    id = request.id,
                    checksum = result.checksum,
                    filename = request.filename,
                    path = result.localPath.orEmpty(),
                    mediaType = request.mediaType,
                    duration = request.duration,
                    width = request.width,
                    height = request.height,
                    size = request.size,
                    status = DownloadStatus.COMPLETED.name,
                    createdAt = existing?.createdAt ?: now(),
                    updatedAt = request.updatedAt ?: existing?.updatedAt ?: now(),
                )
            )
            Log.i(
                TAG,
                "event=download_finished mediaId=${request.id} localPath=${result.localPath.orEmpty()} checksum=${result.checksum} skipped=${result.skipped}"
            )
            Log.i(TAG, "event=database_updated mediaId=${request.id} status=${DownloadStatus.COMPLETED.name}")
            result
        } catch (cancelled: Exception) {
            val status = if (cancelled is kotlinx.coroutines.CancellationException) {
                DownloadStatus.CANCELLED
            } else {
                DownloadStatus.FAILED
            }
            mediaDao.updateStatus(request.id, status.name)
            Log.e(
                TAG,
                "event=download_failed mediaId=${request.id} status=${status.name} message=${cancelled.message}",
                cancelled,
            )
            throw cancelled
        }
    }

    override suspend fun pauseDownload(mediaId: Long) {
        mediaDao.updateStatus(mediaId, DownloadStatus.PAUSED.name)
        downloadManager.cancel(mediaId)
        workManager.cancelUniqueWork(uniqueWorkName(mediaId))
    }

    override suspend fun resumeDownload(request: MediaDownloadRequest) {
        enqueueDownload(request)
    }

    override suspend fun cancelDownload(mediaId: Long) {
        mediaDao.updateStatus(mediaId, DownloadStatus.CANCELLED.name)
        downloadManager.cancel(mediaId)
        workManager.cancelUniqueWork(uniqueWorkName(mediaId))
    }

    override suspend fun findMedia(mediaId: Long): MediaEntityModel? {
        return mediaDao.findById(mediaId)?.toDomain()
    }

    override suspend fun registerProgressCallback(mediaId: Long, callback: suspend (DownloadProgress) -> Unit) {
        progressCallbacks[mediaId] = callback
    }

    override suspend fun clearProgressCallback(mediaId: Long) {
        progressCallbacks.remove(mediaId)
    }

    private suspend fun upsertQueued(request: MediaDownloadRequest) {
        val existing = mediaDao.findById(request.id)
        mediaDao.upsert(
            MediaEntity(
                id = request.id,
                checksum = request.checksum,
                filename = request.filename,
                path = existing?.path.orEmpty(),
                mediaType = request.mediaType,
                duration = request.duration,
                width = request.width,
                height = request.height,
                size = request.size,
                status = DownloadStatus.QUEUED.name,
                createdAt = existing?.createdAt ?: now(),
                updatedAt = request.updatedAt ?: existing?.updatedAt ?: now(),
            )
        )
    }

    private fun MediaEntity.toDomain(): MediaEntityModel {
        return MediaEntityModel(
            id = id,
            checksum = checksum,
            filename = filename,
            path = path,
            mediaType = mediaType,
            duration = duration,
            width = width,
            height = height,
            size = size,
            status = DownloadStatus.valueOf(status),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun MediaDownloadRequest.toWorkData(): Data {
        return Data.Builder()
            .putLong(DownloadWorker.KEY_MEDIA_ID, id)
            .putString(DownloadWorker.KEY_MEDIA_URL, mediaUrl)
            .putString(DownloadWorker.KEY_CHECKSUM, checksum)
            .putString(DownloadWorker.KEY_FILENAME, filename)
            .putString(DownloadWorker.KEY_MEDIA_TYPE, mediaType)
            .putInt(DownloadWorker.KEY_DURATION, duration ?: -1)
            .putInt(DownloadWorker.KEY_WIDTH, width ?: -1)
            .putInt(DownloadWorker.KEY_HEIGHT, height ?: -1)
            .putLong(DownloadWorker.KEY_SIZE, size)
            .putLong(DownloadWorker.KEY_UPDATED_AT, updatedAt ?: -1L)
            .build()
    }

    private fun uniqueWorkName(mediaId: Long): String = "media-download-$mediaId"

    private fun now(): Long = Instant.now().toEpochMilli()

    companion object {
        private const val TAG = "DownloadRepository"
    }
}
