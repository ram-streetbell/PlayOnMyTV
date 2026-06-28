package com.playonmytv.domain.model

data class ManifestDevice(
    val id: Long,
    val uuid: String,
    val name: String,
    val platform: String,
    val appVersion: String?,
    val firmwareVersion: String?,
    val status: String,
    val timezone: String?,
    val screenResolution: String?,
    val lastSeenAt: String?,
    val lastSyncAt: String?,
    val updatedAt: String?,
)

