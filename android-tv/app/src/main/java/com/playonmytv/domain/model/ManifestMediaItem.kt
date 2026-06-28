package com.playonmytv.domain.model

data class ManifestMediaItem(
    val id: Long,
    val filename: String,
    val title: String,
    val checksum: String,
    val type: String,
    val duration: Int?,
    val size: Long,
    val width: Int?,
    val height: Int?,
    val storageUrl: String,
    val thumbnailUrl: String?,
    val updatedAt: String,
)

