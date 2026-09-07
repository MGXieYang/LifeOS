package com.niuma.driver.domain.freetime

import com.niuma.driver.domain.UserSettings
import com.niuma.driver.domain.salary.WorkTimeCalculator

data class FreeTimeResult(
    val freeSeconds: Long,
    val workRelatedSeconds: Long,
    val freeRatio: Double,
    val workRelatedRatio: Double,
    val isClamped: Boolean,
)

object FreeTimeCalculator {
    private const val DAY_SECONDS = 24 * 60 * 60L
    fun calculate(settings: UserSettings): FreeTimeResult {
        val sleep = settings.sleepMinutes * 60L
        val commute = settings.commuteMinutes * 60L
        val necessary = if (settings.necessaryLifeEnabled) settings.necessaryLifeMinutes * 60L else 0L
        val schedule = WorkTimeCalculator.scheduleOccupiedSeconds(settings)
        val workRelated = schedule + commute
        val raw = DAY_SECONDS - sleep - workRelated - necessary
        val free = raw.coerceAtLeast(0)
        return FreeTimeResult(free, workRelated, free.toDouble() / DAY_SECONDS,
            workRelated.toDouble() / DAY_SECONDS, raw < 0)
    }
}
