package com.playonmytv.sync

import android.util.Log
import com.playonmytv.data.local.entities.MediaEntity
import com.playonmytv.data.local.entities.SyncStateEntity
import com.playonmytv.data.local.preferences.PreferenceStore
import com.playonmytv.data.remote.ManifestApiException
import com.playonmytv.data.repository.ManifestRepository
import com.playonmytv.domain.model.DownloadStatus
import com.playonmytv.domain.model.ManifestMediaItem
import com.playonmytv.domain.model.MediaDownloadRequest
import com.playonmytv.domain.model.MediaEntityModel
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
        val currentState = manifestRepository.getSyncState()
            ?: SyncStateEntity(lastSync = null, lastSuccess = null, lastFailure = null, lastError = null)
        val startedAt = now()
        val deviceToken = preferenceStore.getDeviceToken()

        if (deviceToken.isNullOrBlank()) {
            Log.i(TAG, "event=sync_complete status=skipped reason=no_device_token")
            return SyncExecution.skipped("Device token not available.")
        }

        return try {
            Log.i(TAG, "event=sync_started")
            val manifest = manifestRepository.fetchManifest(deviceToken)
            manifestRepository.replaceManifestStructure(manifest)
            Log.i(
                TAG,
                "event=local_manifest_persisted playlists=${manifest.assignedPlaylists.size} playlistItems=${manifest.assignedPlaylists.sumOf { it.items.size }} schedules=${manifest.schedules.size} scheduleSlots=${manifest.schedules.sumOf { it.slots.size }}"
            )
            val localMedia = manifestRepository.getLocalMedia().map { it.toDomain() }
            val requiresReconcile = !currentState.lastError.isNullOrBlank()
            val diff = comparator.compare(
                remoteManifestVersion = manifest.manifestVersion,
                localManifestVersion = currentState.manifestVersion,
                localMedia = localMedia,
                manifestMedia = manifest.media,
                forceReconcile = requiresReconcile,
            )
            val unchangedCount = (manifest.media.size - diff.missingOrUpdated.size).coerceAtLeast(0)

            Log.i(
                TAG,
                "event=comparison_complete manifestVersion=${manifest.manifestVersion} newOrUpdated=${diff.missingOrUpdated.size} deleted=${diff.obsolete.size} unchanged=$unchangedCount forcedReconcile=$requiresReconcile"
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
                Log.i(TAG, "event=sync_complete status=unchanged manifestVersion=${manifest.manifestVersion}")
                return SyncExecution.unchanged(manifest.manifestVersion)
            }

            val localById = localMedia.associateBy { it.id }
            diff.obsolete.forEach { obsolete ->
                deleteLocalMedia(obsolete)
            }
            Log.i(TAG, "event=cleanup_complete deleted=${diff.obsolete.size}")

            diff.missingOrUpdated.forEach { remoteMedia ->
                val existingMedia = localById[remoteMedia.id]
                if (existingMedia != null) {
                    deleteStaleLocalCopy(existingMedia)
                }
                Log.i(
                    TAG,
                    "event=download_queued mediaId=${remoteMedia.id} filename=${remoteMedia.filename} checksum=${remoteMedia.checksum}"
                )
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
                    lastSuccess = if (pendingActiveDeletes) currentState.lastSuccess else startedAt,
                    lastError = if (pendingActiveDeletes) {
                        "Obsolete media cleanup deferred while downloads are active."
                    } else {
                        null
                    },
                )
            )

            val execution = SyncExecution.updated(
                manifestVersion = manifest.manifestVersion,
                queuedDownloads = diff.missingOrUpdated.size,
                deletedMedia = diff.obsolete.size,
                pendingCleanup = pendingActiveDeletes,
            )
            Log.i(
                TAG,
                "event=sync_complete status=updated manifestVersion=${execution.manifestVersion} queued=${execution.queuedDownloads} deleted=${execution.deletedMedia} pendingCleanup=${execution.pendingCleanup}"
            )
            execution
        } catch (exception: ManifestApiException) {
            Log.e(
                TAG,
                "event=sync_failed type=manifest_api status=${exception.statusCode ?: -1} retryable=${exception.retryable} message=${exception.message}",
                exception,
            )
            manifestRepository.saveSyncState(
                currentState.copy(
                    lastSync = startedAt,
                    lastFailure = startedAt,
                    lastError = exception.message?.take(MAX_ERROR_LENGTH) ?: "Manifest synchronization failed.",
                )
            )
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "event=sync_failed type=unexpected message=${exception.message}", exception)
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
