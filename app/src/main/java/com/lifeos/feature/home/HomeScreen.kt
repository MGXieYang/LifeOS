package com.lifeos.feature.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.ScreenState
import com.lifeos.core.ui.CalendarWarning
import com.lifeos.core.ui.Copper
import com.lifeos.core.ui.Lime
import com.lifeos.core.ui.MotivationTextProvider
import com.lifeos.core.ui.Panel
import com.lifeos.core.ui.currency
import com.lifeos.core.ui.decimal
import com.lifeos.core.ui.timer
import com.lifeos.domain.salary.WorkState
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NowScreen(data: ScreenState.Ready) {
    val salary = data.salary
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("WORK TODAY", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.secondary)
                Text(salary.now.format(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.CHINA)), style = MaterialTheme.typography.headlineMedium)
                Text("今天也在认真生活", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(salary.state)
        }
        CalendarWarning(salary.calendarWarning)

        HeroCard(data)
        EarningsRateCard(salary.salaryPerSecond)

        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("本月已赚", style = MaterialTheme.typography.titleLarge)
                    Text("时间正在一点点变成底气", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${(salary.monthProgress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
            }
            Text(currency(salary.monthEarned), fontSize = 30.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { salary.monthProgress.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("已完成 ${salary.completedWorkDays} / ${salary.monthlyWorkDays} 个工作日", style = MaterialTheme.typography.bodySmall)
                Text("月工资 ${currency(data.settings.monthlySalary)}", style = MaterialTheme.typography.bodySmall)
            }
        }

        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.width(48.dp).height(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Text("薪", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("距离发薪日", style = MaterialTheme.typography.titleMedium)
                    Text(salary.payday.toLocalDate().format(DateTimeFormatter.ofPattern("M月d日")), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    if (salary.isPayday) "今天发工资" else "${salary.paydaySeconds / 86_400}天 ${salary.paydaySeconds / 3_600 % 24}小时",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun HeroCard(data: ScreenState.Ready) {
    val salary = data.salary
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF243E34), Color(0xFF17251F), Color(0xFF322B21))))
            .border(1.dp, Color.White.copy(alpha = .12f), shape).padding(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(statusTitle(data), style = MaterialTheme.typography.titleMedium, color = Lime)
            Text(countdownLabel(salary.state), color = Color.White.copy(alpha = .72f))
            Text(timer(salary.countdownSeconds), fontSize = 36.sp, lineHeight = 40.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = Color.White)
            HorizontalDivider(color = Color.White.copy(alpha = .14f))
            Text("今日已赚", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = .78f))
            Text(currency(salary.todayEarned), fontSize = 44.sp, lineHeight = 50.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
            if (salary.state == WorkState.WORKING) {
                val transition = rememberInfiniteTransition(label = "earning")
                val pulseAlpha by transition.animateFloat(
                    initialValue = .45f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                    label = "earning pulse",
                )
                Text("● 时间正在转换成钱", Modifier.alpha(pulseAlpha), color = Lime)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("已工作 ${duration(salary.todayWorkedSeconds)}", color = Color.White.copy(alpha = .78f))
                Text("${(salary.todayProgress * 100).toInt()}%", color = Lime, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { salary.todayProgress.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Lime,
                trackColor = Color.White.copy(alpha = .14f),
            )
            Text(MotivationTextProvider.message(salary.state, salary.todayProgress), color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EarningsRateCard(perSecond: BigDecimal) {
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("我的时间单价", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                Text("实时", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RateMetric("每小时", perSecond.multiply(BigDecimal("3600")), 2, Modifier.weight(1f))
            RateDivider()
            RateMetric("每分钟", perSecond.multiply(BigDecimal("60")), 2, Modifier.weight(1f))
            RateDivider()
            RateMetric("每秒", perSecond, 4, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RateMetric(label: String, value: BigDecimal, scale: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("¥${decimal(value, scale)}", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RateDivider() {
    Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(42.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun StatusPill(state: WorkState) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(7.dp).height(7.dp).clip(CircleShape).background(if (state == WorkState.WORKING) Copper else MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(7.dp))
            Text(statusPillText(state), style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun statusPillText(state: WorkState) = when (state) {
    WorkState.BEFORE_WORK -> "未上班"
    WorkState.WORKING -> "上班中"
    WorkState.LUNCH_BREAK -> "午休中"
    WorkState.AFTER_WORK -> "已下班"
    WorkState.HOLIDAY -> "休息日"
}

private fun statusTitle(data: ScreenState.Ready): String = MotivationTextProvider.title(data.salary.state)

private fun countdownLabel(state: WorkState) = when (state) {
    WorkState.BEFORE_WORK -> "距离上班还有"
    WorkState.WORKING -> "距离下班还有"
    WorkState.LUNCH_BREAK -> "距离午休结束还有"
    WorkState.AFTER_WORK -> "今日已完成"
    WorkState.HOLIDAY -> "今天不用上班"
}

private fun duration(seconds: Long): String = "${seconds / 3_600}小时${seconds / 60 % 60}分钟"
