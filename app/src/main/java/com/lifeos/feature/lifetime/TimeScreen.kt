package com.lifeos.feature.lifetime

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lifeos.app.ScreenState
import com.lifeos.core.ui.CalendarWarning
import com.lifeos.core.ui.Forest
import com.lifeos.core.ui.Lime
import com.lifeos.core.ui.Panel
import com.lifeos.domain.lifetime.FutureTimeBudget
import com.lifeos.domain.lifetime.LifeNode
import com.lifeos.domain.lifetime.LifeNodeType
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun TimeScreen(
    data: ScreenState.Ready,
    delete: (Long, (String?) -> Unit) -> Unit,
    openAddNode: () -> Unit,
) {
    var window by rememberSaveable { mutableIntStateOf(90) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("时间", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("看见一年与一生已经走到哪里。")
        CalendarWarning(if (data.annual.calendarEstimated) data.salary.calendarWarning else null)

        Panel(green = true) {
            Text("${data.salary.now.year} 已过去 ${(data.annual.progress * 1000).toInt() / 10.0}%", color = Lime, style = MaterialTheme.typography.titleLarge)
            AnnualRing(data.annual.progress.toFloat(), true)
            Text("还剩 ${data.annual.remainingNaturalDays} 天")
            Text("工作日 ${data.annual.remainingWorkdays} · 非工作日 ${data.annual.remainingNonWorkdays} · 法定假期 ${data.annual.remainingLegalHolidays}")
        }

        Panel {
            Text("人生时间轴", style = MaterialTheme.typography.titleLarge)
            val birth = data.settings.birthDate
            val age = data.currentAge
            if (birth == null || age == null) {
                Text("完成个人信息后查看人生时间轴和退休时间。")
            } else {
                Text("当前 ${age.years} 年 ${age.months} 个月")
                LifeTimeline(age.years, data.retirement?.date?.year?.minus(birth.year), true)
                val futureDefaults = listOf(40, 50).filter { it > age.years }
                Text((listOf("当前") + futureDefaults.map { "${it}岁" } + listOfNotNull(data.retirement?.let { "退休" })).joinToString("  ·  "))
            }
        }

        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("自定义人生节点", style = MaterialTheme.typography.titleLarge)
                    Text("已添加 " + data.nodes.size + " 个节点")
                }
                Button(onClick = openAddNode) { Text("新增节点") }
            }
            Text("新增时进入独立页面；已有节点只在这里展示和手动删除。")
        }

        data.nodes.forEach { node ->
            Panel {
                Text(node.title, style = MaterialTheme.typography.titleMedium)
                val result = data.nodeResults[node.id]
                if (result == null) Text("年龄节点需要先设置出生日期。") else if (result.reached) {
                    Text("节点已到达 · ${result.targetDate}")
                } else {
                    Text("距离节点 ${result.remaining.years}年 ${result.remaining.months}个月 ${result.remaining.days}天")
                    Text("约 ${result.totalDays} 天 · 约 ${result.naturalWeekendCount} 个自然周末")
                    Text("自然周末仅按星期六/星期日统计，不等同于法定非工作日。")
                }
                TextButton(onClick = { delete(node.id) { message = it ?: "已删除" } }) { Text("删除") }
            }
        }

        Panel {
            Text("距离退休", style = MaterialTheme.typography.titleLarge)
            val retirement = data.retirement
            if (retirement == null) Text("完成个人信息后查看退休时间。") else if (retirement.reached) {
                Text("已达到法定退休年龄")
            } else {
                Text("${retirement.remaining.years}年 ${retirement.remaining.months}个月 ${retirement.remaining.days}天", style = MaterialTheme.typography.headlineSmall)
                Text("预计退休日期 ${retirement.date}")
                Text("约 ${java.time.temporal.ChronoUnit.DAYS.between(data.salary.now.toLocalDate(), retirement.date)} 天")
                Text("预计还需要 ${retirement.estimatedWorkdays} 个工作日")
                Text("退休具体日期与未来工作日均为估算 · ${retirement.policyVersion}")
            }
        }

        Panel {
            Text("未来时间预算", style = MaterialTheme.typography.titleLarge)
            Text("未来真正可以自由安排的大块时间有多少？")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(90, 180, 365).forEach { days -> FilterChip(window == days, { window = days }, { Text("$days 天") }) }
            }
            data.futureBudgets[window]?.let { FutureBudgetCard(it) }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun AddLifeNodeScreen(
    save: (LifeNode, (String?) -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    var nodeTitle by rememberSaveable { mutableStateOf("") }
    var nodeTarget by rememberSaveable { mutableStateOf("") }
    var ageNode by rememberSaveable { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) { Text("← 返回时间") }
        Text("新增人生节点", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("记录一个年龄或日期，只计算它与你的时间距离。")
        Panel {
            Input("节点名称", nodeTitle, { nodeTitle = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(ageNode, { ageNode = true }, { Text("年龄节点") })
                FilterChip(!ageNode, { ageNode = false }, { Text("日期节点") })
            }
            Input(
                if (ageNode) "目标年龄" else "目标日期 YYYY-MM-DD",
                nodeTarget,
                { nodeTarget = it },
                if (ageNode) KeyboardType.Number else KeyboardType.Text,
            )
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(
            onClick = {
                runCatching {
                    val now = LocalDateTime.now(Clock.systemDefaultZone())
                    if (ageNode) {
                        LifeNode(title = nodeTitle, type = LifeNodeType.AGE, targetAge = nodeTarget.toInt(), createdAt = now)
                    } else {
                        LifeNode(title = nodeTitle, type = LifeNodeType.DATE, targetDate = LocalDate.parse(nodeTarget), createdAt = now)
                    }
                }.fold(
                    onSuccess = { node ->
                        busy = true
                        save(node) { failure ->
                            busy = false
                            if (failure == null) onBack() else message = failure
                        }
                    },
                    onFailure = { message = it.message },
                )
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (busy) "正在保存…" else "保存节点") }
    }
}

@Composable
private fun AnnualRing(progress: Float, animated: Boolean) {
    val value by animateFloatAsState(progress, tween(if (animated) 700 else 0), label = "annual progress")
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val radius = size.minDimension * .38f
        val topLeft = Offset(size.width / 2 - radius, size.height / 2 - radius)
        val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        drawArc(Lime.copy(alpha = .18f), -90f, 360f, false, topLeft, arcSize, style = Stroke(18f, cap = StrokeCap.Round))
        drawArc(Lime, -90f, 360f * value.coerceIn(0f, 1f), false, topLeft, arcSize, style = Stroke(18f, cap = StrokeCap.Round))
    }
}

@Composable
private fun LifeTimeline(currentAge: Int, retirementAge: Int?, animated: Boolean) {
    val end = (retirementAge ?: 65).coerceAtLeast(currentAge + 1)
    val position = (currentAge.toFloat() / end).coerceIn(0f, 1f)
    val animatedPosition by animateFloatAsState(position, tween(if (animated) 700 else 0), label = "life position")
    Canvas(Modifier.fillMaxWidth().height(80.dp)) {
        val y = size.height / 2
        val start = 16f
        val finish = size.width - 16f
        drawLine(Forest.copy(alpha = .35f), Offset(start, y), Offset(finish, y), 8f, StrokeCap.Round)
        drawLine(Forest, Offset(start, y), Offset(start + (finish - start) * animatedPosition, y), 8f, StrokeCap.Round)
        drawCircle(Lime, 16f, Offset(start + (finish - start) * animatedPosition, y))
    }
}

@Composable
private fun FutureBudgetCard(budget: FutureTimeBudget) {
    Text("未来 ${budget.windowDays} 天", style = MaterialTheme.typography.headlineSmall)
    Text("工作日 ${budget.workdays} · 非工作日 ${budget.nonWorkdays}")
    Text("法定假期 ${budget.legalHolidays} · 完整可休周末 ${budget.fullWeekends}")
    if (budget.calendarEstimated) Text("⚠ 窗口包含未收录官方日历的年份，部分结果按普通星期规则估算。")
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
