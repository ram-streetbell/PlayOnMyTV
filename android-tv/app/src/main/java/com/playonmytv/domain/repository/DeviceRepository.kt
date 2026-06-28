package com.playonmytv.domain.repository

import com.playonmytv.domain.model.PairingStartResult
import com.playonmytv.domain.model.PairingStatusResult

interface DeviceRepository {
    fun getOrCreateDeviceUuid(): String

    fun getDeviceToken(): String?

    fun savePairedDevice(deviceToken: String, deviceName: String?)

    suspend fun requestPairing(deviceName: String, appVersion: String): PairingStartResult

    suspend fun checkPairingStatus(): PairingStatusResult
}
