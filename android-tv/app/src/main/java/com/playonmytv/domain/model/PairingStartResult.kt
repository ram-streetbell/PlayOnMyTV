package com.playonmytv.domain.model

import java.time.OffsetDateTime

data class PairingStartResult(
    val deviceUuid: String,
    val pairingCode: String,
    val expiresAt: OffsetDateTime
)

