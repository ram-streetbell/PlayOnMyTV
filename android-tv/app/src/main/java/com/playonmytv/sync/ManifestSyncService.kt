package com.playonmytv.sync

import android.util.Log
import com.playonmytv.data.local.entities.MediaEntity
import com.playonmytv.data.local.entities.SyncStateEntity
import com.playonmytv.data.local.preferences.PreferenceStore
import com.playonmytv.data.repository.ManifestRepository
import com.playonmytv.domain.model.ManifestMediaItem
import com.playonmytv.domain.model.MediaDownloadRequest
import com.playonmytv.domain.model.MediaEntityModel
import com.playonmytv.domain.model.DownloadStatus
import com.playonmytv.domain.model.ManifestDiff
import com.playonmytv.domain.repository.MediaRepository
import com.playonmytv.player.download.MediaStorageHelper
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ManifestSyncService(
    private val preferenceStore: PreferenceStore,
    private val manifestRepository: ManifestRepository,
    private val mediaRepository: MediaRepository,
    private val storageHelper: MediaStorageHelper,
    private val comparator: ManifestComparator,
) {
    suspend fun synchronize(): SyncExecution {
        val currentState = manifestRepository.getSyncState() ?: SyncStateEntity(lastSync = null, lastSuccess = null, lastFailure = null, lastError = null)
        val startedAt = now()
        val deviceToken = preferenceStore.getDeviceToken()

        if (deviceToken.isNullOrBlank()) {
            return SyncExecution.skipped("Device token not available.")
        }

        return try {
            val manifest = manifestRepository.fetchManifest(deviceToken)
            val localMedia = manifestRepository.getLocalMedia().map { it.toDomain() }
            val requiresReconcile = !currentState.lastError.isNullOrBlank()
            val diff = comparator.compare(
                remoteManifestVersion = manifest.manifestVersion,
                localManifestVersion = currentState.manifestVersion,
                localMedia = localMedia,
                manifestMedia = manifest.media,
                forceReconcile = requiresReconcile,
            )

            if (diff.isSameVersion) {
                manifestRepository.saveSyncState(
                    currentState.copy(
                        manifestVersion = manifest.manifestVersion,
                        lastSync = startedAt,
                        lastSuccess = startedAt,
                        lastError = null,
                    )
                )
                return SyncExecution.unchanged(manifest.manifestVersion)
            }

            val localById = localMedia.associateBy { it.id }
            diff.obsolete.forEach { obsolete ->
                deleteLocalMedia(obsolete)
            }

            diff.missingOrUpdated.forEach { remoteMedia ->
                localById[remoteMedia.id]?.let(::deleteStaleLocalCopy)
                mediaRepository.enqueueDownload(remoteMedia.toDownloadRequest())
            }

            val pendingActiveDeletes = manifestRepository.getLocalMedia()
                .map { it.toDomain() }
                .any { local ->
                    manifest.media.none { it.id == local.id } &&
                        (local.status == DownloadStatus.QUEUED || local.status == DownloadStatus.DOWNLOADING)
                }

            manifestRepository.saveSyncState(
                currentState.copy(
                    manifestVersion = manifest.manifestVersion,
                    lastSync = startedAt,
                    lastSuccess = startedAt.takeUnless { pendingActiveDeletes },
                    lastError = if (pendingActiveDeletes) {
                        "Obsolete media cleanup deferred while downloads are active."
                    } else {
                        null
                    },
                )
            )

            SyncExecution.updated(
                manifestVersion = manifest.manifestVersion,
                queuedDownloads = diff.missingOrUpdated.size,
                deletedMedia = diff.obsolete.size,
                pendingCleanup = pendingActiveDeletes,
            )
        } catch (exception: Exception) {
            Log.e(TAG, "Manifest synchronization failed.", exception)
            manifestRepository.saveSyncState(
                currentState.copy(
                    lastSync = startedAt,
                    lastFailure = startedAt,
                    lastError = exception.message?.take(MAX_ERROR_LENGTH) ?: "Manifest synchronization failed.",
                )
            )
            throw exception
        }
    }

    private suspend fun deleteLocalMedia(media: MediaEntityModel) {
        deleteFilesFor(media)
        manifestRepository.deleteLocalMedia(media.id)
    }

    private suspend fun deleteStaleLocalCopy(media: MediaEntityModel) {
        deleteFilesFor(media)
        manifestRepository.deleteLocalMedia(media.id)
    }

    private fun deleteFilesFor(media: MediaEntityModel) {
        if (media.path.isNotBlank()) {
            File(media.path).takeIf(File::exists)?.delete()
            File(media.path + ".part").takeIf(File::exists)?.delete()
        } else {
            storageHelper.ensureFreshDownloadTarget(media.filename, media.mediaType)
        }
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

    private fun ManifestMediaItem.toDownloadRequest(): MediaDownloadRequest {
        return MediaDownloadRequest(
            id = id,
            mediaUrl = storageUrl,
            checksum = checksum,
            filename = filename,
            mediaType = type,
            duration = duration,
            width = width,
            height = height,
            size = size,
            updatedAt = parseTimestamp(updatedAt),
        )
    }

    private fun parseTimestamp(value: String?): Long? {
        if (value.isNullOrBlank()) {
            return null
        }

        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(value, MYSQL_TIMESTAMP_FORMAT)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            }.getOrNull()
    }

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        private const val TAG = "ManifestSyncService"
        private const val MAX_ERROR_LENGTH = 500
        private val MYSQL_TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
