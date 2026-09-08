package com.lifeos.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeos.domain.money
import com.lifeos.domain.ThemeMode
import com.lifeos.domain.salary.WorkState
import java.math.BigDecimal
import java.math.RoundingMode

val Forest = Color(0xFF183C32)
val Lime = Color(0xFFCEF58A)
val WarmCream = Color(0xFFF8F4EA)
val WarmSand = Color(0xFFE9DDC8)
val Copper = Color(0xFFA86D36)
@Composable fun LifeOsTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when(themeMode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    val colors = if(dark) darkColorScheme(
        primary=Lime,onPrimary=Forest,primaryContainer=Color(0xFF29473D),onPrimaryContainer=Color(0xFFE5F7C6),
        background=Color(0xFF121512),surface=Color(0xFF1D211D),surfaceVariant=Color(0xFF292D27),
        onSurface=Color(0xFFF1EEE6),onSurfaceVariant=Color(0xFFC9C7BE),secondary=Color(0xFFD9B58D),
        outline=Color(0xFF454B43),outlineVariant=Color(0xFF343A33),
    ) else lightColorScheme(
        primary=Forest,onPrimary=Color.White,primaryContainer=Color(0xFFE4F4C9),onPrimaryContainer=Forest,
        background=WarmCream,surface=Color(0xFFFFFDF8),surfaceVariant=Color(0xFFF1EADF),
        onSurface=Color(0xFF272B27),onSurfaceVariant=Color(0xFF66675F),secondary=Copper,
        outline=Color(0xFFD6CCBC),outlineVariant=Color(0xFFE8DFD1),
    )
    val typography = Typography(
        headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
    MaterialTheme(
        colorScheme=colors,
        typography=typography,
        shapes=Shapes(
            extraSmall=RoundedCornerShape(10.dp), small=RoundedCornerShape(14.dp),
            medium=RoundedCornerShape(20.dp), large=RoundedCornerShape(28.dp), extraLarge=RoundedCornerShape(34.dp),
        ),
        content=content,
    )
}
@Composable fun Panel(modifier: Modifier = Modifier, green: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(),shape=RoundedCornerShape(26.dp),
        colors=CardDefaults.cardColors(containerColor=if(green) Forest else MaterialTheme.colorScheme.surface,
            contentColor=if(green) Color.White else MaterialTheme.colorScheme.onSurface),
        border=if(green) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation=CardDefaults.cardElevation(defaultElevation=if(green) 6.dp else 1.dp)) {
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
        WorkState.AFTER_WORK -> "🏃 下班，任务完成！"
        WorkState.HOLIDAY -> "🎉 今日拒绝出售人生"
    }
    fun message(state: WorkState, progress: Double) = when(state) {
        WorkState.BEFORE_WORK -> "珍惜现在，这是今天最后的自由时光。"
        WorkState.WORKING -> if(progress > .8) "胜利在望，再坚持一下就能撤离工位。" else "至少这一秒，是有工资的。"
        WorkState.LUNCH_BREAK -> "放心吃，这段时间老板买不到。"
        WorkState.AFTER_WORK -> "今天的牛马任务已完成，请立即撤离工位。"
        WorkState.HOLIDAY -> "好好休息，资本家今天买不到你。"
    }
}
