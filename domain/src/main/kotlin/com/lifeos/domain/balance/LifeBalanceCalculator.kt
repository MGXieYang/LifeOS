package com.lifeos.domain.balance

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDateTime

enum class PurchaseWeight { LIGHT, SLIGHTLY_LIGHT, NORMAL, HEAVY, VERY_HEAVY }

enum class UsageFrequencyType(val periodsPerYear: BigDecimal) {
    DAILY(BigDecimal("365")),
    WEEKLY(BigDecimal("52")),
    MONTHLY(BigDecimal("12")),
}

data class BalanceItem(
    val id: Long = 0,
    val name: String = "",
    val price: BigDecimal,
    val expectedUsagePeriodMonths: Int? = null,
    val usageFrequencyType: UsageFrequencyType? = null,
    val usageFrequencyValue: BigDecimal? = null,
    val createdAt: LocalDateTime,
) {
    init {
        require(price >= BigDecimal.ZERO && price.scale() <= 2) { "价格须为非负金额，最多两位小数" }
        require(expectedUsagePeriodMonths == null || expectedUsagePeriodMonths > 0) { "预计使用月数必须大于 0" }
        require(usageFrequencyValue == null || usageFrequencyValue > BigDecimal.ZERO) { "使用频率必须大于 0" }
    }
}

data class LifeBalanceInput(
    val price: BigDecimal,
    val monthlySalary: BigDecimal,
    val dailyWorkSeconds: Long,
    val monthlyWorkSeconds: Long,
)

data class WorkTimeCost(
    val hours: BigDecimal,
    val days: BigDecimal,
    val salaryRatio: BigDecimal,
)

data class LifeBalanceResult(
    val workTimeCost: WorkTimeCost,
    val weight: PurchaseWeight,
)

object PurchaseWeightPolicy {
    fun evaluate(price: BigDecimal, monthlySalary: BigDecimal, workTimeCost: WorkTimeCost): PurchaseWeight {
        require(price >= BigDecimal.ZERO) { "价格不能为负数" }
        require(monthlySalary > BigDecimal.ZERO) { "月工资必须大于 0" }
        val ratio = price.divide(monthlySalary, MathContext.DECIMAL128)
        return when {
            ratio <= BigDecimal("0.10") && workTimeCost.days <= BigDecimal("2.2") -> PurchaseWeight.LIGHT
            ratio <= BigDecimal("0.25") -> PurchaseWeight.SLIGHTLY_LIGHT
            ratio <= BigDecimal("0.50") -> PurchaseWeight.NORMAL
            ratio <= BigDecimal.ONE -> PurchaseWeight.HEAVY
            else -> PurchaseWeight.VERY_HEAVY
        }
    }
}

object LifeBalanceCalculator {
    fun calculate(input: LifeBalanceInput): LifeBalanceResult {
        require(input.price >= BigDecimal.ZERO) { "价格不能为负数" }
        require(input.monthlySalary > BigDecimal.ZERO && input.dailyWorkSeconds > 0 && input.monthlyWorkSeconds > 0) {
            "工资和有效工作时间必须大于 0"
        }
        val ratio = input.price.divide(input.monthlySalary, MathContext.DECIMAL128)
        val seconds = ratio.multiply(input.monthlyWorkSeconds.toBigDecimal())
        val cost = WorkTimeCost(
            hours = seconds.divide(BigDecimal("3600"), MathContext.DECIMAL128),
            days = seconds.divide(input.dailyWorkSeconds.toBigDecimal(), MathContext.DECIMAL128),
            salaryRatio = ratio,
        )
        return LifeBalanceResult(cost, PurchaseWeightPolicy.evaluate(input.price, input.monthlySalary, cost))
    }
}

data class UsageValueInput(
    val price: BigDecimal,
    val usageDurationMonths: Int,
    val frequencyType: UsageFrequencyType,
    val frequencyValue: BigDecimal,
)

data class UsageValueResult(
    val estimatedUsageCount: BigDecimal,
    val costPerUse: BigDecimal,
    val costPerDay: BigDecimal,
    val costPerMonth: BigDecimal,
)

object UsageValueCalculator {
    fun calculate(input: UsageValueInput): UsageValueResult {
        require(input.price >= BigDecimal.ZERO) { "价格不能为负数" }
        require(input.usageDurationMonths > 0) { "预计使用月数必须大于 0" }
        require(input.frequencyValue > BigDecimal.ZERO) { "使用频率必须大于 0" }
        val months = input.usageDurationMonths.toBigDecimal()
        val years = months.divide(BigDecimal("12"), MathContext.DECIMAL128)
        val days = years.multiply(BigDecimal("365"))
        val count = input.frequencyType.periodsPerYear.multiply(input.frequencyValue).multiply(years)
        return UsageValueResult(
            estimatedUsageCount = count,
            costPerUse = input.price.divide(count, MathContext.DECIMAL128),
            costPerDay = input.price.divide(days, MathContext.DECIMAL128),
            costPerMonth = input.price.divide(months, MathContext.DECIMAL128),
        )
    }
}

