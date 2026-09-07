package com.niuma.driver.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.niuma.driver.domain.money
import com.niuma.driver.domain.salary.WorkState
import java.math.BigDecimal
import java.math.RoundingMode

val Forest = Color(0xFF183C32)
val Lime = Color(0xFFCEF58A)
@Composable fun NiuMaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=lightColorScheme(primary=Forest,onPrimary=Color.White,
        primaryContainer=Lime,onPrimaryContainer=Forest,background=Color(0xFFF5F6F2),
        surface=Color(0xFFFFFFFF),onSurface=Color(0xFF202B25),secondary=Color(0xFF64755D)),content=content)
}
@Composable fun Panel(modifier: Modifier = Modifier, green: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),
        colors=CardDefaults.cardColors(containerColor=if(green) Forest else MaterialTheme.colorScheme.surface,
            contentColor=if(green) Color.White else MaterialTheme.colorScheme.onSurface)) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
    }
}
@Composable fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier,verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(label,style=MaterialTheme.typography.labelMedium)
        Text(value,style=MaterialTheme.typography.titleLarge,fontFamily=FontFamily.Monospace)
    }
}
@Composable fun CalendarWarning(message: String?) {
    if(message != null) Surface(color=Color(0xFFFFE8B9),shape=RoundedCornerShape(16.dp)) {
        Text(message,Modifier.padding(14.dp),style=MaterialTheme.typography.bodySmall,color=Color(0xFF664500))
    }
}
fun currency(value: BigDecimal) = "¥ ${value.money()}"
fun decimal(value: BigDecimal, scale: Int = 1) = value.setScale(scale,RoundingMode.HALF_UP).toPlainString()
fun timer(seconds: Long): String {
    val s=seconds.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s/3600,s/60%60,s%60)
}
object MotivationTextProvider {
    fun title(state: WorkState) = when(state) {
        WorkState.BEFORE_WORK -> "😴 牛马尚未上线"
        WorkState.WORKING -> "🐂 牛马驱动器运行中"
        WorkState.LUNCH_BREAK -> "🍚 正在补充牛马燃料"
        WorkState.WORKING_AFTERNOON -> "🐂 下午场已启动"
        WorkState.AFTER_WORK -> "🏃 下班，任务完成！"
        WorkState.HOLIDAY -> "🎉 今日拒绝出售人生"
    }
    fun message(state: WorkState, progress: Double) = when(state) {
        WorkState.BEFORE_WORK -> "珍惜现在，这是今天最后的自由时光。"
        WorkState.WORKING, WorkState.WORKING_AFTERNOON -> if(progress > .8) "胜利在望，再坚持一下就能撤离工位。" else "至少这一秒，是有工资的。"
        WorkState.LUNCH_BREAK -> "放心吃，这段时间老板买不到。"
        WorkState.AFTER_WORK -> "今天的牛马任务已完成，请立即撤离工位。"
        WorkState.HOLIDAY -> "好好休息，资本家今天买不到你。"
    }
    fun wish(days: BigDecimal) = when {
        days < BigDecimal.ONE -> "这个可以冲，老板请的。"
        days < BigDecimal("5") -> "几天班换一个快乐，你自己看着办。"
        days < BigDecimal("23") -> "喜欢就买之前，先让老板付一阵工资。"
        else -> "这个愿望建议先别让老板知道。"
    }
}
