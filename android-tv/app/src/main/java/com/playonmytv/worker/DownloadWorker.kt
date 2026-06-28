package com.playonmytv.worker

import android.content.Context
import androidx.work.Data
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.playonmytv.app.di.ServiceLocator
import com.playonmytv.domain.model.MediaDownloadRequest

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val request = toDownloadRequest() ?: return Result.failure()
        val repository = ServiceLocator.provideMediaRepository(applicationContext)

        return try {
            repository.processDownload(request) { progress ->
                setProgress(
                    Data.Builder()
                        .putLong(KEY_MEDIA_ID, progress.mediaId)
                        .putLong(KEY_BYTES_DOWNLOADED, progress.bytesDownloaded)
                        .putLong(KEY_TOTAL_BYTES, progress.totalBytes)
                        .putInt(KEY_PROGRESS_PERCENT, progress.percent)
                        .build()
                )
            }
            Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            Result.failure()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun toDownloadRequest(): MediaDownloadRequest? {
        val mediaId = inputData.getLong(KEY_MEDIA_ID, -1L)
        val mediaUrl = inputData.getString(KEY_MEDIA_URL)
        val checksum = inputData.getString(KEY_CHECKSUM)
        val filename = inputData.getString(KEY_FILENAME)
        val mediaType = inputData.getString(KEY_MEDIA_TYPE)
        val size = inputData.getLong(KEY_SIZE, -1L)

        if (
            mediaId < 0 ||
            mediaUrl.isNullOrBlank() ||
            checksum.isNullOrBlank() ||
            filename.isNullOrBlank() ||
            mediaType.isNullOrBlank() ||
            size < 0
        ) {
            return null
        }

        return MediaDownloadRequest(
            id = mediaId,
            mediaUrl = mediaUrl,
            checksum = checksum,
            filename = filename,
            mediaType = mediaType,
            duration = inputData.getInt(KEY_DURATION, -1).takeIf { it >= 0 },
            width = inputData.getInt(KEY_WIDTH, -1).takeIf { it >= 0 },
            height = inputData.getInt(KEY_HEIGHT, -1).takeIf { it >= 0 },
            size = size,
            updatedAt = inputData.getLong(KEY_UPDATED_AT, -1L).takeIf { it >= 0L },
        )
    }

    companion object {
        const val KEY_MEDIA_ID = "key_media_id"
        const val KEY_MEDIA_URL = "key_media_url"
        const val KEY_CHECKSUM = "key_checksum"
        const val KEY_FILENAME = "key_filename"
        const val KEY_MEDIA_TYPE = "key_media_type"
        const val KEY_DURATION = "key_duration"
        const val KEY_WIDTH = "key_width"
        const val KEY_HEIGHT = "key_height"
        const val KEY_SIZE = "key_size"
        const val KEY_UPDATED_AT = "key_updated_at"
        const val KEY_BYTES_DOWNLOADED = "key_bytes_downloaded"
        const val KEY_TOTAL_BYTES = "key_total_bytes"
        const val KEY_PROGRESS_PERCENT = "key_progress_percent"

        fun workTag(mediaId: Long): String = "media-download-tag-$mediaId"
    }
}
