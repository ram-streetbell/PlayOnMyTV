package com.playonmytv.domain.model

data class ManifestPayload(
    val manifestVersion: Long,
    val generatedAt: String,
    val device: ManifestDevice,
    val assignedPlaylists: List<ManifestPlaylist>,
    val schedules: List<ManifestSchedule>,
    val media: List<ManifestMediaItem>,
)

