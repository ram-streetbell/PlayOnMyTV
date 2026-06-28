package com.playonmytv.domain.model

data class DownloadProgress(
    val mediaId: Long,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percent: Int,
)

