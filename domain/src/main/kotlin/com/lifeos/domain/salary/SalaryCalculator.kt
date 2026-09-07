package com.lifeos.domain.salary

import com.lifeos.domain.UserSettings
import com.lifeos.domain.calendar.WorkCalendar
import java.math.BigDecimal
import java.math.MathContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth

data class SalaryResult(
    val now: LocalDateTime,
    val state: WorkState,
    val monthlyWorkDays: Int,
    val completedWorkDays: Int,
    val dailyWorkSeconds: Long,
    val monthlyWorkSeconds: Long,
    val todayWorkedSeconds: Long,
    val salaryPerSecond: BigDecimal,
    val todayEarned: BigDecimal,
    val monthEarned: BigDecimal,
    val todayProgress: Double,
    val monthProgress: Double,
    val countdownSeconds: Long,
    val payday: LocalDateTime,
    val paydaySeconds: Long,
    val isPayday: Boolean,
    val calendarWarning: String?,
)
class SalaryCalculator(private val calendar: WorkCalendar) {
    fun calculate(s: UserSettings, clock: Clock): SalaryResult = calculate(s, LocalDateTime.now(clock))
    fun calculate(s: UserSettings, now: LocalDateTime): SalaryResult {
        val date = now.toLocalDate()
        val time = now.toLocalTime()
        val month = YearMonth.from(date)
        val days = calendar.workdays(month)
        val workday = calendar.isWorkday(date)
        val daily = WorkTimeCalculator.dailySeconds(s)
        val monthly = days.size * daily
        val worked = if (workday) WorkTimeCalculator.workedSeconds(s, time) else 0L
        val previous = days.count { it < date }
        val state = WorkTimeCalculator.state(s, time, workday)
        val rate = if (monthly == 0L) BigDecimal.ZERO else s.monthlySalary.divide(monthly.toBigDecimal(), MathContext.DECIMAL128)
        // Divide once using total elapsed time, never sum rounded daily amounts.
        fun earned(seconds: Long) = if (monthly == 0L) BigDecimal.ZERO else
            s.monthlySalary.multiply(seconds.toBigDecimal()).divide(monthly.toBigDecimal(), MathContext.DECIMAL128)
        val total = previous * daily + worked
        val currentPayday = month.atDay(s.salaryDay.coerceAtMost(month.lengthOfMonth()))
        val payday = if (date <= currentPayday) currentPayday else month.plusMonths(1).let {
            it.atDay(s.salaryDay.coerceAtMost(it.lengthOfMonth()))
        }
        val countdownTarget = when (state) {
            WorkState.BEFORE_WORK -> s.workStart
            WorkState.LUNCH_BREAK -> s.lunchEnd
            WorkState.WORKING -> s.workEnd
            else -> time
        }
        return SalaryResult(now, state, days.size, previous + if (workday && worked == daily) 1 else 0,
            daily, monthly, worked, rate, earned(worked), earned(total),
            worked.toDouble() / daily, if (monthly == 0L) 0.0 else total.toDouble() / monthly,
            Duration.between(time, countdownTarget).seconds.coerceAtLeast(0), payday.atStartOfDay(),
            Duration.between(now, payday.atStartOfDay()).seconds.coerceAtLeast(0), date == currentPayday,
            calendar.warning(date.year))
    }
}
