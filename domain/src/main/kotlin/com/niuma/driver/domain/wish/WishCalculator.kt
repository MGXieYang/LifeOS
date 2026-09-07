package com.niuma.driver.domain.wish

import com.niuma.driver.domain.salary.SalaryResult
import java.math.BigDecimal
import java.math.MathContext

data class WishWorkTimeResult(val seconds: BigDecimal, val minutes: BigDecimal,
    val hours: BigDecimal, val days: BigDecimal, val months: BigDecimal)
object WishCalculator {
    fun calculate(price: BigDecimal, salary: SalaryResult): WishWorkTimeResult {
        require(price >= BigDecimal.ZERO) { "价格不能为负数" }
        require(salary.salaryPerSecond > BigDecimal.ZERO) { "当前月份没有可用于换算的工作时间" }
        val seconds = price.divide(salary.salaryPerSecond, MathContext.DECIMAL128)
        fun divide(n: Long) = seconds.divide(n.toBigDecimal(), MathContext.DECIMAL128)
        return WishWorkTimeResult(seconds, divide(60), divide(3600), divide(salary.dailyWorkSeconds), divide(salary.monthlyWorkSeconds))
    }
}
