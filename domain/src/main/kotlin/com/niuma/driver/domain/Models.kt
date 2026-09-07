package com.niuma.driver.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class RetirementType { MALE, FEMALE_55, FEMALE_50 }
data class UserSettings(
    val monthlySalary: BigDecimal = BigDecimal("10000.00"),
    val workStart: LocalTime = LocalTime.of(9, 30),
    val lunchStart: LocalTime = LocalTime.of(12, 30),
    val lunchEnd: LocalTime = LocalTime.of(14, 0),
    val workEnd: LocalTime = LocalTime.of(19, 0),
    val lunchBreakEnabled: Boolean = true,
    val salaryDay: Int = 7,
    val birthDate: LocalDate? = null,
    val retirementType: RetirementType = RetirementType.MALE,
    val freeTimeEnabled: Boolean = false,
    val sleepMinutes: Int = 8 * 60,
    val commuteMinutes: Int = 0,
    val necessaryLifeEnabled: Boolean = true,
    val necessaryLifeMinutes: Int = 2 * 60,
    val animationsEnabled: Boolean = true,
    val funModeEnabled: Boolean = true,
    val currencySymbol: String = "¥",
    val moneyDecimals: Int = 2,
    val onboarded: Boolean = false,
) {
    init {
        require(monthlySalary > BigDecimal.ZERO) { "税后月薪必须大于 0" }
        require(monthlySalary.scale() <= 2) { "工资最多保留两位小数" }
        require(workStart < workEnd) { "上班时间必须早于下班时间（不支持跨夜班）" }
        if (lunchBreakEnabled) require(workStart < lunchStart && lunchStart < lunchEnd && lunchEnd < workEnd) {
            "时间需满足：上班 < 午休开始 < 午休结束 < 下班"
        }
        require(salaryDay in 1..31) { "发薪日应在 1～31 之间" }
        require(sleepMinutes in 0..24 * 60) { "平均睡眠时间应在 0～24 小时之间" }
        require(commuteMinutes in 0..12 * 60) { "每日往返通勤应在 0～12 小时之间" }
        require(necessaryLifeMinutes in 0..12 * 60) { "必要生活时间应在 0～12 小时之间" }
        require(currencySymbol.isNotBlank()) { "货币符号不能为空" }
        require(moneyDecimals in 0..4) { "金额小数位应在 0～4 之间" }
    }
}
enum class WishStatus { WISHING, ACHIEVED }
data class WishItem(
    val id: Long = 0,
    val name: String,
    val price: BigDecimal,
    val remark: String = "",
    val status: WishStatus = WishStatus.WISHING,
    val createdAt: LocalDateTime,
    val achievedAt: LocalDateTime? = null,
) {
    init {
        require(name.isNotBlank()) { "请输入心愿名称" }
        require(price >= BigDecimal.ZERO && price.scale() <= 2) { "价格须为非负金额，最多两位小数" }
    }
}
fun BigDecimal.money(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()
