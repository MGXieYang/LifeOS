package com.lifeos.domain

import com.lifeos.domain.calendar.*
import com.lifeos.domain.salary.*
import com.lifeos.domain.wish.*
import com.lifeos.domain.retirement.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.*

class DomainTest {
    private val calendar = WorkCalendar(listOf(CalendarYear(2026, "2026.1", "2025-11-04", mapOf(
        LocalDate.parse("2026-09-20") to WorkDayType.WORKDAY,
        LocalDate.parse("2026-09-25") to WorkDayType.HOLIDAY,
        LocalDate.parse("2026-02-14") to WorkDayType.WORKDAY,
        LocalDate.parse("2026-01-01") to WorkDayType.HOLIDAY,
    ))))
    private val settings = UserSettings()
    private val calculator = SalaryCalculator(calendar)
    private fun at(time: String, date: String = "2026-09-07") = calculator.calculate(settings, LocalDateTime.parse("${date}T$time"))
    @Test fun acceptanceExamplesAndStateBoundaries() {
        val cases = listOf(
            Triple("09:29:59", "0.00", WorkState.BEFORE_WORK),
            Triple("09:30:00", "0.00", WorkState.WORKING_MORNING),
            Triple("10:30:00", "56.82", WorkState.WORKING_MORNING),
            Triple("12:30:00", "170.45", WorkState.LUNCH_BREAK),
            Triple("13:30:00", "170.45", WorkState.LUNCH_BREAK),
            Triple("14:00:00", "170.45", WorkState.WORKING_AFTERNOON),
            Triple("15:00:00", "227.27", WorkState.WORKING_AFTERNOON),
            Triple("19:00:00", "454.55", WorkState.AFTER_WORK),
            Triple("23:59:59", "454.55", WorkState.AFTER_WORK))
        cases.forEach { (time, amount, state) ->
            assertEquals(time, amount, at(time).todayEarned.money())
            assertEquals(state, at(time).state)
        }
        assertEquals(28800L, at("19:00:00").todayWorkedSeconds)
        assertEquals(22, at("10:00:00").monthlyWorkDays)
    }
    @Test fun calendarWeekdayHolidayAndWeekendOverride() {
        assertTrue(calendar.isWorkday(LocalDate.parse("2026-09-07")))
        assertFalse(calendar.isWorkday(LocalDate.parse("2026-09-05")))
        assertFalse(calendar.isWorkday(LocalDate.parse("2026-01-01")))
        assertTrue(calendar.isWorkday(LocalDate.parse("2026-02-14")))
        assertTrue(calendar.isWorkday(LocalDate.parse("2026-09-20")))
        assertNotNull(calendar.warning(2028))
        assertNull(calendar.warning(2026))
    }
    @Test fun holidayIncomeAndProgressStayZero() {
        for (date in listOf("2026-09-05", "2026-09-25")) {
            val result = at("16:00:00", date)
            assertEquals(WorkState.HOLIDAY, result.state)
            assertEquals("0.00", result.todayEarned.money())
            assertEquals(0.0, result.todayProgress, 0.0)
        }
    }
    @Test fun monthlyTotalIsExactlySalaryAndLunchFreezes() {
        val result = at("23:59:59", "2026-09-30")
        assertEquals(0, BigDecimal("10000").compareTo(result.monthEarned))
        assertEquals(22, result.completedWorkDays)
        assertEquals(1.0, result.monthProgress, 0.0)
        assertEquals(at("12:30:00").monthEarned, at("13:59:59").monthEarned)
        assertEquals(at("12:30:00").todayProgress, at("13:59:59").todayProgress, 0.0)
        assertEquals(0L, at("09:00:00", "2026-09-01").todayWorkedSeconds)
        assertEquals("0.00", at("09:00:00", "2026-09-01").monthEarned.money())
    }
    @Test fun paydayClampsAndKeepsTodayVisible() {
        val s = settings.copy(salaryDay = 31)
        val feb = calculator.calculate(s, LocalDateTime.parse("2028-02-28T12:00:00"))
        assertEquals(LocalDate.of(2028,2,29), feb.payday.toLocalDate())
        assertEquals(43200L, feb.paydaySeconds)
        assertTrue(calculator.calculate(s, LocalDateTime.parse("2027-02-28T23:59:59")).isPayday)
        assertTrue(at("23:00:00").isPayday)
        assertEquals(LocalDate.of(2026,10,7), at("01:00:00", "2026-09-08").payday.toLocalDate())
        assertEquals(LocalDate.of(2027,1,7), at("12:00:00", "2026-12-08").payday.toLocalDate())
    }
    @Test fun clockAndSettingsChangesRecalculateWithoutAccumulation() {
        val instant = Instant.parse("2026-09-07T02:30:00Z")
        val china = calculator.calculate(settings, Clock.fixed(instant, ZoneId.of("Asia/Shanghai")))
        val utc = calculator.calculate(settings, Clock.fixed(instant, ZoneOffset.UTC))
        assertEquals("56.82", china.todayEarned.money())
        assertEquals("0.00", utc.todayEarned.money())
        val twice = calculator.calculate(settings.copy(monthlySalary=BigDecimal("20000")), china.now)
        assertTrue((twice.todayEarned - china.todayEarned * BigDecimal(2)).abs() < BigDecimal("1E-28"))
        assertEquals("113.64", twice.todayEarned.money())
        assertEquals(china, calculator.calculate(settings, china.now))
    }
    @Test fun worktimeModificationRecalculates() {
        val changed = settings.copy(workEnd=LocalTime.of(18,30))
        val result = calculator.calculate(changed, LocalDateTime.parse("2026-09-07T19:00:00"))
        assertEquals(27000L, result.dailyWorkSeconds)
        assertEquals("454.55", result.todayEarned.money())
    }
    @Test fun allWorkAndLunchTimesAreCustomizable() {
        val changed = settings.copy(
            workStart=LocalTime.of(8,0),
            lunchStart=LocalTime.of(11,45),
            lunchEnd=LocalTime.of(13,15),
            workEnd=LocalTime.of(17,30),
        )
        val beforeLunch = calculator.calculate(changed, LocalDateTime.parse("2026-09-07T11:45:00"))
        val duringLunch = calculator.calculate(changed, LocalDateTime.parse("2026-09-07T12:30:00"))
        val afterLunch = calculator.calculate(changed, LocalDateTime.parse("2026-09-07T14:15:00"))
        assertEquals(28800L, beforeLunch.dailyWorkSeconds)
        assertEquals(WorkState.LUNCH_BREAK, duringLunch.state)
        assertEquals(beforeLunch.todayWorkedSeconds, duringLunch.todayWorkedSeconds)
        assertEquals(WorkState.WORKING_AFTERNOON, afterLunch.state)
        assertEquals(beforeLunch.todayWorkedSeconds + 3600, afterLunch.todayWorkedSeconds)
    }
    @Test fun wishZeroNormalAndHugeAmounts() {
        val salary = at("10:30:00")
        assertEquals(0, WishCalculator.calculate(BigDecimal.ZERO,salary).seconds.compareTo(BigDecimal.ZERO))
        val result = WishCalculator.calculate(BigDecimal("8999"), salary)
        assertEquals(0.8999, result.months.toDouble(), 0.0000001)
        assertEquals(19.7978, result.days.toDouble(), 0.0000001)
        assertTrue(WishCalculator.calculate(BigDecimal("999999999999999999999999"),salary).hours > BigDecimal.ZERO)
    }
    @Test fun retirementOfficialTableBoundariesAndCategories() {
        val calc = RetirementCalculator(calendar)
        val today = LocalDate.of(2024,1,1)
        val cases = listOf(
            Triple("1964-12-15",RetirementType.MALE,"2024-12-15"),
            Triple("1965-01-15",RetirementType.MALE,"2025-02-15"),
            Triple("1965-04-15",RetirementType.MALE,"2025-05-15"),
            Triple("1965-05-15",RetirementType.MALE,"2025-07-15"),
            Triple("1976-09-15",RetirementType.MALE,"2039-09-15"),
            Triple("1995-06-18",RetirementType.MALE,"2058-06-18"),
            Triple("1970-01-15",RetirementType.FEMALE_55,"2025-02-15"),
            Triple("1995-06-18",RetirementType.FEMALE_55,"2053-06-18"),
            Triple("1975-01-15",RetirementType.FEMALE_50,"2025-02-15"),
            Triple("1975-03-15",RetirementType.FEMALE_50,"2025-05-15"),
            Triple("1995-06-18",RetirementType.FEMALE_50,"2050-06-18"),
            Triple("1975-01-31",RetirementType.FEMALE_50,"2025-02-28"))
        cases.forEach { (birth,type,expected) -> assertEquals(LocalDate.parse(expected),calc.calculate(LocalDate.parse(birth),type,today).date) }
        val retired=calc.calculate(LocalDate.of(1960,1,1),RetirementType.MALE,LocalDate.of(2026,1,1))
        assertTrue(retired.reached)
        assertEquals(Period.ZERO,retired.remaining)
        assertEquals(0L,retired.estimatedWorkdays)
    }
    @Test(expected=IllegalArgumentException::class) fun invalidSalaryRejected() { settings.copy(monthlySalary=BigDecimal.ZERO) }
    @Test(expected=IllegalArgumentException::class) fun invertedLunchRejected() { settings.copy(lunchEnd=LocalTime.of(12,0)) }
    @Test(expected=IllegalArgumentException::class) fun overlappingWorkRejected() { settings.copy(workStart=settings.lunchStart) }
    @Test(expected=IllegalArgumentException::class) fun invalidPaydayRejected() { settings.copy(salaryDay=32) }
    @Test(expected=IllegalArgumentException::class) fun negativeWishRejected() { WishCalculator.calculate(BigDecimal("-1"),at("10:00:00")) }
    @Test(expected=IllegalArgumentException::class) fun futureBirthRejected() { RetirementCalculator(calendar).calculate(LocalDate.of(2030,1,1),RetirementType.MALE,LocalDate.of(2026,1,1)) }
}

