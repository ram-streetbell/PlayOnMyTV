package com.playonmytv.domain.model

data class ManifestDiff(
    val isSameVersion: Boolean,
    val missingOrUpdated: List<ManifestMediaItem>,
    val obsolete: List<MediaEntityModel>,
)

