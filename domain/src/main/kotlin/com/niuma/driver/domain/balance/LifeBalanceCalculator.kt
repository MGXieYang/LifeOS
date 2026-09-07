package com.niuma.driver.domain.balance

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class BalanceCategory { DIGITAL, TRAVEL, TRANSPORT, HOUSING, ENTERTAINMENT, LEARNING, LIFE, OTHER }
enum class BalanceStatus { CONSIDERING, PURCHASED, GAVE_UP }
enum class UsageFrequencyType(val periodsPerYear: BigDecimal) {
    DAILY(BigDecimal("365")), WEEKLY(BigDecimal("52")), MONTHLY(BigDecimal("12")), YEARLY(BigDecimal.ONE)
}
enum class LifeCostLevel { CASUAL, HALF_DAY, ONE_DAY, SMALL_BLEED, MONTHLY_HEAVY, LIFE_EQUIPMENT, MAJOR_DECISION }

data class BalanceItem(
    val id: Long = 0,
    val name: String,
    val price: BigDecimal,
    val category: BalanceCategory? = null,
    val remark: String = "",
    val status: BalanceStatus = BalanceStatus.CONSIDERING,
    val isPinned: Boolean = false,
    val expectedUsagePeriodMonths: Int? = null,
    val usageFrequencyType: UsageFrequencyType? = null,
    val usageFrequencyValue: BigDecimal? = null,
    val oneTimeExtraCost: BigDecimal = BigDecimal.ZERO,
    val monthlyHoldingCost: BigDecimal = BigDecimal.ZERO,
    val annualHoldingCost: BigDecimal = BigDecimal.ZERO,
    val createdAt: java.time.LocalDateTime,
    val updatedAt: java.time.LocalDateTime,
    val purchasedAt: java.time.LocalDateTime? = null,
) {
    init {
        require(name.isNotBlank()) { "请输入项目名称" }
        listOf(price, oneTimeExtraCost, monthlyHoldingCost, annualHoldingCost).forEach {
            require(it >= BigDecimal.ZERO && it.scale() <= 2) { "金额须为非负数，最多两位小数" }
        }
        require(expectedUsagePeriodMonths == null || expectedUsagePeriodMonths > 0) { "预计使用月数必须大于 0" }
        require(usageFrequencyValue == null || usageFrequencyValue > BigDecimal.ZERO) { "使用频率必须大于 0" }
    }
}

data class LifeBalanceInput(
    val price: BigDecimal,
    val monthlySalary: BigDecimal,
    val dailyWorkSeconds: Long,
    val monthlyWorkSeconds: Long,
    val expectedUsagePeriodMonths: Int? = null,
    val usageFrequencyType: UsageFrequencyType? = null,
    val usageFrequencyValue: BigDecimal? = null,
    val oneTimeExtraCost: BigDecimal = BigDecimal.ZERO,
    val monthlyHoldingCost: BigDecimal = BigDecimal.ZERO,
    val annualHoldingCost: BigDecimal = BigDecimal.ZERO,
    val dailyFreeSeconds: Long? = null,
)

data class LifeBalanceResult(
    val totalCost: BigDecimal,
    val workHours: BigDecimal,
    val workDays: BigDecimal,
    val workMonths: BigDecimal,
    val salaryRatio: BigDecimal,
    val level: LifeCostLevel,
    val expectedUsageCount: BigDecimal?,
    val costPerUse: BigDecimal?,
    val equivalentFreeEvenings: BigDecimal?,
)

object LifeCostLevelCalculator {
    fun calculate(workHours: BigDecimal, workDays: BigDecimal, workMonths: BigDecimal): LifeCostLevel = when {
        workHours <= BigDecimal("0.5") -> LifeCostLevel.CASUAL
        workDays <= BigDecimal("0.5") -> LifeCostLevel.HALF_DAY
        workDays <= BigDecimal.ONE -> LifeCostLevel.ONE_DAY
        workDays <= BigDecimal("5") -> LifeCostLevel.SMALL_BLEED
        workMonths <= BigDecimal.ONE -> LifeCostLevel.MONTHLY_HEAVY
        workMonths <= BigDecimal("6") -> LifeCostLevel.LIFE_EQUIPMENT
        else -> LifeCostLevel.MAJOR_DECISION
    }
}

object LifeBalanceCalculator {
    fun calculate(input: LifeBalanceInput): LifeBalanceResult {
        require(input.price >= BigDecimal.ZERO) { "价格不能为负数" }
        require(input.monthlySalary > BigDecimal.ZERO && input.dailyWorkSeconds > 0 && input.monthlyWorkSeconds > 0) {
            "工资和有效工作时间必须大于 0"
        }
        val months = input.expectedUsagePeriodMonths ?: 0
        val years = BigDecimal(months).divide(BigDecimal(12), MathContext.DECIMAL128)
        val total = input.price + input.oneTimeExtraCost +
            input.monthlyHoldingCost * BigDecimal(months) + input.annualHoldingCost * years
        val salaryRatio = total.divide(input.monthlySalary, MathContext.DECIMAL128)
        val seconds = salaryRatio * BigDecimal(input.monthlyWorkSeconds)
        val hours = seconds.divide(BigDecimal(3600), MathContext.DECIMAL128)
        val days = seconds.divide(BigDecimal(input.dailyWorkSeconds), MathContext.DECIMAL128)
        val usageCount = if (months > 0 && input.usageFrequencyType != null && input.usageFrequencyValue != null) {
            input.usageFrequencyType.periodsPerYear * input.usageFrequencyValue * years
        } else null
        val costPerUse = usageCount?.takeIf { it > BigDecimal.ZERO }?.let { total.divide(it, MathContext.DECIMAL128) }
        val freeEvenings = input.dailyFreeSeconds?.takeIf { it > 0 }?.let {
            seconds.divide(BigDecimal(it), MathContext.DECIMAL128)
        }
        return LifeBalanceResult(total, hours, days, salaryRatio, salaryRatio,
            LifeCostLevelCalculator.calculate(hours, days, salaryRatio), usageCount, costPerUse, freeEvenings)
    }
}
