package com.lifeos.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lifeos.core.ui.Panel
import com.lifeos.domain.RetirementType
import com.lifeos.domain.ThemeMode
import com.lifeos.domain.UserSettings
import com.lifeos.domain.money
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun SettingsScreen(
    initial: UserSettings,
    calendarDescription: String,
    save: (UserSettings, (String?) -> Unit) -> Unit,
) {
    var salary by rememberSaveable(initial) { mutableStateOf(initial.monthlySalary.money()) }
    var start by rememberSaveable(initial) { mutableStateOf(initial.workStart.toString()) }
    var lunchStart by rememberSaveable(initial) { mutableStateOf(initial.lunchStart.toString()) }
    var lunchEnd by rememberSaveable(initial) { mutableStateOf(initial.lunchEnd.toString()) }
    var end by rememberSaveable(initial) { mutableStateOf(initial.workEnd.toString()) }
    var payday by rememberSaveable(initial) { mutableStateOf(initial.salaryDay.toString()) }
    var birth by rememberSaveable(initial) { mutableStateOf(initial.birthDate?.toString().orEmpty()) }
    var retirementType by rememberSaveable(initial) { mutableStateOf(initial.retirementType) }
    var lunchEnabled by rememberSaveable(initial) { mutableStateOf(initial.lunchBreakEnabled) }
    var animations by rememberSaveable(initial) { mutableStateOf(initial.animationsEnabled) }
    var funMode by rememberSaveable(initial) { mutableStateOf(initial.funModeEnabled) }
    var theme by rememberSaveable(initial) { mutableStateOf(initial.themeMode) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun build(): UserSettings {
        val pay = salary.trim().toBigDecimalOrNull() ?: error("请输入有效的税后月薪")
        require(pay > BigDecimal.ZERO && pay.scale() <= 2) { "月薪须大于 0，最多两位小数" }
        val birthday = birth.trim().takeIf(String::isNotBlank)?.let {
            runCatching { LocalDate.parse(it) }.getOrElse { error("出生日期格式应为 YYYY-MM-DD") }
        }
        require(birthday == null || birthday <= LocalDate.now(Clock.systemDefaultZone())) { "出生日期不能晚于今天" }
        require((birthday == null) == (retirementType == null)) { "出生日期和退休人员类别请同时填写，或一起留空" }
        return initial.copy(
            monthlySalary = pay,
            workStart = LocalTime.parse(start.trim()),
            lunchStart = LocalTime.parse(lunchStart.trim()),
            lunchEnd = LocalTime.parse(lunchEnd.trim()),
            workEnd = LocalTime.parse(end.trim()),
            lunchBreakEnabled = lunchEnabled,
            salaryDay = payday.toIntOrNull() ?: error("发薪日请输入 1～31 的整数"),
            birthDate = birthday,
            retirementType = retirementType,
            animationsEnabled = animations,
            funModeEnabled = funMode,
            themeMode = theme,
        )
    }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("调整规则后，所有结果立即按当前系统时间重新计算。")

        Panel {
            Text("工作设置", style = MaterialTheme.typography.titleLarge)
            Field("税后月薪", salary, { salary = it }, KeyboardType.Decimal)
            Field("上班时间 HH:mm", start, { start = it })
            Field("下班时间 HH:mm", end, { end = it })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("午休", modifier = Modifier.padding(top = 12.dp))
                Switch(lunchEnabled, { lunchEnabled = it })
            }
            if (lunchEnabled) {
                Field("午休开始 HH:mm", lunchStart, { lunchStart = it })
                Field("午休结束 HH:mm", lunchEnd, { lunchEnd = it })
            }
            Field("每月发薪日（1～31）", payday, { payday = it }, KeyboardType.Number)
        }

        Panel {
            Text("个人设置", style = MaterialTheme.typography.titleLarge)
            Field("出生日期 YYYY-MM-DD", birth, { birth = it })
            RetirementOptions(retirementType) { retirementType = it }
            TextButton(onClick = { birth = ""; retirementType = null }) { Text("清除退休信息") }
            Text("自定义人生节点在“时间”页面管理。")
        }

        Panel {
            Text("显示设置", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("动画", modifier = Modifier.padding(top = 12.dp)); Switch(animations, { animations = it })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (funMode) "轻松模式" else "简洁模式", modifier = Modifier.padding(top = 12.dp)); Switch(funMode, { funMode = it })
            }
            Text("主题")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ThemeMode.SYSTEM to "跟随系统", ThemeMode.LIGHT to "浅色", ThemeMode.DARK to "深色").forEach { (value, label) ->
                    FilterChip(theme == value, { theme = value }, { Text(label) })
                }
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(
            onClick = {
                runCatching(::build).fold(
                    onSuccess = { value -> busy = true; error = null; save(value) { busy = false; error = it; if (it == null) message = "已保存" } },
                    onFailure = { error = it.message },
                )
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (busy) "正在保存…" else "保存设置") }

        Panel {
            Text("数据", style = MaterialTheme.typography.titleLarge)
            Text("工作设置、个人设置、天平历史和人生节点只保存在本机。V3 Phase 1-5 不提供云同步或不兼容的 V2 备份入口。")
        }
        Panel {
            Text("本地工作日日历", style = MaterialTheme.typography.titleLarge)
            Text(calendarDescription)
            Text("缺失年度按普通星期规则估算并明确提示；核心功能不依赖网络 API。")
        }
        Panel {
            Text("关于", style = MaterialTheme.typography.titleLarge)
            Text("LifeOS 0.0.4 · 产品基线 V3.0")
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun RetirementOptions(selected: RetirementType?, onSelect: (RetirementType) -> Unit) {
    listOf(
        RetirementType.MALE to "男性职工",
        RetirementType.FEMALE_55 to "女性职工（原 55 岁退休类别）",
        RetirementType.FEMALE_50 to "女性职工（原 50 岁退休类别）",
    ).forEach { (value, label) ->
        Row {
            RadioButton(selected == value, { onSelect(value) })
            TextButton(onClick = { onSelect(value) }) { Text(label) }
        }
    }
}

@Composable
private fun Field(label: String, value: String, change: (String) -> Unit, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = change,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}
