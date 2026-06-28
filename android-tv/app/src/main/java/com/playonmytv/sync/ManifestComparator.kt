package com.playonmytv.sync

import com.playonmytv.domain.model.DownloadStatus
import com.playonmytv.domain.model.ManifestDiff
import com.playonmytv.domain.model.ManifestMediaItem
import com.playonmytv.domain.model.MediaEntityModel
import java.io.File

class ManifestComparator {
    fun compare(
        remoteManifestVersion: Long,
        localManifestVersion: Long,
        localMedia: List<MediaEntityModel>,
        manifestMedia: List<ManifestMediaItem>,
        forceReconcile: Boolean,
    ): ManifestDiff {
        if (!forceReconcile && remoteManifestVersion == localManifestVersion) {
            return ManifestDiff(
                isSameVersion = true,
                missingOrUpdated = emptyList(),
                obsolete = emptyList(),
            )
        }

        val manifestById = manifestMedia.associateBy { it.id }
        val localById = localMedia.associateBy { it.id }

        val missingOrUpdated = manifestMedia.filter { remote ->
            val local = localById[remote.id] ?: return@filter true
            val isInFlight = local.status == DownloadStatus.QUEUED || local.status == DownloadStatus.DOWNLOADING
            val fileMissing = local.path.isBlank() || !File(local.path).exists()

            local.checksum != remote.checksum || (!isInFlight && fileMissing)
        }

        val obsolete = localMedia.filter { local ->
            manifestById[local.id] == null &&
                local.status != DownloadStatus.QUEUED &&
                local.status != DownloadStatus.DOWNLOADING
        }

        return ManifestDiff(
            isSameVersion = false,
            missingOrUpdated = missingOrUpdated,
            obsolete = obsolete,
        )
    }
}
