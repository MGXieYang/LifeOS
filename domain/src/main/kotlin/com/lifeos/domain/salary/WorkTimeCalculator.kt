package com.lifeos.domain.salary

import com.lifeos.domain.UserSettings
import java.time.Duration
import java.time.LocalTime

enum class WorkState { BEFORE_WORK, WORKING, LUNCH_BREAK, AFTER_WORK, HOLIDAY }
object WorkTimeCalculator {
    fun dailySeconds(s: UserSettings): Long = if (s.lunchBreakEnabled) {
        Duration.between(s.workStart, s.lunchStart).seconds + Duration.between(s.lunchEnd, s.workEnd).seconds
    } else Duration.between(s.workStart, s.workEnd).seconds
    fun scheduleOccupiedSeconds(s: UserSettings): Long = Duration.between(s.workStart, s.workEnd).seconds
    fun lunchSeconds(s: UserSettings): Long = if (s.lunchBreakEnabled) Duration.between(s.lunchStart, s.lunchEnd).seconds else 0
    fun workedSeconds(s: UserSettings, time: LocalTime): Long = if (s.lunchBreakEnabled) {
        elapsed(s.workStart, s.lunchStart, time) + elapsed(s.lunchEnd, s.workEnd, time)
    } else elapsed(s.workStart, s.workEnd, time)
    private fun elapsed(start: LocalTime, end: LocalTime, now: LocalTime): Long =
        Duration.between(start, now).seconds.coerceIn(0, Duration.between(start, end).seconds)
    fun state(s: UserSettings, time: LocalTime, workday: Boolean): WorkState = when {
        !workday -> WorkState.HOLIDAY
        time < s.workStart -> WorkState.BEFORE_WORK
        !s.lunchBreakEnabled && time < s.workEnd -> WorkState.WORKING
        s.lunchBreakEnabled && time < s.lunchStart -> WorkState.WORKING
        s.lunchBreakEnabled && time < s.lunchEnd -> WorkState.LUNCH_BREAK
        s.lunchBreakEnabled && time < s.workEnd -> WorkState.WORKING
        else -> WorkState.AFTER_WORK
    }
}
