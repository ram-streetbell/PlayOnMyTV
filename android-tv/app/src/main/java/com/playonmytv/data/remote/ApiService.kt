package com.playonmytv.data.remote

import com.playonmytv.app.config.AppConfig
import com.playonmytv.domain.model.PairingStartResult
import com.playonmytv.domain.model.PairingStatusResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime

class ApiService {
    suspend fun startPairing(
        deviceUuid: String,
        deviceName: String,
        appVersion: String
    ): PairingStartResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("device_uuid", deviceUuid)
            .put("device_name", deviceName)
            .put("app_version", appVersion)

        val response = postJson("/device/pairing/start", payload)
        val data = response.getJSONObject("data")

        PairingStartResult(
            deviceUuid = data.getString("device_uuid"),
            pairingCode = data.getString("pairing_code"),
            expiresAt = OffsetDateTime.parse(data.getString("expires_at"))
        )
    }

    suspend fun pairingStatus(deviceUuid: String): PairingStatusResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("device_uuid", deviceUuid)
        val response = postJson("/device/pairing/status", payload)
        val data = response.getJSONObject("data")

        PairingStatusResult(
            waiting = data.optBoolean("waiting", true),
            deviceToken = data.optString("device_token").takeIf { it.isNotBlank() },
            deviceName = data.optString("device_name").takeIf { it.isNotBlank() },
            syncInterval = data.optInt("sync_interval", 300)
        )
    }

    private fun postJson(path: String, payload: JSONObject): JSONObject {
        val connection = URL(AppConfig.apiBaseUrl + path).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.doOutput = true
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json")

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        return try {
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            val json = JSONObject(responseText)

            if (connection.responseCode !in 200..299 || !json.optBoolean("success", false)) {
                throw IllegalStateException(json.optString("message", "Unexpected API error."))
            }

            json
        } finally {
            connection.disconnect()
        }
    }
}
