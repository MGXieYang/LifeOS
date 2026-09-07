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
    val workEnd: LocalTime = LocalTime.of(19, 30),
    val salaryDay: Int = 7,
    val birthDate: LocalDate? = null,
    val retirementType: RetirementType = RetirementType.MALE,
    val onboarded: Boolean = false,
) {
    init {
        require(monthlySalary > BigDecimal.ZERO) { "税后月薪必须大于 0" }
        require(monthlySalary.scale() <= 2) { "工资最多保留两位小数" }
        require(workStart < lunchStart && lunchStart < lunchEnd && lunchEnd < workEnd) {
            "时间需满足：上班 < 午休开始 < 午休结束 < 下班（不支持跨夜班）"
        }
        require(salaryDay in 1..31) { "发薪日应在 1～31 之间" }
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
