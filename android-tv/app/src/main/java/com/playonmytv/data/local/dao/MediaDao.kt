package com.playonmytv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.playonmytv.data.local.entities.MediaEntity

@Dao
interface MediaDao {
    @Query("SELECT * FROM media WHERE id = :mediaId LIMIT 1")
    suspend fun findById(mediaId: Long): MediaEntity?

    @Query("SELECT * FROM media")
    suspend fun findAll(): List<MediaEntity>

    @Query("SELECT * FROM media WHERE status IN (:statuses)")
    suspend fun findByStatuses(statuses: List<String>): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mediaEntity: MediaEntity)

    @Query("UPDATE media SET status = :status WHERE id = :mediaId")
    suspend fun updateStatus(mediaId: Long, status: String)

    @Query("DELETE FROM media WHERE id = :mediaId")
    suspend fun deleteById(mediaId: Long)
}
