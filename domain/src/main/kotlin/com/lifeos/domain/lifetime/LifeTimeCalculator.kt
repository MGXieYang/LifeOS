package com.lifeos.domain.lifetime

import com.lifeos.domain.calendar.WorkCalendar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period

enum class LifeNodeType { AGE, DATE }
data class LifeNode(
    val id: Long = 0,
    val title: String,
    val type: LifeNodeType,
    val targetAge: Int? = null,
    val targetDate: LocalDate? = null,
    val createdAt: java.time.LocalDateTime,
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
    val weeks: Long,
    val naturalWeekendDays: Long,
    val reached: Boolean,
)
data class AnnualBalance(
    val progress: Double,
    val elapsedDays: Int,
    val remainingNaturalDays: Int,
    val remainingWorkdays: Int,
    val remainingNonWorkdays: Int,
    val calendarEstimated: Boolean,
)
class LifeTimeCalculator(private val calendar: WorkCalendar) {
    fun node(node: LifeNode, birthDate: LocalDate?, today: LocalDate): LifeTimeResult {
        val target = when (node.type) {
            LifeNodeType.AGE -> requireNotNull(birthDate) { "年龄节点需要先设置出生日期" }.plusYears(node.targetAge!!.toLong())
            LifeNodeType.DATE -> node.targetDate!!
        }
        val reached = today >= target
        var weekends = 0L
        var cursor = today
        while (cursor < target) {
            if (cursor.dayOfWeek == DayOfWeek.SATURDAY || cursor.dayOfWeek == DayOfWeek.SUNDAY) weekends++
            cursor = cursor.plusDays(1)
        }
        val days = if (reached) 0 else java.time.temporal.ChronoUnit.DAYS.between(today, target)
        return LifeTimeResult(target, if (reached) Period.ZERO else Period.between(today, target), days, days / 7, weekends, reached)
    }
    fun annual(today: LocalDate): AnnualBalance {
        val first = today.withDayOfYear(1)
        val last = today.withDayOfYear(today.lengthOfYear())
        val elapsed = today.dayOfYear
        var work = 0
        var nonWork = 0
        var cursor = today.plusDays(1)
        while (!cursor.isAfter(last)) {
            if (calendar.isWorkday(cursor)) work++ else nonWork++
            cursor = cursor.plusDays(1)
        }
        return AnnualBalance(elapsed.toDouble() / today.lengthOfYear(), elapsed,
            today.lengthOfYear() - elapsed, work, nonWork, !calendar.hasYear(today.year))
    }
}
