package com.playonmytv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.playonmytv.data.local.entities.LocalPlaylistEntity
import com.playonmytv.data.local.entities.LocalPlaylistItemEntity

@Dao
interface LocalPlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylists(playlists: List<LocalPlaylistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylistItems(items: List<LocalPlaylistItemEntity>)

    @Query("DELETE FROM local_playlist_items")
    suspend fun clearPlaylistItems()

    @Query("DELETE FROM local_playlists")
    suspend fun clearPlaylists()

    @Query("SELECT COUNT(*) FROM local_playlists")
    suspend fun countPlaylists(): Int

    @Query("SELECT * FROM local_playlists WHERE id = :playlistId LIMIT 1")
    suspend fun findPlaylistById(playlistId: Long): LocalPlaylistEntity?

    @Query(
        """
        SELECT
            lpi.id AS playlistItemId,
            lpi.playlistId AS playlistId,
            lpi.mediaId AS mediaId,
            lpi.sortOrder AS sortOrder,
            lpi.imageDurationSeconds AS imageDurationSeconds,
            m.filename AS filename,
            m.path AS path,
            m.mediaType AS mediaType,
            m.checksum AS checksum,
            m.duration AS duration,
            m.width AS width,
            m.height AS height,
            m.size AS size,
            m.updatedAt AS updatedAt
        FROM local_playlist_items lpi
        INNER JOIN media m ON m.id = lpi.mediaId
        WHERE lpi.playlistId = :playlistId
          AND m.status = :completedStatus
        ORDER BY lpi.sortOrder ASC, lpi.id ASC
        """
    )
    suspend fun getOrderedMediaRows(
        playlistId: Long,
        completedStatus: String,
    ): List<LocalPlaylistMediaRow>
}
