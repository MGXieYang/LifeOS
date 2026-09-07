package com.lifeos.data

import com.lifeos.domain.UserSettings
import com.lifeos.domain.calendar.CalendarYear
import com.lifeos.domain.calendar.WorkCalendar
import com.lifeos.domain.calendar.WorkDayType
import com.lifeos.domain.money
import com.lifeos.domain.salary.SalaryCalculator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.math.BigDecimal

class CalendarDataTest {
    private fun load(year: Int): CalendarYear {
        val json = JSONObject(File("src/main/assets/calendar/$year.json").readText())
        val days = json.getJSONObject("days")
        return CalendarYear(
            year,
            json.getString("calendarVersion"),
            json.getString("calendarLastUpdated"),
            days.keys().asSequence().associate { LocalDate.parse(it) to WorkDayType.valueOf(days.getString(it)) },
        )
    }

    @Test fun bundledCalendarMatchesOfficialExamples() {
        val calendar = WorkCalendar(listOf(load(2025), load(2026)))
        assertEquals(22, calendar.workdays(YearMonth.of(2026, 9)).size)
        listOf(
            "2025-01-26", "2025-02-08", "2025-04-27", "2025-09-28", "2025-10-11",
            "2026-01-04", "2026-02-14", "2026-02-28", "2026-05-09", "2026-09-20", "2026-10-10",
        ).forEach { assertTrue(it, calendar.isWorkday(LocalDate.parse(it))) }
        listOf(
            "2025-01-28", "2025-02-04", "2025-06-02", "2025-10-08",
            "2026-01-02", "2026-02-23", "2026-09-25", "2026-10-07",
        ).forEach { assertFalse(it, calendar.isWorkday(LocalDate.parse(it))) }
        for (year in 2025..2026) for (month in 1..12) {
            val last = YearMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59)
            assertEquals("10000.00", SalaryCalculator(calendar).calculate(UserSettings(), last).monthEarned.money())
        }
    }

    @Test fun missingYearFallsBackWithAnExplicitWarning() {
        val calendar = WorkCalendar(listOf(load(2026)))
        assertTrue(calendar.isWorkday(LocalDate.of(2027, 9, 6)))
        assertFalse(calendar.isWorkday(LocalDate.of(2027, 9, 5)))
        assertTrue(calendar.warning(2027)!!.contains("估算"))
    }

    @Test fun legacyYearlyBalanceFrequencyIsReadWithoutDataLoss() {
        val item = BalanceHistoryEntity(1, "旧记录", "1200", 12, "YEARLY", "12", "2026-09-07T12:00").domain()
        assertEquals("MONTHLY", item.usageFrequencyType!!.name)
        assertEquals(0, BigDecimal.ONE.compareTo(item.usageFrequencyValue))
    }
}
