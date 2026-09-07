package com.lifeos.feature.balance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lifeos.app.ScreenState
import com.lifeos.core.ui.CalendarWarning
import com.lifeos.core.ui.Forest
import com.lifeos.core.ui.Lime
import com.lifeos.core.ui.Panel
import com.lifeos.core.ui.currency
import com.lifeos.core.ui.decimal
import com.lifeos.domain.balance.BalanceItem
import com.lifeos.domain.balance.LifeBalanceCalculator
import com.lifeos.domain.balance.LifeBalanceInput
import com.lifeos.domain.balance.LifeBalanceResult
import com.lifeos.domain.balance.PurchaseWeight
import com.lifeos.domain.balance.UsageFrequencyType
import com.lifeos.domain.balance.UsageValueCalculator
import com.lifeos.domain.balance.UsageValueInput
import com.lifeos.domain.balance.UsageValueResult
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDateTime

@Composable
fun BalanceScreen(
    data: ScreenState.Ready,
    save: (BalanceItem, (String?) -> Unit) -> Unit,
    delete: (Long, (String?) -> Unit) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var priceText by rememberSaveable { mutableStateOf("") }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var months by rememberSaveable { mutableStateOf("36") }
    var frequencyType by rememberSaveable { mutableStateOf(UsageFrequencyType.DAILY) }
    var frequencyValue by rememberSaveable { mutableStateOf("1") }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val price = priceText.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO && it.scale() <= 2 }
    val result = remember(price, data.settings.monthlySalary, data.salary.dailyWorkSeconds, data.salary.monthlyWorkSeconds) {
        price?.let {
            runCatching {
                LifeBalanceCalculator.calculate(
                    LifeBalanceInput(it, data.settings.monthlySalary, data.salary.dailyWorkSeconds, data.salary.monthlyWorkSeconds),
                )
            }.getOrNull()
        }
    }
    val usage = remember(price, advanced, months, frequencyType, frequencyValue) {
        if (!advanced || price == null) null else runCatching {
            UsageValueCalculator.calculate(
                UsageValueInput(price, months.toInt(), frequencyType, frequencyValue.toBigDecimal()),
            )
        }.getOrNull()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("⚖ 天平", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("换个角度，看见选择的重量。")
            }
            TextButton(onClick = { showHistory = !showHistory }) { Text("历史") }
        }
        CalendarWarning(data.salary.calendarWarning)

        Panel {
            Input("商品名称（可选）", name, { name = it })
            Input("金额", priceText, { priceText = it }, KeyboardType.Decimal)
            val current = price?.toFloat() ?: 0f
            val max = maxOf(10_000f, data.settings.monthlySalary.toFloat() * 2f, current * 1.2f)
            Text("看看不同价格有多重")
            Slider(
                value = current.coerceIn(0f, max),
                onValueChange = { priceText = it.toLong().toString() },
                valueRange = 0f..max,
            )
            result?.let { ScaleVisual(name, price!!, it, data.settings.animationsEnabled) }
        }

        result?.let { value ->
            Panel(green = true) {
                Text(weightTitle(value.weight), color = Lime, style = MaterialTheme.typography.titleLarge)
                Text(weightMessage(value.weight))
                Text("需要 ${decimal(value.workTimeCost.hours)} 工作小时", style = MaterialTheme.typography.headlineSmall)
                Text("${decimal(value.workTimeCost.days)} 工作日 · ${decimal(value.workTimeCost.salaryRatio * BigDecimal("100"))}% 月工资")
                Text("这是消费压力的规则判断，不是购买建议。")
            }
        }

        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("长期使用价值", style = MaterialTheme.typography.titleLarge)
                    Text("可选高级输入，默认关闭。")
                }
                Switch(advanced, { advanced = it })
            }
            if (advanced) {
                Input("预计使用月数", months, { months = it }, KeyboardType.Number)
                Text("预计使用频率")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        UsageFrequencyType.DAILY to "每天",
                        UsageFrequencyType.WEEKLY to "每周",
                        UsageFrequencyType.MONTHLY to "每月",
                    ).forEach { (type, label) -> FilterChip(frequencyType == type, { frequencyType = type }, { Text(label) }) }
                }
                Input("每个周期使用次数", frequencyValue, { frequencyValue = it }, KeyboardType.Decimal)
                usage?.let { UsageResult(it, result?.weight) }
            }
        }

        result?.let {
            Button(
                onClick = {
                    save(
                        BalanceItem(
                            name = name.trim(),
                            price = price!!,
                            expectedUsagePeriodMonths = months.toIntOrNull().takeIf { advanced },
                            usageFrequencyType = frequencyType.takeIf { advanced },
                            usageFrequencyValue = frequencyValue.toBigDecimalOrNull().takeIf { advanced },
                            createdAt = LocalDateTime.now(Clock.systemDefaultZone()),
                        ),
                    ) { message = it ?: "已保存到历史" }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存这次称重") }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        if (showHistory) {
            Text("称重历史", style = MaterialTheme.typography.titleLarge)
            if (data.balances.isEmpty()) Text("还没有保存过称重项目。")
            data.balances.forEach { item ->
                Panel {
                    Text(item.name.ifBlank { "未命名金额" }, style = MaterialTheme.typography.titleMedium)
                    Text(currency(item.price), style = MaterialTheme.typography.headlineSmall)
                    Text(item.createdAt.toLocalDate().toString())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            name = item.name
                            priceText = item.price.toPlainString()
                            advanced = item.expectedUsagePeriodMonths != null
                            item.expectedUsagePeriodMonths?.let { months = it.toString() }
                            item.usageFrequencyType?.let { frequencyType = it }
                            item.usageFrequencyValue?.let { frequencyValue = it.toPlainString() }
                        }) { Text("再次称量") }
                        TextButton(onClick = { delete(item.id) { message = it ?: "已删除" } }) { Text("删除") }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ScaleVisual(name: String, price: BigDecimal, result: LifeBalanceResult, animated: Boolean) {
    val targetTilt = when (result.weight) {
        PurchaseWeight.LIGHT -> -10f
        PurchaseWeight.SLIGHTLY_LIGHT -> -5f
        PurchaseWeight.NORMAL -> 0f
        PurchaseWeight.HEAVY -> 6f
        PurchaseWeight.VERY_HEAVY -> 11f
    }
    val tilt by animateFloatAsState(targetTilt, tween(if (animated) 500 else 0), label = "scale tilt")
    Canvas(Modifier.fillMaxWidth().height(210.dp)) {
        val center = Offset(size.width / 2, size.height * .45f)
        val half = size.width * .37f
        val left = Offset(center.x - half, center.y - tilt)
        val right = Offset(center.x + half, center.y + tilt)
        drawLine(Forest, left, right, strokeWidth = 10f, cap = StrokeCap.Round)
        drawLine(Forest, center, Offset(center.x, size.height * .82f), strokeWidth = 12f, cap = StrokeCap.Round)
        drawLine(Forest, Offset(center.x - 55f, size.height * .82f), Offset(center.x + 55f, size.height * .82f), strokeWidth = 12f, cap = StrokeCap.Round)
        listOf(left, right).forEach { point ->
            drawLine(Forest, point, Offset(point.x, point.y + 65f), strokeWidth = 4f)
            drawLine(Forest, Offset(point.x - 48f, point.y + 65f), Offset(point.x + 48f, point.y + 65f), strokeWidth = 5f, cap = StrokeCap.Round)
        }
        val blocks = result.weight.ordinal + 1
        repeat(blocks) { index ->
            drawRoundRect(
                color = Lime,
                topLeft = Offset(right.x - 30f, right.y + 48f - index * 15f),
                size = Size(60f, 13f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f),
            )
        }
        drawCircle(Color(0xFFD7B46A), 22f, Offset(left.x, left.y + 42f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${name.ifBlank { "金额" }}\n${currency(price)}")
        Text("${decimal(result.workTimeCost.days)} 工作日\n${decimal(result.workTimeCost.hours)} 小时")
    }
}

@Composable
private fun UsageResult(value: UsageValueResult, weight: PurchaseWeight?) {
    Text("预计使用 ${decimal(value.estimatedUsageCount, 0)} 次")
    Text("单次 ${currency(value.costPerUse)} · 每天 ${currency(value.costPerDay)} · 每月 ${currency(value.costPerMonth)}")
    Text(
        if (weight == PurchaseWeight.HEAVY || weight == PurchaseWeight.VERY_HEAVY) {
            "一次性压力较高；长期成本会随使用频率提高而摊薄。"
        } else "一次性压力可控；长期价值取决于实际使用频率。",
    )
}

private fun weightTitle(weight: PurchaseWeight) = when (weight) {
    PurchaseWeight.LIGHT -> "⚖ 很轻"
    PurchaseWeight.SLIGHTLY_LIGHT -> "⚖ 偏轻"
    PurchaseWeight.NORMAL -> "⚖ 适中"
    PurchaseWeight.HEAVY -> "⚖ 偏重，但可承担"
    PurchaseWeight.VERY_HEAVY -> "⚖ 很重，需要认真考虑"
}

private fun weightMessage(weight: PurchaseWeight) = when (weight) {
    PurchaseWeight.LIGHT -> "对当前收入的压力很小。"
    PurchaseWeight.SLIGHTLY_LIGHT -> "会占用一部分工作回报，但整体偏轻。"
    PurchaseWeight.NORMAL -> "这是一笔有感知、但尚在半个月工资内的支出。"
    PurchaseWeight.HEAVY -> "需要投入半个月到一个月工资。"
    PurchaseWeight.VERY_HEAVY -> "超过一个月工资，工作代价明显。"
}

@Composable
private fun Input(label: String, value: String, change: (String) -> Unit, type: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = change,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        modifier = Modifier.fillMaxWidth(),
    )
}

