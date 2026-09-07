package com.niuma.driver.domain.salary

import com.niuma.driver.domain.UserSettings
import java.time.Duration
import java.time.LocalTime

enum class WorkState { BEFORE_WORK, WORKING, LUNCH_BREAK, WORKING_AFTERNOON, AFTER_WORK, HOLIDAY }
object WorkTimeCalculator {
    fun dailySeconds(s: UserSettings): Long = Duration.between(s.workStart, s.lunchStart).seconds +
        Duration.between(s.lunchEnd, s.workEnd).seconds
    fun workedSeconds(s: UserSettings, time: LocalTime): Long =
        elapsed(s.workStart, s.lunchStart, time) + elapsed(s.lunchEnd, s.workEnd, time)
    private fun elapsed(start: LocalTime, end: LocalTime, now: LocalTime): Long =
        Duration.between(start, now).seconds.coerceIn(0, Duration.between(start, end).seconds)
    fun state(s: UserSettings, time: LocalTime, workday: Boolean): WorkState = when {
        !workday -> WorkState.HOLIDAY
        time < s.workStart -> WorkState.BEFORE_WORK
        time < s.lunchStart -> WorkState.WORKING
        time < s.lunchEnd -> WorkState.LUNCH_BREAK
        time < s.workEnd -> WorkState.WORKING_AFTERNOON
        else -> WorkState.AFTER_WORK
    }
}
