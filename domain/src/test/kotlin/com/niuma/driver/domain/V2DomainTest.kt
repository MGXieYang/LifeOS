package com.niuma.driver.domain

import com.niuma.driver.domain.balance.*
import com.niuma.driver.domain.calendar.WorkCalendar
import com.niuma.driver.domain.decision.*
import com.niuma.driver.domain.freetime.FreeTimeCalculator
import com.niuma.driver.domain.lifetime.*
import com.niuma.driver.domain.salary.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.*

class V2DomainTest {
    @Test fun lunchCanBeDisabled() {
        val s = UserSettings(lunchBreakEnabled=false)
        assertEquals(34200, WorkTimeCalculator.dailySeconds(s))
        assertEquals(WorkState.WORKING, WorkTimeCalculator.state(s, LocalTime.NOON, true))
        assertEquals(9000, WorkTimeCalculator.workedSeconds(s, LocalTime.NOON))
    }

    @Test fun balanceSupportsTcoUsageAndLargeValues() {
        val r = LifeBalanceCalculator.calculate(LifeBalanceInput(
            price=BigDecimal("1200"), monthlySalary=BigDecimal("10000"),
            dailyWorkSeconds=30600, monthlyWorkSeconds=673200,
            expectedUsagePeriodMonths=12, usageFrequencyType=UsageFrequencyType.MONTHLY,
            usageFrequencyValue=BigDecimal("2"), oneTimeExtraCost=BigDecimal("100"),
            monthlyHoldingCost=BigDecimal("10"), annualHoldingCost=BigDecimal("120")))
        assertEquals(0, BigDecimal("1540").compareTo(r.totalCost))
        assertEquals(0, BigDecimal("24").compareTo(r.expectedUsageCount))
        assertTrue(r.costPerUse!! > BigDecimal("64"))
        assertTrue(LifeBalanceCalculator.calculate(LifeBalanceInput(BigDecimal("999999999999999999"), BigDecimal("1"), 1, 1)).workHours > BigDecimal.ZERO)
    }

    @Test fun freeTimeUsesOccupiedScheduleAndClamps() {
        val normal=FreeTimeCalculator.calculate(UserSettings(freeTimeEnabled=true))
        assertEquals(16200, normal.freeSeconds)
        val extreme=FreeTimeCalculator.calculate(UserSettings(freeTimeEnabled=true,sleepMinutes=1440,commuteMinutes=720,necessaryLifeMinutes=720))
        assertEquals(0, extreme.freeSeconds)
        assertTrue(extreme.isClamped)
    }

    @Test fun lifeNodesAnnualAndDecisionReview() {
        val today=LocalDate.of(2026,9,7)
        val calc=LifeTimeCalculator(WorkCalendar(emptyList()))
        val node=LifeNode(title="四十岁",type=LifeNodeType.AGE,targetAge=40,createdAt=LocalDateTime.now())
        assertEquals(LocalDate.of(2030,1,2),calc.node(node,LocalDate.of(1990,1,2),today).targetDate)
        assertTrue(calc.annual(today).calendarEstimated)
        val decision=Decision(title="换工作",confidence=75,status=DecisionStatus.DECIDED,
            createdAt=LocalDateTime.now(),decidedAt=LocalDateTime.now(),reviewDate=today.minusDays(1))
        assertEquals(DecisionStatus.REVIEW_DUE,decision.effectiveStatus(today))
        val review=DecisionReview(decisionId=1,resultRating=ResultRating.WORSE,wouldChooseAgain=ChooseAgain.NO,satisfactionScore=3,reviewedAt=LocalDateTime.now())
        assertEquals(1,DecisionStatisticsCalculator.calculate(listOf(decision),listOf(review)).regretCount)
    }
}
