package com.niuma.driver.feature.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.niuma.driver.app.ScreenState
import com.niuma.driver.core.ui.*
import java.math.BigDecimal

@Composable fun StatisticsScreen(data: ScreenState.Ready) {
    val s=data.salary
    var input by rememberSaveable { mutableStateOf("60") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("时间报价所",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text("${s.now.monthValue}月 · 你的时间，目前这个价")
        CalendarWarning(s.calendarWarning)
        Panel(green=true) {
            Text("每工作小时",color=Lime)
            Text(currency(s.salaryPerSecond*BigDecimal(3600)),style=MaterialTheme.typography.headlineLarge)
            Text("请老板珍惜使用。")
        }
        Panel {
            Text("本月牛马账本",style=MaterialTheme.typography.titleLarge)
            Metric("税后月薪",currency(data.settings.monthlySalary))
            Metric("法定工作日 / 有效工作时间","${s.monthlyWorkDays} 天 / ${decimal(s.monthlyWorkSeconds.toBigDecimal().divide(BigDecimal(3600),java.math.MathContext.DECIMAL128))} 小时")
            Metric("每工作日",currency(s.salaryPerSecond*s.dailyWorkSeconds.toBigDecimal()))
            Metric("每分钟 / 每秒","${currency(s.salaryPerSecond*BigDecimal(60))} / ¥ ${decimal(s.salaryPerSecond,4)}")
        }
        Panel {
            Text("我的时间值多少钱？",style=MaterialTheme.typography.titleLarge)
            for((label,seconds) in listOf("1 分钟" to 60L,"10 分钟" to 600L,"1 小时" to 3600L,"1 工作日" to s.dailyWorkSeconds,"1 工作月" to s.monthlyWorkSeconds)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(label); Text(currency(s.salaryPerSecond*seconds.toBigDecimal()))
                }
            }
            OutlinedTextField(input,{input=it},label={Text("自定义工作分钟")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
            val minutes=input.toBigDecimalOrNull()?.takeIf { it>=BigDecimal.ZERO }
            Text(if(minutes==null) "请输入非负分钟数" else "这段时间 = ${currency(minutes*BigDecimal(60)*s.salaryPerSecond)}",color=Forest)
        }
    }
}

