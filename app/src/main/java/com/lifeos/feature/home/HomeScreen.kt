package com.lifeos.feature.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.ScreenState
import com.lifeos.core.ui.CalendarWarning
import com.lifeos.core.ui.Lime
import com.lifeos.core.ui.Metric
import com.lifeos.core.ui.MotivationTextProvider
import com.lifeos.core.ui.Panel
import com.lifeos.core.ui.currency
import com.lifeos.core.ui.timer
import com.lifeos.domain.salary.WorkState
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NowScreen(data: ScreenState.Ready) {
    val salary = data.salary
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("当下", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(salary.now.format(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.CHINA)))
        CalendarWarning(salary.calendarWarning)

        Panel(green = true) {
            Text(statusTitle(data), style = MaterialTheme.typography.titleMedium, color = Lime)
            Text("今日已赚", style = MaterialTheme.typography.labelLarge)
            Text(
                currency(salary.todayEarned),
                fontSize = 42.sp,
                lineHeight = 48.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            if (salary.state == WorkState.WORKING && data.settings.animationsEnabled) {
                val transition = rememberInfiniteTransition(label = "earning")
                val alpha by transition.animateFloat(
                    initialValue = .4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                    label = "earning pulse",
                )
                Text("● 时间正在转换成钱", Modifier.alpha(alpha), color = Lime)
            }
            HorizontalDivider(color = Lime.copy(alpha = .2f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Metric("已工作", duration(salary.todayWorkedSeconds), Modifier.weight(1f))
                Metric(countdownLabel(salary.state), timer(salary.countdownSeconds), Modifier.weight(1f))
            }
            Text("今日进度 ${(salary.todayProgress * 100).toInt()}%")
            LinearProgressIndicator(
                progress = { salary.todayProgress.toFloat() },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = Lime,
                trackColor = Lime.copy(alpha = .16f),
            )
            if (data.settings.funModeEnabled) Text(MotivationTextProvider.message(salary.state, salary.todayProgress))
        }

        Panel {
            Text("💼 本月已赚", style = MaterialTheme.typography.titleLarge)
            Text(currency(salary.monthEarned), style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("月工资总额 ${currency(data.settings.monthlySalary)}")
            LinearProgressIndicator(progress = { salary.monthProgress.toFloat() }, modifier = Modifier.fillMaxWidth())
            Text("本月进度 ${(salary.monthProgress * 100).toInt()}% · 已完成 ${salary.completedWorkDays} / ${salary.monthlyWorkDays} 个工作日")
        }

        Panel {
            Text("📅 距离发薪日", style = MaterialTheme.typography.titleLarge)
            Text(salary.payday.toLocalDate().toString(), style = MaterialTheme.typography.titleMedium)
            Text(
                if (salary.isPayday) "今天发工资" else "${salary.paydaySeconds / 86_400} 天 ${salary.paydaySeconds / 3_600 % 24} 小时",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (data.settings.funModeEnabled && !salary.isPayday) Text("再坚持一下，工资在向你招手。")
        }
        Spacer(Modifier.height(20.dp))
    }
}

private fun statusTitle(data: ScreenState.Ready): String = if (data.settings.funModeEnabled) {
    MotivationTextProvider.title(data.salary.state)
} else when (data.salary.state) {
    WorkState.BEFORE_WORK -> "尚未开始工作"
    WorkState.WORKING -> "工作中"
    WorkState.LUNCH_BREAK -> "午休中"
    WorkState.AFTER_WORK -> "今日工作完成"
    WorkState.HOLIDAY -> "今天不是工作日"
}

private fun countdownLabel(state: WorkState) = when (state) {
    WorkState.BEFORE_WORK -> "距离上班"
    WorkState.WORKING -> "距离下班"
    WorkState.LUNCH_BREAK -> "午休剩余"
    WorkState.AFTER_WORK -> "今日已完成"
    WorkState.HOLIDAY -> "今日休息"
}

private fun duration(seconds: Long): String = "${seconds / 3_600}小时${seconds / 60 % 60}分钟"
