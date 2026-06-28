package com.playonmytv.data.repository

import com.playonmytv.data.local.preferences.PreferenceStore
import com.playonmytv.data.remote.ApiService
import com.playonmytv.domain.model.PairingStartResult
import com.playonmytv.domain.model.PairingStatusResult
import com.playonmytv.domain.repository.DeviceRepository

class DeviceRepositoryImpl(
    private val apiService: ApiService,
    private val preferenceStore: PreferenceStore
) : DeviceRepository {
    override fun getOrCreateDeviceUuid(): String = preferenceStore.getOrCreateDeviceUuid()

    override fun getDeviceToken(): String? = preferenceStore.getDeviceToken()

    override fun savePairedDevice(deviceToken: String, deviceName: String?) {
        preferenceStore.saveDeviceToken(deviceToken)
        if (!deviceName.isNullOrBlank()) {
            preferenceStore.saveDeviceName(deviceName)
        }
    }

    override suspend fun requestPairing(deviceName: String, appVersion: String): PairingStartResult {
        return apiService.startPairing(
            deviceUuid = getOrCreateDeviceUuid(),
            deviceName = deviceName,
            appVersion = appVersion
        )
    }

    override suspend fun checkPairingStatus(): PairingStatusResult {
        return apiService.pairingStatus(getOrCreateDeviceUuid())
    }
}
