package com.lifeos.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.lifeos.domain.UserSettings
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun OnboardingScreen(initial: UserSettings, save: (UserSettings, (String?) -> Unit) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var salary by rememberSaveable { mutableStateOf(initial.monthlySalary.toPlainString()) }
    var start by rememberSaveable { mutableStateOf(initial.workStart.toString()) }
    var end by rememberSaveable { mutableStateOf(initial.workEnd.toString()) }
    var lunchEnabled by rememberSaveable { mutableStateOf(initial.lunchBreakEnabled) }
    var lunchStart by rememberSaveable { mutableStateOf(initial.lunchStart.toString()) }
    var lunchEnd by rememberSaveable { mutableStateOf(initial.lunchEnd.toString()) }
    var payday by rememberSaveable { mutableStateOf(initial.salaryDay.toString()) }
    var birth by rememberSaveable { mutableStateOf("") }
    var retirementType by rememberSaveable { mutableStateOf<RetirementType?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun build(): UserSettings {
        val pay = salary.trim().toBigDecimalOrNull() ?: error("请输入有效的税后月薪")
        require(pay > BigDecimal.ZERO && pay.scale() <= 2) { "月薪须大于 0，最多两位小数" }
        val birthday = birth.trim().takeIf(String::isNotBlank)?.let {
            runCatching { LocalDate.parse(it) }.getOrElse { error("出生日期格式应为 YYYY-MM-DD") }
        }
        require(birthday == null || birthday <= LocalDate.now(Clock.systemDefaultZone())) { "出生日期不能晚于今天" }
        require((birthday == null) == (retirementType == null)) { "出生日期和退休人员类别请同时填写，或一起跳过" }
        return initial.copy(
            monthlySalary = pay,
            workStart = LocalTime.parse(start.trim()),
            workEnd = LocalTime.parse(end.trim()),
            lunchBreakEnabled = lunchEnabled,
            lunchStart = LocalTime.parse(lunchStart.trim()),
            lunchEnd = LocalTime.parse(lunchEnd.trim()),
            salaryDay = payday.toIntOrNull() ?: error("发薪日请输入 1～31 的整数"),
            birthDate = birthday,
            retirementType = retirementType,
            onboarded = true,
        )
    }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("LifeOS", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("看见你的时间，也看见它值多少钱。", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth())

        when (step) {
            0 -> Panel {
                Text("欢迎使用 LifeOS", style = MaterialTheme.typography.headlineSmall)
                Text("先完成必要设置。所有个人数据只保存在本机，收入始终根据系统时间计算。")
                Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) { Text("开始设置") }
            }
            1 -> Panel {
                Text("工作设置", style = MaterialTheme.typography.headlineSmall)
                Field("税后月工资", salary, { salary = it }, KeyboardType.Decimal)
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
                Field("每月发薪日", payday, { payday = it }, KeyboardType.Number)
            }
            else -> Panel {
                Text("个人信息", style = MaterialTheme.typography.headlineSmall)
                Text("可稍后填写；跳过后仍可完整使用工作收入和天平。")
                Field("出生日期 YYYY-MM-DD", birth, { birth = it })
                RetirementOptions(retirementType) { retirementType = it }
                if (birth.isBlank()) TextButton(onClick = { retirementType = null }) { Text("暂时跳过") }
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (step > 0) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { step--; error = null }, enabled = !busy) { Text("上一步") }
            Button(
                onClick = {
                    if (step == 1) {
                        runCatching { build().copy(birthDate = null, retirementType = null) }
                            .fold({ step = 2; error = null }, { error = it.message })
                    } else {
                        runCatching(::build).fold(
                            onSuccess = { value ->
                                busy = true
                                save(value) { failure -> busy = false; error = failure }
                            },
                            onFailure = { error = it.message },
                        )
                    }
                },
                enabled = !busy,
                modifier = Modifier.weight(1f),
            ) { Text(if (step == 2) "进入 LifeOS" else "下一步") }
        }
    }
}

@Composable
private fun RetirementOptions(selected: RetirementType?, onSelect: (RetirementType) -> Unit) {
    listOf(
        RetirementType.MALE to "男性职工",
        RetirementType.FEMALE_55 to "女性职工（原 55 岁退休类别）",
        RetirementType.FEMALE_50 to "女性职工（原 50 岁退休类别）",
    ).forEach { (type, label) ->
        Row {
            RadioButton(selected == type, { onSelect(type) })
            TextButton(onClick = { onSelect(type) }) { Text(label) }
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
