package com.playonmytv.domain.model

data class PairingStatusResult(
    val waiting: Boolean,
    val deviceToken: String?,
    val deviceName: String?,
    val syncInterval: Int
)

