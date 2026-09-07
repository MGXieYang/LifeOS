package com.lifeos.feature.settings

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
import com.lifeos.core.ui.*
import com.lifeos.domain.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.Clock
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable fun SettingsScreen(initial: UserSettings, calendarDescription: String,
    save: (UserSettings,(String?) -> Unit) -> Unit,
    updateCalendar: (Int,(String) -> Unit) -> Unit,
    exportBackup: (Uri,(String?) -> Unit) -> Unit = {_,done->done("当前入口不可用")},
    importBackup: (Uri,(String?) -> Unit) -> Unit = {_,done->done("当前入口不可用")},
    onboarding: Boolean = false,
) {
    var salary by rememberSaveable { mutableStateOf(initial.monthlySalary.money()) }
    var start by rememberSaveable { mutableStateOf(initial.workStart.toString()) }
    var lunchStart by rememberSaveable { mutableStateOf(initial.lunchStart.toString()) }
    var lunchEnd by rememberSaveable { mutableStateOf(initial.lunchEnd.toString()) }
    var end by rememberSaveable { mutableStateOf(initial.workEnd.toString()) }
    var payday by rememberSaveable { mutableStateOf(initial.salaryDay.toString()) }
    var birth by rememberSaveable { mutableStateOf(initial.birthDate?.toString() ?: "") }
    var type by rememberSaveable { mutableStateOf(initial.retirementType) }
    var lunchEnabled by rememberSaveable { mutableStateOf(initial.lunchBreakEnabled) }
    var freeEnabled by rememberSaveable { mutableStateOf(initial.freeTimeEnabled) }
    var sleep by rememberSaveable { mutableStateOf(initial.sleepMinutes.toString()) }
    var commute by rememberSaveable { mutableStateOf(initial.commuteMinutes.toString()) }
    var necessaryEnabled by rememberSaveable { mutableStateOf(initial.necessaryLifeEnabled) }
    var necessary by rememberSaveable { mutableStateOf(initial.necessaryLifeMinutes.toString()) }
    var animations by rememberSaveable { mutableStateOf(initial.animationsEnabled) }
    var funMode by rememberSaveable { mutableStateOf(initial.funModeEnabled) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var updating by remember { mutableStateOf(false) }
    val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->uri?.let{exportBackup(it){message=it?:"备份已导出"}}}
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{importBackup(it){message=it?:"备份已导入"}}}
    fun readSettings(): UserSettings {
        require(Regex("[0-9]+(\\.[0-9]{1,2})?").matches(salary.trim())) { "请输入税后月薪，最多两位小数" }
        val birthday=if(birth.isBlank()) null else runCatching { LocalDate.parse(birth.trim()) }.getOrElse { error("出生日期格式应为 YYYY-MM-DD") }
        require(birthday==null || birthday<=LocalDate.now(Clock.systemDefaultZone())) { "出生日期不能晚于今天" }
        fun time(value: String): LocalTime {
            require(Regex("[0-2][0-9]:[0-5][0-9]").matches(value.trim())) { "时间格式应为 HH:mm，例如 09:30" }
            return runCatching { LocalTime.parse(value.trim()) }.getOrElse { error("请输入有效时间") }
        }
        return UserSettings(monthlySalary=salary.trim().toBigDecimal(),workStart=time(start),lunchStart=time(lunchStart),
            lunchEnd=time(lunchEnd),workEnd=time(end),lunchBreakEnabled=lunchEnabled,
            salaryDay=payday.toIntOrNull() ?: error("发薪日请输入 1～31 的整数"),birthDate=birthday,retirementType=type,
            freeTimeEnabled=freeEnabled,sleepMinutes=sleep.toIntOrNull()?:error("请输入睡眠分钟数"),
            commuteMinutes=commute.toIntOrNull()?:error("请输入通勤分钟数"),necessaryLifeEnabled=necessaryEnabled,
            necessaryLifeMinutes=necessary.toIntOrNull()?:error("请输入必要生活分钟数"),animationsEnabled=animations,
            funModeEnabled=funMode,currencySymbol=initial.currencySymbol,moneyDecimals=initial.moneyDecimals,onboarded=true)
    }
    fun submit() {
        val result=runCatching { readSettings() }
        result.onFailure { error=it.message }
        result.onSuccess { value ->
            busy=true; error=null
            save(value) { failure -> busy=false;error=failure;if(failure==null) message="已保存，当前月份已按新设置重新计算。" }
        }
    }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(if(onboarding) "欢迎加入牛马驱动器 🐂" else "我的打工设置",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text(if(onboarding) "看着时间，一秒一秒变成钱。" else "规则由你设定，时间负责变现。")
        if(onboarding) {
            LinearProgressIndicator(progress={(step+1)/5f},modifier=Modifier.fillMaxWidth())
            Text("第 ${step+1} / 5 步",style=MaterialTheme.typography.labelLarge)
        }
        if(!onboarding || step==0) Panel {
            Text("先给自己的时间标个价",style=MaterialTheme.typography.titleLarge)
            Input("税后月薪（元 / 月）",salary,{salary=it},KeyboardType.Decimal)
            Text("工资和个人设置仅保存在这台设备。",style=MaterialTheme.typography.bodySmall)
        }
        if(!onboarding || step==1) Panel {
            Text("自定义你的营业时间",style=MaterialTheme.typography.titleLarge)
            Input("上班 HH:mm",start,{start=it})
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("启用午休",Modifier.padding(top=12.dp));Switch(lunchEnabled,{lunchEnabled=it}) }
            if(lunchEnabled) { Input("午休开始 HH:mm",lunchStart,{lunchStart=it});Input("午休结束 HH:mm",lunchEnd,{lunchEnd=it}) }
            Input("下班 HH:mm",end,{end=it})
            Text("四个时间均可修改。午休不计薪，保存后立即重算；暂不支持跨夜班。",style=MaterialTheme.typography.bodySmall)
        }
        if(!onboarding || step==2) Panel {
            Text("每月哪天回血？",style=MaterialTheme.typography.titleLarge)
            Input("自定义每月发薪日（1～31 号）",payday,{payday=it},KeyboardType.Number)
            Text("默认 7 号；不足该日号的月份按月末计算。随时可修改。",style=MaterialTheme.typography.bodySmall)
        }
        if(!onboarding || step==3) Panel {
            Text("给退休留个盼头",style=MaterialTheme.typography.titleLarge)
            Input("出生日期 YYYY-MM-DD（可留空跳过）",birth,{birth=it})
            listOf(RetirementType.MALE to "男性职工",RetirementType.FEMALE_55 to "女性职工 · 原退休年龄 55 岁",RetirementType.FEMALE_50 to "女性职工 · 原退休年龄 50 岁").forEach { (value,label) ->
                Row { RadioButton(selected=type==value,onClick={type=value});TextButton(onClick={type=value}) {Text(label)} }
            }
            Text("按一般职工法定年龄估算，具体日按生日推算；不包含特殊工种和弹性退休。",style=MaterialTheme.typography.bodySmall)
        }
        if(!onboarding) Panel {
            Text("生活时间",style=MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("启用自由时间模型");Switch(freeEnabled,{freeEnabled=it})}
            Input("每日睡眠（分钟）",sleep,{sleep=it},KeyboardType.Number)
            Input("每日往返通勤（分钟）",commute,{commute=it},KeyboardType.Number)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("计入必要生活时间");Switch(necessaryEnabled,{necessaryEnabled=it})}
            if(necessaryEnabled) Input("必要生活（分钟）",necessary,{necessary=it},KeyboardType.Number)
        }
        if(!onboarding) Panel {
            Text("显示",style=MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("动效");Switch(animations,{animations=it})}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("趣味文案");Switch(funMode,{funMode=it})}
        }
        if(onboarding && step==4) Panel(green=true) {
            Text("准备好了，开始变现。",style=MaterialTheme.typography.titleLarge,color=Lime)
            Text("月薪 ¥ $salary\n工作 $start — $end\n午休 $lunchStart — $lunchEnd\n每月 $payday 号发薪")
            Text("关闭 App 也不会丢进度，下次打开按系统时间恢复。")
        }
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        message?.let { Text(it,color=Forest) }
        if(onboarding) Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(step>0) OutlinedButton(onClick={step--;error=null},enabled=!busy) {Text("上一步")}
            Button(onClick={
                if(step==4) submit() else {
                    // Validate each visible step without forcing future optional fields.
                    val issue=runCatching {
                        when(step) {
                            0 -> { require(Regex("[0-9]+(\\.[0-9]{1,2})?").matches(salary.trim()) && salary.toBigDecimal().signum()>0) {"月薪须大于 0，最多两位小数"} }
                            1 -> { UserSettings(workStart=LocalTime.parse(start),lunchStart=LocalTime.parse(lunchStart),lunchEnd=LocalTime.parse(lunchEnd),workEnd=LocalTime.parse(end)) }
                            2 -> require(payday.toIntOrNull() in 1..31) {"发薪日请输入 1～31 的整数"}
                            3 -> readSettings()
                        }
                    }.exceptionOrNull()
                    if(issue==null) {step++;error=null} else error=issue.message ?: "请检查输入格式"
                }
            },enabled=!busy,modifier=Modifier.weight(1f)) {Text(if(busy) "正在保存…" else if(step==4) "启动牛马驱动器" else if(step==3 && birth.isBlank()) "暂时跳过" else "下一步")}
        } else {
            Button(onClick={submit()},enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text(if(busy) "正在保存…" else "保存设置") }
            Text("更改月薪、工时会立即重算当前月份，不保留历史版本。",style=MaterialTheme.typography.bodySmall)
            Panel {
                Text("法定工作日日历",style=MaterialTheme.typography.titleLarge)
                Text(calendarDescription,style=MaterialTheme.typography.bodySmall)
                Text("可按需联网更新，成功后保存在本机。来源：holiday-cn 开源数据（整理自国务院公告），非官方 API。",style=MaterialTheme.typography.bodySmall)
                val year=LocalDate.now(Clock.systemDefaultZone()).year
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    for(y in listOf(year,year+1)) OutlinedButton(onClick={updating=true;updateCalendar(y) {message=it;updating=false}},enabled=!updating) {Text("更新 $y 年")}
                }
                if(updating) Text("正在检查日历…")
            }
            Panel {
                Text("数据",style=MaterialTheme.typography.titleLarge)
                Text("个人数据默认只保存在本机。建议定期导出 JSON 备份。",style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({exporter.launch("life-os-backup-${LocalDate.now()}.json")}){Text("导出备份")};OutlinedButton({importer.launch(arrayOf("application/json","text/plain"))}){Text("导入备份")}}
            }
            Panel {Text("关于",style=MaterialTheme.typography.titleLarge);Text("Life OS 0.0.3 · 当前需求基线 V2.0")}
        }
        Spacer(Modifier.height(12.dp))
    }
}
@Composable private fun Input(label: String,value: String,change: (String)->Unit,type: KeyboardType=KeyboardType.Text) {
    OutlinedTextField(value,change,label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=type),modifier=Modifier.fillMaxWidth())
}
