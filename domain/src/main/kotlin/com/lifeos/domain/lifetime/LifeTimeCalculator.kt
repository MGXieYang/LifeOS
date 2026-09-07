package com.lifeos.domain.lifetime

import com.lifeos.domain.calendar.WorkCalendar
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.ChronoUnit

enum class LifeNodeType { AGE, DATE }

data class LifeNode(
    val id: Long = 0,
    val title: String,
    val type: LifeNodeType,
    val targetAge: Int? = null,
    val targetDate: LocalDate? = null,
    val createdAt: LocalDateTime,
) {
    init {
        require(title.isNotBlank()) { "请输入节点名称" }
        when (type) {
            LifeNodeType.AGE -> require(targetAge != null && targetAge in 0..150) { "年龄节点应在 0～150 岁之间" }
            LifeNodeType.DATE -> require(targetDate != null) { "请选择目标日期" }
        }
    }
}

data class LifeTimeResult(
    val targetDate: LocalDate,
    val remaining: Period,
    val totalDays: Long,
    val naturalWeekendCount: Long,
    val reached: Boolean,
)

data class AnnualBalance(
    val progress: Double,
    val elapsedDays: Int,
    val remainingNaturalDays: Int,
    val remainingWorkdays: Int,
    val remainingNonWorkdays: Int,
    val remainingLegalHolidays: Int,
    val calendarEstimated: Boolean,
)

data class CurrentAge(val years: Int, val months: Int, val days: Int)

class LifeTimeCalculator(private val calendar: WorkCalendar) {
    fun node(node: LifeNode, birthDate: LocalDate?, clock: Clock): LifeTimeResult =
        node(node, birthDate, LocalDate.now(clock))

    fun node(node: LifeNode, birthDate: LocalDate?, today: LocalDate): LifeTimeResult {
        val target = when (node.type) {
            LifeNodeType.AGE -> requireNotNull(birthDate) { "年龄节点需要先设置出生日期" }
                .plusYears(node.targetAge!!.toLong())
            LifeNodeType.DATE -> node.targetDate!!
        }
        val reached = today >= target
        val days = if (reached) 0 else ChronoUnit.DAYS.between(today, target)
        var fullNaturalWeekends = 0L
        var cursor = today
        while (!reached && cursor.plusDays(1).isBefore(target.plusDays(1))) {
            if (cursor.dayOfWeek == DayOfWeek.SATURDAY && cursor.plusDays(1).isBefore(target)) fullNaturalWeekends++
            cursor = cursor.plusDays(1)
        }
        return LifeTimeResult(
            targetDate = target,
            remaining = if (reached) Period.ZERO else Period.between(today, target),
            totalDays = days,
            naturalWeekendCount = fullNaturalWeekends,
            reached = reached,
        )
    }

    fun age(birthDate: LocalDate, clock: Clock): CurrentAge = age(birthDate, LocalDate.now(clock))

    fun age(birthDate: LocalDate, today: LocalDate): CurrentAge {
        require(birthDate <= today) { "出生日期不能晚于今天" }
        val period = Period.between(birthDate, today)
        return CurrentAge(period.years, period.months, period.days)
    }

    fun annual(clock: Clock): AnnualBalance = annual(LocalDate.now(clock))

    fun annual(today: LocalDate): AnnualBalance {
        val last = today.withDayOfYear(today.lengthOfYear())
        var work = 0
        var nonWork = 0
        var legalHolidays = 0
        var cursor = today.plusDays(1)
        while (!cursor.isAfter(last)) {
            if (calendar.isWorkday(cursor)) work++ else nonWork++
            if (calendar.isExplicitHoliday(cursor)) legalHolidays++
            cursor = cursor.plusDays(1)
        }
        val elapsed = today.dayOfYear
        return AnnualBalance(
            progress = elapsed.toDouble() / today.lengthOfYear(),
            elapsedDays = elapsed,
            remainingNaturalDays = today.lengthOfYear() - elapsed,
            remainingWorkdays = work,
            remainingNonWorkdays = nonWork,
            remainingLegalHolidays = legalHolidays,
            calendarEstimated = !calendar.hasYear(today.year),
        )
    }
}

data class FutureTimeBudget(
    val windowDays: Int,
    val workdays: Int,
    val nonWorkdays: Int,
    val legalHolidays: Int,
    val fullWeekends: Int,
    val calendarEstimated: Boolean,
)

class FutureTimeBudgetCalculator(private val calendar: WorkCalendar) {
    fun calculate(windowDays: Int, clock: Clock): FutureTimeBudget =
        calculate(LocalDate.now(clock), windowDays)

    fun calculate(today: LocalDate, windowDays: Int): FutureTimeBudget {
        require(windowDays > 0) { "未来时间窗口必须大于 0 天" }
        val start = today.plusDays(1)
        val endInclusive = today.plusDays(windowDays.toLong())
        var workdays = 0
        var nonWorkdays = 0
        var legalHolidays = 0
        var fullWeekends = 0
        var date = start
        while (!date.isAfter(endInclusive)) {
            if (calendar.isWorkday(date)) workdays++ else nonWorkdays++
            if (calendar.isExplicitHoliday(date)) legalHolidays++
            if (
                date.dayOfWeek == DayOfWeek.SATURDAY &&
                !date.plusDays(1).isAfter(endInclusive) &&
                !calendar.isWorkday(date) &&
                !calendar.isWorkday(date.plusDays(1))
            ) fullWeekends++
            date = date.plusDays(1)
        }
        val estimated = generateSequence(start) { it.plusDays(1) }
            .take(windowDays)
            .map(LocalDate::getYear)
            .distinct()
            .any { !calendar.hasYear(it) }
        return FutureTimeBudget(windowDays, workdays, nonWorkdays, legalHolidays, fullWeekends, estimated)
    }
}

