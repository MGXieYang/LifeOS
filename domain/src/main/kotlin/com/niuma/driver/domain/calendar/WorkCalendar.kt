package com.niuma.driver.domain.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class WorkDayType { WORKDAY, HOLIDAY }
data class CalendarYear(
    val year: Int,
    val version: String,
    val lastUpdated: String,
    val days: Map<LocalDate, WorkDayType>,
) {
    init { require(days.keys.all { it.year == year }) }
}
interface CalendarRepository { fun years(): List<CalendarYear> }
class WorkCalendar(years: List<CalendarYear>) {
    private val entries = years.associateBy { it.year }
    val coveredYears get() = entries.keys.sorted()
    fun hasYear(year: Int) = year in entries
    fun metadata(year: Int): CalendarYear? = entries[year]
    fun isWorkday(date: LocalDate): Boolean = when (entries[date.year]?.days?.get(date)) {
        WorkDayType.WORKDAY -> true
        WorkDayType.HOLIDAY -> false
        null -> date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
    }
    fun workdays(month: YearMonth): List<LocalDate> = (1..month.lengthOfMonth())
        .map(month::atDay).filter(::isWorkday)
    fun warning(year: Int): String? = if (hasYear(year)) null else
        "⚠ $year 年法定调休数据尚未收录，当前按照普通工作周估算。"
}
