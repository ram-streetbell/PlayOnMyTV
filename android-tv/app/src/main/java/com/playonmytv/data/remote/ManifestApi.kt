package com.playonmytv.data.remote

import com.playonmytv.app.config.AppConfig
import com.playonmytv.domain.model.ManifestDevice
import com.playonmytv.domain.model.ManifestMediaItem
import com.playonmytv.domain.model.ManifestPayload
import com.playonmytv.domain.model.ManifestPlaylist
import com.playonmytv.domain.model.ManifestPlaylistItem
import com.playonmytv.domain.model.ManifestSchedule
import com.playonmytv.domain.model.ManifestScheduleSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class ManifestApi(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun fetchManifest(deviceToken: String): ManifestPayload = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${AppConfig.apiBaseUrl}/device/manifest")
            .get()
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $deviceToken")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            val payload = if (responseText.isBlank()) JSONObject() else JSONObject(responseText)

            if (!response.isSuccessful || !payload.optBoolean("success", false)) {
                throw IllegalStateException(payload.optString("message", "Unable to fetch device manifest."))
            }

            parseManifest(payload.getJSONObject("data"))
        }
    }

    private fun parseManifest(data: JSONObject): ManifestPayload {
        return ManifestPayload(
            manifestVersion = data.getLong("manifest_version"),
            generatedAt = data.getString("generated_at"),
            device = parseDevice(data.getJSONObject("device")),
            assignedPlaylists = parsePlaylists(data.optJSONArray("assigned_playlists")),
            schedules = parseSchedules(data.optJSONArray("schedules")),
            media = parseMedia(data.optJSONArray("media")),
        )
    }

    private fun parseDevice(json: JSONObject): ManifestDevice {
        return ManifestDevice(
            id = json.getLong("id"),
            uuid = json.getString("uuid"),
            name = json.getString("name"),
            platform = json.getString("platform"),
            appVersion = json.optNullableString("app_version"),
            firmwareVersion = json.optNullableString("firmware_version"),
            status = json.getString("status"),
            timezone = json.optNullableString("timezone"),
            screenResolution = json.optNullableString("screen_resolution"),
            lastSeenAt = json.optNullableString("last_seen_at"),
            lastSyncAt = json.optNullableString("last_sync_at"),
            updatedAt = json.optNullableString("updated_at"),
        )
    }

    private fun parsePlaylists(array: JSONArray?): List<ManifestPlaylist> {
        return array.asSequence().map { json ->
            ManifestPlaylist(
                id = json.getLong("id"),
                name = json.getString("name"),
                description = json.optNullableString("description"),
                status = json.getString("status"),
                isLooping = json.optBoolean("is_looping", false),
                updatedAt = json.optNullableString("updated_at"),
                items = json.optJSONArray("items").asSequence().map { item ->
                    ManifestPlaylistItem(
                        id = item.getLong("id"),
                        mediaId = item.getLong("media_id"),
                        sortOrder = item.getInt("sort_order"),
                        imageDurationSeconds = item.optInt("image_duration_seconds", -1).takeIf { it >= 0 },
                        updatedAt = item.optNullableString("updated_at"),
                    )
                },
            )
        }
    }

    private fun parseSchedules(array: JSONArray?): List<ManifestSchedule> {
        return array.asSequence().map { json ->
            ManifestSchedule(
                id = json.getLong("id"),
                name = json.getString("name"),
                description = json.optNullableString("description"),
                status = json.getString("status"),
                timezone = json.optNullableString("timezone"),
                assignedAt = json.optNullableString("assigned_at"),
                updatedAt = json.optNullableString("updated_at"),
                slots = json.optJSONArray("slots").asSequence().map { slot ->
                    ManifestScheduleSlot(
                        id = slot.getLong("id"),
                        playlistId = slot.getLong("playlist_id"),
                        dayOfWeek = slot.getInt("day_of_week"),
                        startTime = slot.getString("start_time"),
                        endTime = slot.getString("end_time"),
                        priority = slot.getInt("priority"),
                        updatedAt = slot.optNullableString("updated_at"),
                    )
                },
            )
        }
    }

    private fun parseMedia(array: JSONArray?): List<ManifestMediaItem> {
        return array.asSequence().map { json ->
            ManifestMediaItem(
                id = json.getLong("id"),
                filename = json.getString("filename"),
                title = json.getString("title"),
                checksum = json.getString("checksum"),
                type = json.getString("type"),
                duration = json.optInt("duration", -1).takeIf { it >= 0 },
                size = json.getLong("size"),
                width = json.optInt("width", -1).takeIf { it >= 0 },
                height = json.optInt("height", -1).takeIf { it >= 0 },
                storageUrl = json.getString("storage_url"),
                thumbnailUrl = json.optNullableString("thumbnail_url"),
                updatedAt = json.getString("updated_at"),
            )
        }
    }

    private fun JSONArray?.asSequence(): List<JSONObject> {
        if (this == null) {
            return emptyList()
        }

        val values = ArrayList<JSONObject>(length())
        for (index in 0 until length()) {
            values += getJSONObject(index)
        }
        return values
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return optString(key).takeIf { it.isNotBlank() && !isNull(key) }
    }
}
