package com.lifeos.domain.retirement

import com.lifeos.domain.RetirementType
import com.lifeos.domain.calendar.WorkCalendar
import java.time.LocalDate
import java.time.Clock
import java.time.Period
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class RetirementPolicy(val version: String = "CN-2025.1", val effectiveDate: LocalDate = LocalDate.of(2025, 1, 1))
data class RetirementResult(val date: LocalDate, val remaining: Period, val estimatedWorkdays: Long,
    val delayMonths: Long, val reached: Boolean, val policyVersion: String)
class RetirementCalculator(private val calendar: WorkCalendar, private val policy: RetirementPolicy = RetirementPolicy()) {
    fun calculate(birth: LocalDate, type: RetirementType, clock: Clock): RetirementResult =
        calculate(birth, type, LocalDate.now(clock))

    fun calculate(birth: LocalDate, type: RetirementType, today: LocalDate): RetirementResult {
        require(birth <= today) { "出生日期不能晚于今天" }
        val baseAge = when (type) { RetirementType.MALE -> 60; RetirementType.FEMALE_55 -> 55; RetirementType.FEMALE_50 -> 50 }
        val step = if (type == RetirementType.FEMALE_50) 2 else 4
        val cap = if (type == RetirementType.FEMALE_50) 60L else 36L
        val baseline = birth.plusYears(baseAge.toLong())
        val offset = ChronoUnit.MONTHS.between(YearMonth.from(policy.effectiveDate), YearMonth.from(baseline))
        val delay = if (offset < 0) 0 else (offset / step + 1).coerceAtMost(cap)
        // Published tables define a retirement month. The birthday within that month is a UI estimate.
        val date = birth.plusMonths(baseAge * 12L + delay)
        val reached = today >= date
        var workdays = 0L
        var cursor = today
        while (cursor < date) {
            if (calendar.isWorkday(cursor)) workdays++
            cursor = cursor.plusDays(1)
        }
        return RetirementResult(date, if (reached) Period.ZERO else Period.between(today, date),
            workdays, delay, reached, policy.version)
    }
}
