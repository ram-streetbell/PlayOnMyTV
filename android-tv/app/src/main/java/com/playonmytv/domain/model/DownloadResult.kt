package com.playonmytv.domain.model

data class DownloadResult(
    val mediaId: Long,
    val success: Boolean,
    val localPath: String?,
    val checksum: String,
    val skipped: Boolean = false,
    val message: String? = null,
)

