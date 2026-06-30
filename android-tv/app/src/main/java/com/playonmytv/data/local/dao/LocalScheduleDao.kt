package com.playonmytv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.playonmytv.data.local.entities.LocalScheduleEntity
import com.playonmytv.data.local.entities.LocalScheduleSlotEntity

@Dao
interface LocalScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedules(schedules: List<LocalScheduleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScheduleSlots(slots: List<LocalScheduleSlotEntity>)

    @Query("DELETE FROM local_schedule_slots")
    suspend fun clearScheduleSlots()

    @Query("DELETE FROM local_schedules")
    suspend fun clearSchedules()

    @Query("SELECT COUNT(*) FROM local_schedules")
    suspend fun countSchedules(): Int

    @Query(
        """
        SELECT
            lss.id AS slotId,
            lss.scheduleId AS scheduleId,
            lss.playlistId AS playlistId,
            lss.dayOfWeek AS dayOfWeek,
            lss.startTime AS startTime,
            lss.endTime AS endTime,
            lss.priority AS priority,
            lss.updatedAt AS slotUpdatedAt,
            ls.name AS scheduleName,
            ls.timezone AS scheduleTimezone,
            ls.status AS scheduleStatus,
            ls.updatedAt AS scheduleUpdatedAt,
            lp.name AS playlistName,
            lp.isLooping AS playlistLooping,
            lp.status AS playlistStatus,
            lp.updatedAt AS playlistUpdatedAt
        FROM local_schedule_slots lss
        INNER JOIN local_schedules ls ON ls.id = lss.scheduleId
        INNER JOIN local_playlists lp ON lp.id = lss.playlistId
        WHERE ls.status = :activeStatus
          AND lp.status = :activeStatus
        ORDER BY lss.priority DESC, lss.startTime ASC, lss.id ASC
        """
    )
    suspend fun findScheduleCandidates(activeStatus: String): List<LocalActiveScheduleSlotRow>

    @Query(
        """
        SELECT
            lss.id AS slotId,
            lss.scheduleId AS scheduleId,
            lss.playlistId AS playlistId,
            lss.dayOfWeek AS dayOfWeek,
            lss.startTime AS startTime,
            lss.endTime AS endTime,
            lss.priority AS priority,
            lss.updatedAt AS slotUpdatedAt,
            ls.name AS scheduleName,
            ls.timezone AS scheduleTimezone,
            ls.status AS scheduleStatus,
            ls.updatedAt AS scheduleUpdatedAt,
            lp.name AS playlistName,
            lp.isLooping AS playlistLooping,
            lp.status AS playlistStatus,
            lp.updatedAt AS playlistUpdatedAt
        FROM local_schedule_slots lss
        INNER JOIN local_schedules ls ON ls.id = lss.scheduleId
        INNER JOIN local_playlists lp ON lp.id = lss.playlistId
        WHERE lss.dayOfWeek = :backendDayOfWeek
          AND :currentTime >= lss.startTime
          AND :currentTime < lss.endTime
          AND ls.status = :activeStatus
          AND lp.status = :activeStatus
        ORDER BY lss.priority DESC, lss.startTime ASC, lss.id ASC
        LIMIT 1
        """
    )
    suspend fun findActiveScheduleSlot(
        backendDayOfWeek: Int,
        currentTime: String,
        activeStatus: String,
    ): LocalActiveScheduleSlotRow?
}
