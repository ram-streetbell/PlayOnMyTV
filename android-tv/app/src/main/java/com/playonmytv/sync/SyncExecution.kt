package com.playonmytv.sync

data class SyncExecution(
    val status: Status,
    val manifestVersion: Long?,
    val queuedDownloads: Int,
    val deletedMedia: Int,
    val pendingCleanup: Boolean,
    val message: String?,
) {
    enum class Status {
        SKIPPED,
        UNCHANGED,
        UPDATED,
    }

    companion object {
        fun skipped(message: String): SyncExecution {
            return SyncExecution(
                status = Status.SKIPPED,
                manifestVersion = null,
                queuedDownloads = 0,
                deletedMedia = 0,
                pendingCleanup = false,
                message = message,
            )
        }

        fun unchanged(manifestVersion: Long): SyncExecution {
            return SyncExecution(
                status = Status.UNCHANGED,
                manifestVersion = manifestVersion,
                queuedDownloads = 0,
                deletedMedia = 0,
                pendingCleanup = false,
                message = null,
            )
        }

        fun updated(
            manifestVersion: Long,
            queuedDownloads: Int,
            deletedMedia: Int,
            pendingCleanup: Boolean,
        ): SyncExecution {
            return SyncExecution(
                status = Status.UPDATED,
                manifestVersion = manifestVersion,
                queuedDownloads = queuedDownloads,
                deletedMedia = deletedMedia,
                pendingCleanup = pendingCleanup,
                message = null,
            )
        }
    }
}
