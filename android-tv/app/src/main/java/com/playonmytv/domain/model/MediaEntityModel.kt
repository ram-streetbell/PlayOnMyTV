package com.playonmytv.domain.model

data class MediaEntityModel(
    val id: Long,
    val checksum: String,
    val filename: String,
    val path: String,
    val mediaType: String,
    val duration: Int?,
    val width: Int?,
    val height: Int?,
    val size: Long,
    val status: DownloadStatus,
    val createdAt: Long,
    val updatedAt: Long,
)

