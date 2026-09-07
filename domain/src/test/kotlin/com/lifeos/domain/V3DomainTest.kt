package com.lifeos.domain

import com.lifeos.domain.balance.LifeBalanceCalculator
import com.lifeos.domain.balance.LifeBalanceInput
import com.lifeos.domain.balance.PurchaseWeight
import com.lifeos.domain.balance.UsageFrequencyType
import com.lifeos.domain.balance.UsageValueCalculator
import com.lifeos.domain.balance.UsageValueInput
import com.lifeos.domain.calendar.CalendarYear
import com.lifeos.domain.calendar.WorkCalendar
import com.lifeos.domain.calendar.WorkDayType
import com.lifeos.domain.lifetime.FutureTimeBudgetCalculator
import com.lifeos.domain.lifetime.LifeNode
import com.lifeos.domain.lifetime.LifeNodeType
import com.lifeos.domain.lifetime.LifeTimeCalculator
import com.lifeos.domain.salary.WorkState
import com.lifeos.domain.salary.WorkTimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class V3DomainTest {
    private val calendar = WorkCalendar(
        listOf(
            CalendarYear(
                2026,
                "test",
                "2026-01-01",
                mapOf(
                    LocalDate.parse("2026-09-12") to WorkDayType.WORKDAY,
                    LocalDate.parse("2026-09-14") to WorkDayType.HOLIDAY,
                    LocalDate.parse("2026-09-19") to WorkDayType.HOLIDAY,
                    LocalDate.parse("2026-09-20") to WorkDayType.HOLIDAY,
                ),
            ),
        ),
    )

    @Test fun lunchCanBeDisabledWithoutChangingTheProvenWorktimeModel() {
        val settings = UserSettings(lunchBreakEnabled = false)
        assertEquals(34_200, WorkTimeCalculator.dailySeconds(settings))
        assertEquals(WorkState.WORKING, WorkTimeCalculator.state(settings, LocalTime.NOON, true))
        assertEquals(9_000, WorkTimeCalculator.workedSeconds(settings, LocalTime.NOON))
    }

    @Test fun balanceConvertsZeroNormalAndHugeAmounts() {
        fun calculate(price: String) = LifeBalanceCalculator.calculate(
            LifeBalanceInput(BigDecimal(price), BigDecimal("10000"), 28_800, 633_600),
        )
        assertEquals(0, BigDecimal.ZERO.compareTo(calculate("0").workTimeCost.hours))
        val normal = calculate("8999")
        assertEquals(0, BigDecimal("0.8999").compareTo(normal.workTimeCost.salaryRatio))
        assertEquals(0, BigDecimal("19.7978").compareTo(normal.workTimeCost.days))
        assertEquals(PurchaseWeight.HEAVY, normal.weight)
        assertTrue(calculate("999999999999999999").workTimeCost.hours > BigDecimal.ZERO)
    }

    @Test fun purchaseWeightPolicyCoversAllPressureLevels() {
        val expected = listOf(
            "500" to PurchaseWeight.LIGHT,
            "2000" to PurchaseWeight.SLIGHTLY_LIGHT,
            "4000" to PurchaseWeight.NORMAL,
            "8000" to PurchaseWeight.HEAVY,
            "12000" to PurchaseWeight.VERY_HEAVY,
        )
        expected.forEach { (price, level) ->
            assertEquals(level, LifeBalanceCalculator.calculate(LifeBalanceInput(BigDecimal(price), BigDecimal("10000"), 28_800, 633_600)).weight)
        }
    }

    @Test fun usageValueSupportsDailyWeeklyMonthlyAndDifferentDurations() {
        val daily = UsageValueCalculator.calculate(UsageValueInput(BigDecimal("365"), 12, UsageFrequencyType.DAILY, BigDecimal.ONE))
        assertEquals(0, BigDecimal("365").compareTo(daily.estimatedUsageCount))
        assertEquals(0, BigDecimal.ONE.compareTo(daily.costPerUse))
        val weekly = UsageValueCalculator.calculate(UsageValueInput(BigDecimal("520"), 24, UsageFrequencyType.WEEKLY, BigDecimal("2")))
        assertEquals(0, BigDecimal("208").compareTo(weekly.estimatedUsageCount))
        val monthly = UsageValueCalculator.calculate(UsageValueInput(BigDecimal("1200"), 12, UsageFrequencyType.MONTHLY, BigDecimal("3")))
        assertEquals(0, BigDecimal("36").compareTo(monthly.estimatedUsageCount))
        assertEquals(0, BigDecimal("100").compareTo(monthly.costPerMonth))
    }

    @Test(expected = IllegalArgumentException::class)
    fun usageValueRejectsInvalidDuration() {
        UsageValueCalculator.calculate(UsageValueInput(BigDecimal.TEN, 0, UsageFrequencyType.DAILY, BigDecimal.ONE))
    }

    @Test fun lifetimeCalculatesAnnualAgeAndNaturalWeekends() {
        val calculator = LifeTimeCalculator(calendar)
        val today = LocalDate.of(2026, 9, 7)
        val annual = calculator.annual(today)
        assertEquals(115, annual.remainingNaturalDays)
        assertEquals(3, annual.remainingLegalHolidays)
        assertEquals(31, calculator.age(LocalDate.of(1995, 7, 7), today).years)
        val node = LifeNode(title = "四十岁", type = LifeNodeType.AGE, targetAge = 40, createdAt = LocalDateTime.of(2026, 1, 1, 0, 0))
        val result = calculator.node(node, LocalDate.of(1990, 1, 2), today)
        assertEquals(LocalDate.of(2030, 1, 2), result.targetDate)
        assertTrue(result.naturalWeekendCount > 0)
    }

    @Test fun futureBudgetCountsLegalHolidayAdjustedWorkAndFullWeekends() {
        val result = FutureTimeBudgetCalculator(calendar).calculate(LocalDate.of(2026, 9, 11), 10)
        assertEquals(10, result.workdays + result.nonWorkdays)
        assertEquals(3, result.legalHolidays)
        assertEquals(1, result.fullWeekends)
        assertTrue(!result.calendarEstimated)
    }

    @Test fun futureBudgetMarksMissingYearAsEstimated() {
        assertTrue(FutureTimeBudgetCalculator(calendar).calculate(LocalDate.of(2026, 12, 1), 90).calendarEstimated)
    }
}

