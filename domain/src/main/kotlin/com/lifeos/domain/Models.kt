package com.lifeos.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime

enum class RetirementType { MALE, FEMALE_55, FEMALE_50 }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
data class UserSettings(
    val monthlySalary: BigDecimal = BigDecimal("10000.00"),
    val workStart: LocalTime = LocalTime.of(9, 30),
    val lunchStart: LocalTime = LocalTime.of(12, 30),
    val lunchEnd: LocalTime = LocalTime.of(14, 0),
    val workEnd: LocalTime = LocalTime.of(19, 0),
    val lunchBreakEnabled: Boolean = true,
    val salaryDay: Int = 7,
    val birthDate: LocalDate? = null,
    val retirementType: RetirementType? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
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
    }
}
fun BigDecimal.money(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()
