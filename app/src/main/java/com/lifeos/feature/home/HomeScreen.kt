package com.lifeos.feature.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.ScreenState
import com.lifeos.core.ui.*
import com.lifeos.domain.WishStatus
import com.lifeos.domain.salary.WorkState
import com.lifeos.domain.wish.WishCalculator
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable fun HomeScreen(data: ScreenState.Ready, openWishes: () -> Unit, openSettings: () -> Unit) {
    val s=data.salary
    val working=s.state==WorkState.WORKING || s.state==WorkState.WORKING_AFTERNOON
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("牛马驱动器",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text(s.now.format(DateTimeFormatter.ofPattern("M月d日 · EEEE",Locale.CHINA)),style=MaterialTheme.typography.bodyMedium)
        CalendarWarning(s.calendarWarning)
        Panel(green=true) {
            Text(MotivationTextProvider.title(s.state),style=MaterialTheme.typography.titleMedium,color=Lime)
            Spacer(Modifier.height(2.dp))
            Text("今日已赚",style=MaterialTheme.typography.labelLarge)
            Text(currency(s.todayEarned),fontSize=42.sp,lineHeight=48.sp,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold)
            if(working) {
                val transition=rememberInfiniteTransition(label="earning")
                val alpha by transition.animateFloat(.4f,1f,infiniteRepeatable(tween(950),RepeatMode.Reverse),label="pulse")
                Text("● 每一秒，都在变现",Modifier.alpha(alpha),color=Lime,style=MaterialTheme.typography.labelMedium)
            } else Text(if(s.state==WorkState.HOLIDAY) "工资计时器暂停营业" else "收入按有效工作时间计算",style=MaterialTheme.typography.labelMedium)
            HorizontalDivider(color=Lime.copy(alpha=.2f))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                Metric("已工作",timer(s.todayWorkedSeconds),Modifier.weight(1f))
                Metric(when(s.state) {
                    WorkState.BEFORE_WORK -> "距离开工"
                    WorkState.LUNCH_BREAK -> "午休剩余"
                    WorkState.AFTER_WORK -> "今日已完成"
                    WorkState.HOLIDAY -> "今日休息"
                    else -> "距离下班"
                },timer(s.countdownSeconds),Modifier.weight(1f))
            }
            Text("今日牛马进度  ${(s.todayProgress*100).toInt()}%",style=MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress={s.todayProgress.toFloat()},Modifier.fillMaxWidth().height(7.dp),color=Lime,trackColor=Lime.copy(alpha=.16f))
            Text(MotivationTextProvider.message(s.state,s.todayProgress),style=MaterialTheme.typography.bodySmall)
        }
        Panel {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("💰 本月已赚",style=MaterialTheme.typography.titleMedium)
                Text("${decimal((s.monthProgress*100).toBigDecimal())}%",color=Forest)
            }
            Text(currency(s.monthEarned),style=MaterialTheme.typography.headlineMedium,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold)
            LinearProgressIndicator(progress={s.monthProgress.toFloat()},modifier=Modifier.fillMaxWidth())
            Text("已完成 ${s.completedWorkDays} / ${s.monthlyWorkDays} 个工作日 · 剩余 ${s.monthlyWorkDays-s.completedWorkDays} 天",style=MaterialTheme.typography.bodySmall)
        }
        Panel {
            Text("💸 距离发薪",style=MaterialTheme.typography.titleMedium)
            Text(if(s.isPayday) "今天发工资！" else "${s.paydaySeconds/86400}天 ${s.paydaySeconds/3600%24}小时",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text(if(s.isPayday) "今天是约定发薪日，具体到账看老板。" else "${s.payday.toLocalDate()} · 再坚持一下，账户就会回血。",style=MaterialTheme.typography.bodySmall)
        }
        Panel {
            Text("🏖 退休倒计时",style=MaterialTheme.typography.titleMedium)
            val r=data.retirement
            if(r==null) {
                Text("给漫长的打工生涯，一个盼头。")
                TextButton(onClick=openSettings) { Text("设置退休信息 →") }
            } else {
                Text(if(r.reached) "已达到法定退休年龄" else "${r.remaining.years}年 ${r.remaining.months}个月 ${r.remaining.days}天",style=MaterialTheme.typography.titleLarge)
                Text("预计还需 ${r.estimatedWorkdays} 个工作日",style=MaterialTheme.typography.bodyMedium)
                Text("法定退休年月 ${r.date.year}-${r.date.monthValue}；按生日估算至 ${r.date}。未来工作日与具体日期为预计值。",style=MaterialTheme.typography.bodySmall)
            }
        }
        Panel {
            Text("🎁 最近心愿",style=MaterialTheme.typography.titleMedium)
            val wish=data.wishes.firstOrNull { it.status==WishStatus.WISHING }
            if(wish==null) Text("把想要的快乐，换算成打工时光。") else {
                Text(wish.name,style=MaterialTheme.typography.titleLarge)
                Text(currency(wish.price))
                if(s.salaryPerSecond.signum()>0) Text("≈ ${decimal(WishCalculator.calculate(wish.price,s).days)} 个工作日")
            }
            TextButton(onClick=openWishes) { Text("去牛马兑换中心 →") }
        }
        Text("看着时间，一秒一秒变成钱。",style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
    }
}
