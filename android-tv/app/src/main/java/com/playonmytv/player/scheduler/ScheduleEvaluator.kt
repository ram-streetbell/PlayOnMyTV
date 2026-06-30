package com.playonmytv.player.scheduler

import com.playonmytv.domain.model.LocalPlaybackCandidate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleEvaluator {
    fun selectActiveCandidate(
        candidates: List<LocalPlaybackCandidate>,
        now: ZonedDateTime = ZonedDateTime.now(),
    ): LocalPlaybackCandidate? {
        return candidates
            .filter { candidate -> isCandidateActive(candidate, now) }
            .sortedWith(
                compareByDescending<LocalPlaybackCandidate> { it.slot.priority }
                    .thenBy { it.slot.startTime }
                    .thenBy { it.slot.slotId }
            )
            .firstOrNull()
    }

    private fun isCandidateActive(
        candidate: LocalPlaybackCandidate,
        now: ZonedDateTime,
    ): Boolean {
        val zoneId = candidate.schedule.timezone
            ?.takeIf { it.isNotBlank() }
            ?.let(::safeZoneId)
            ?: now.zone
        val zonedNow = now.withZoneSameInstant(zoneId)
        val currentTime = zonedNow.toLocalTime()
        val startTime = LocalTime.parse(candidate.slot.startTime)
        val endTime = LocalTime.parse(candidate.slot.endTime)
        val currentDay = backendDayOfWeek(zonedNow)

        return if (endTime > startTime) {
            candidate.slot.dayOfWeek == currentDay &&
                currentTime >= startTime &&
                currentTime < endTime
        } else {
            val previousDay = if (currentDay == 1) 7 else currentDay - 1
            (candidate.slot.dayOfWeek == currentDay && currentTime >= startTime) ||
                (candidate.slot.dayOfWeek == previousDay && currentTime < endTime)
        }
    }

    private fun backendDayOfWeek(now: ZonedDateTime): Int {
        return now.dayOfWeek.value
    }

    private fun safeZoneId(value: String): ZoneId {
        return try {
            ZoneId.of(value)
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }
    }
}
