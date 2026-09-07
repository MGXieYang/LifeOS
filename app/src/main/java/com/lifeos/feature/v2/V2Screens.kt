package com.lifeos.feature.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.lifeos.app.ScreenState
import com.lifeos.core.ui.*
import com.lifeos.domain.balance.*
import com.lifeos.domain.decision.*
import com.lifeos.domain.lifetime.*
import com.lifeos.domain.retirement.RetirementResult
import com.lifeos.domain.salary.WorkState
import java.math.BigDecimal
import java.time.*

@Composable fun NowScreen(data:ScreenState.Ready,openWork:()->Unit,openSettings:()->Unit){
    val s=data.salary; val focus=data.balances.firstOrNull{it.isPinned} ?: data.balances.firstOrNull{it.status==BalanceStatus.CONSIDERING}
    Page("当下", "今天是你生活的哪一页") {
        CalendarWarning(s.calendarWarning)
        Panel(green=true){Text(MotivationTextProvider.title(s.state),color=Lime,style=MaterialTheme.typography.titleMedium);Text(currency(s.todayEarned),style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold);LinearProgressIndicator({s.todayProgress.toFloat()},Modifier.fillMaxWidth(),color=Lime);Text("已工作 ${timer(s.todayWorkedSeconds)} · ${MotivationTextProvider.message(s.state,s.todayProgress)}");TextButton(openWork){Text("查看工作与收入 →",color=Lime)}}
        Panel { Text("生活换算",style=MaterialTheme.typography.titleLarge); val annual=BigDecimal("12")*data.settings.monthlySalary;Text("税后年收入约 ${currency(annual)}",style=MaterialTheme.typography.headlineSmall);Text("今天的时间，也属于你自己。") }
        Panel { Text("今年还剩",style=MaterialTheme.typography.titleLarge);Text("${data.annual.remainingNaturalDays} 天",style=MaterialTheme.typography.headlineMedium);LinearProgressIndicator({data.annual.progress.toFloat()},Modifier.fillMaxWidth());Text("工作日 ${data.annual.remainingWorkdays} · 非工作日 ${data.annual.remainingNonWorkdays}${if(data.annual.calendarEstimated) " · 缺失年份按普通工作周估算" else ""}") }
        Panel { Text("当前关注",style=MaterialTheme.typography.titleLarge);if(focus==null)Text("在“天平”里记下一个正在考虑的选择。") else {Text(focus.name,style=MaterialTheme.typography.titleMedium);Text("${currency(focus.price)} · ${status(focus.status)}")};TextButton(openSettings){Text("设置")}}
    }
}

@Composable fun BalanceScreen(data:ScreenState.Ready,save:(BalanceItem,(String?)->Unit)->Unit,delete:(Long,(String?)->Unit)->Unit,pin:(Long?,(String?)->Unit)->Unit){
    var name by rememberSaveable{mutableStateOf("")};var price by rememberSaveable{mutableStateOf("")};var months by rememberSaveable{mutableStateOf("")};var extra by rememberSaveable{mutableStateOf("")};var monthly by rememberSaveable{mutableStateOf("")};var annual by rememberSaveable{mutableStateOf("")};var frequency by rememberSaveable{mutableStateOf("")};var category by rememberSaveable{mutableStateOf(BalanceCategory.OTHER)};var result by remember{mutableStateOf<LifeBalanceResult?>(null)};var message by remember{mutableStateOf<String?>(null)}
    fun calculate():LifeBalanceResult { val p=price.toBigDecimalOrNull()?:error("请输入有效价格");return LifeBalanceCalculator.calculate(LifeBalanceInput(p,data.settings.monthlySalary,data.salary.dailyWorkSeconds,data.salary.monthlyWorkSeconds,months.toIntOrNull(),UsageFrequencyType.MONTHLY.takeIf{frequency.isNotBlank()},frequency.toBigDecimalOrNull(),extra.toBigDecimalOrNull()?:BigDecimal.ZERO,monthly.toBigDecimalOrNull()?:BigDecimal.ZERO,annual.toBigDecimalOrNull()?:BigDecimal.ZERO,data.freeTime.freeSeconds.takeIf{data.settings.freeTimeEnabled})) }
    Page("天平","把价格还原成需要付出的生命时间"){
        Panel { Input("想买什么",name,{name=it});Input("价格",price,{price=it},KeyboardType.Decimal);Text("分类：${categoryLabel(category)}");BalanceCategory.entries.chunked(2).forEach{group->Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){group.forEach{FilterChip(category==it,{category=it},{Text(categoryLabel(it))})}}};Input("预计使用月数（可选）",months,{months=it},KeyboardType.Number);Input("每月预计使用次数（可选）",frequency,{frequency=it},KeyboardType.Decimal);Input("一次性附加成本",extra,{extra=it},KeyboardType.Decimal);Input("每月持有成本",monthly,{monthly=it},KeyboardType.Decimal);Input("每年持有成本",annual,{annual=it},KeyboardType.Decimal);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({runCatching{calculate()}.fold({result=it;message=null},{message=it.message})}){Text("只计算")};Button({runCatching{val r=calculate();require(name.isNotBlank()){"请输入名称"};val now=LocalDateTime.now();save(BalanceItem(name=name,price=price.toBigDecimal(),category=category,expectedUsagePeriodMonths=months.toIntOrNull(),usageFrequencyType=UsageFrequencyType.MONTHLY.takeIf{frequency.isNotBlank()},usageFrequencyValue=frequency.toBigDecimalOrNull(),oneTimeExtraCost=extra.toBigDecimalOrNull()?:BigDecimal.ZERO,monthlyHoldingCost=monthly.toBigDecimalOrNull()?:BigDecimal.ZERO,annualHoldingCost=annual.toBigDecimalOrNull()?:BigDecimal.ZERO,createdAt=now,updatedAt=now)){message=it?:"已保存"};result=r}.onFailure{message=it.message}}){Text("保存考虑")}};message?.let{Text(it)} }
        result?.let{r->Panel(green=true){Text("这件东西需要",color=Lime);Text("${decimal(r.workHours)} 小时工作",style=MaterialTheme.typography.headlineMedium);Text("总成本 ${currency(r.totalCost)} · ≈ ${decimal(r.workDays)} 个工作日 · ${decimal(r.salaryRatio*BigDecimal(100))}% 月薪");r.costPerUse?.let{Text("预计单次成本 ${currency(it)}")};r.equivalentFreeEvenings?.let{Text("≈ ${decimal(it)} 个自由夜晚")};Text("生命成本：${level(r.level)}")}}
        data.balances.forEach{x->Panel { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(x.name,style=MaterialTheme.typography.titleMedium);Text("${currency(x.price)} · ${status(x.status)}")};TextButton({pin(if(x.isPinned)null else x.id){message=it?:if(x.isPinned)"已取消置顶" else "已置顶"}}){Text(if(x.isPinned)"取消置顶" else "置顶")}};Row{TextButton({val now=LocalDateTime.now();save(x.copy(status=BalanceStatus.PURCHASED,purchasedAt=now,updatedAt=now)){message=it?:"已标记购入"}}){Text("已购入")};TextButton({save(x.copy(status=BalanceStatus.GAVE_UP,updatedAt=LocalDateTime.now())){message=it?:"已放弃"}}){Text("放弃")};TextButton({delete(x.id){message=it?:"已删除"}}){Text("删除")}}}}
    }
}

@Composable fun TimeScreen(data:ScreenState.Ready,save:(LifeNode,(String?)->Unit)->Unit,delete:(Long,(String?)->Unit)->Unit){
    var title by rememberSaveable{mutableStateOf("")};var target by rememberSaveable{mutableStateOf("")};var asAge by rememberSaveable{mutableStateOf(true)};var msg by remember{mutableStateOf<String?>(null)}
    Page("时间","看清一年与一生的余额"){
        Panel(green=true){Text("年度余额",color=Lime);Text("还剩 ${data.annual.remainingNaturalDays} 天",style=MaterialTheme.typography.headlineMedium);Text("工作日 ${data.annual.remainingWorkdays} · 非工作日 ${data.annual.remainingNonWorkdays}");LinearProgressIndicator({data.annual.progress.toFloat()},Modifier.fillMaxWidth(),color=Lime)}
        data.retirement?.let{RetirementCard(it)}
        if(data.settings.freeTimeEnabled) Panel {Text("每天真正属于你的时间",style=MaterialTheme.typography.titleLarge);Text(timer(data.freeTime.freeSeconds),style=MaterialTheme.typography.headlineMedium);Text("睡眠、工作时段、通勤和必要生活之后${if(data.freeTime.isClamped) "；输入总量超过 24 小时，结果已归零" else ""}")}
        Panel {Text("添加人生节点",style=MaterialTheme.typography.titleLarge);Input("节点名称",title,{title=it});Row{FilterChip(asAge,{asAge=true},{Text("按年龄")});Spacer(Modifier.width(8.dp));FilterChip(!asAge,{asAge=false},{Text("按日期")})};Input(if(asAge)"目标年龄" else "YYYY-MM-DD",target,{target=it},if(asAge)KeyboardType.Number else KeyboardType.Text);Button({runCatching{val now=LocalDateTime.now();val x=if(asAge)LifeNode(title=title,type=LifeNodeType.AGE,targetAge=target.toInt(),createdAt=now) else LifeNode(title=title,type=LifeNodeType.DATE,targetDate=LocalDate.parse(target),createdAt=now);save(x){msg=it?:"已添加"};title="";target=""}.onFailure{msg=it.message}}){Text("添加")};msg?.let{Text(it)}}
        data.nodes.forEach{x->Panel {Text(x.title,style=MaterialTheme.typography.titleMedium);val targetDate=if(x.type==LifeNodeType.AGE)data.settings.birthDate?.plusYears(x.targetAge!!.toLong()) else x.targetDate;Text(if(x.type==LifeNodeType.AGE)"${x.targetAge} 岁 · ${targetDate?:"请先设置出生日期"}" else x.targetDate.toString());targetDate?.let{val days=java.time.temporal.ChronoUnit.DAYS.between(data.salary.now.toLocalDate(),it).coerceAtLeast(0);Text(if(days==0L)"节点已到达" else "还剩 $days 天")};TextButton({delete(x.id){msg=it?:"已删除"}}){Text("删除")}}}
    }
}

@Composable fun DecisionScreen(data:ScreenState.Ready,save:(Decision,(String?)->Unit)->Unit,delete:(Long,(String?)->Unit)->Unit,review:(DecisionReview,(String?)->Unit)->Unit){
    var title by rememberSaveable{mutableStateOf("")};var background by rememberSaveable{mutableStateOf("")};var reason by rememberSaveable{mutableStateOf("")};var risk by rememberSaveable{mutableStateOf("")};var expected by rememberSaveable{mutableStateOf("")};var confidence by rememberSaveable{mutableFloatStateOf(50f)};var preference by rememberSaveable{mutableStateOf(DecisionPreference.UNDECIDED)};var reflection by rememberSaveable{mutableStateOf("")};var reviewScore by rememberSaveable{mutableFloatStateOf(7f)};var msg by remember{mutableStateOf<String?>(null)};val stats=DecisionStatisticsCalculator.calculate(data.decisions,data.reviews)
    Page("决策","记录当时为什么这样选"){
        Panel {Text("新决策",style=MaterialTheme.typography.titleLarge);Input("标题",title,{title=it});Input("背景",background,{background=it});Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(DecisionPreference.YES to "倾向做",DecisionPreference.NO to "倾向不做",DecisionPreference.UNDECIDED to "未决定").forEach{(v,l)->FilterChip(preference==v,{preference=v},{Text(l)})}};Input("主要理由",reason,{reason=it});Input("最大风险",risk,{risk=it});Input("预期结果",expected,{expected=it});Text("信心 ${confidence.toInt()}%");Slider(confidence,{confidence=(it/5).toInt()*5f},valueRange=0f..100f,steps=19);Button({runCatching{save(Decision(title=title,background=background,initialPreference=preference,mainReason=reason,biggestRisk=risk,expectedResult=expected,confidence=confidence.toInt(),createdAt=LocalDateTime.now())){msg=it?:"已记录"};title="";background="";reason="";risk="";expected=""}.onFailure{msg=it.message}}){Text("记录决定")};msg?.let{Text(it)}}
        Panel(green=true){Text("决策回看",color=Lime);Text("共 ${stats.total} 条 · 已复盘 ${stats.reviewed} 条");stats.averageSatisfaction?.let{Text("平均满意度 ${"%.1f".format(it)} / 10 · 不会再选 ${stats.regretCount} 次")}}
        Panel {Text("复盘输入",style=MaterialTheme.typography.titleMedium);Input("复盘反思（用于下方任一决策）",reflection,{reflection=it});Text("满意度 ${reviewScore.toInt()} / 10");Slider(reviewScore,{reviewScore=it.toInt().toFloat()},valueRange=1f..10f,steps=8)}
        data.decisions.forEach{x->Panel {Text(x.title,style=MaterialTheme.typography.titleMedium);Text("信心 ${x.confidence?:"未填"}% · ${decisionStatus(x.effectiveStatus(LocalDate.now()))}");if(x.mainReason.isNotBlank())Text("理由：${x.mainReason}");if(x.biggestRisk.isNotBlank())Text("风险：${x.biggestRisk}");Row{TextButton({val now=LocalDateTime.now();save(x.copy(status=DecisionStatus.DECIDED,finalDecision=when(x.initialPreference){DecisionPreference.YES->"做";DecisionPreference.NO->"不做";else->"待补充"},decidedAt=now,reviewDate=now.toLocalDate().plusDays(30))){msg=it?:"已决定，30 天后提醒复盘"}}){Text("做出决定")};TextButton({review(DecisionReview(decisionId=x.id,resultRating=ResultRating.AS_EXPECTED,wouldChooseAgain=ChooseAgain.YES,satisfactionScore=reviewScore.toInt(),reflection=reflection,reviewedAt=LocalDateTime.now())){e->if(e==null)save(x.copy(status=DecisionStatus.REVIEWED)){msg=it?:"已完成复盘"}else msg=e}}){Text("完成复盘")};TextButton({delete(x.id){msg=it?:"已删除"}}){Text("删除")}}}}
    }
}

@Composable private fun Page(title:String,subtitle:String,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text(subtitle);content();Spacer(Modifier.height(20.dp))}}
@Composable private fun Input(label:String,value:String,change:(String)->Unit,type:KeyboardType=KeyboardType.Text)=OutlinedTextField(value,change,label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=type),modifier=Modifier.fillMaxWidth())
@Composable private fun RetirementCard(r:RetirementResult)=Panel {Text("退休节点",style=MaterialTheme.typography.titleLarge);Text(if(r.reached)"已达到法定退休年龄" else "${r.remaining.years}年 ${r.remaining.months}个月 ${r.remaining.days}天",style=MaterialTheme.typography.headlineSmall);Text("预计日期 ${r.date} · 未来工作日为估算")}
private fun status(x:BalanceStatus)=when(x){BalanceStatus.CONSIDERING->"考虑中";BalanceStatus.PURCHASED->"已购入";BalanceStatus.GAVE_UP->"已放弃"}
private fun level(x:LifeCostLevel)=when(x){LifeCostLevel.CASUAL->"随手级";LifeCostLevel.HALF_DAY->"半日级";LifeCostLevel.ONE_DAY->"一日级";LifeCostLevel.SMALL_BLEED->"小出血";LifeCostLevel.MONTHLY_HEAVY->"月度重物";LifeCostLevel.LIFE_EQUIPMENT->"人生装备";LifeCostLevel.MAJOR_DECISION->"重大决定"}
private fun categoryLabel(x:BalanceCategory)=when(x){BalanceCategory.DIGITAL->"数码";BalanceCategory.TRAVEL->"旅行";BalanceCategory.TRANSPORT->"交通";BalanceCategory.HOUSING->"居住";BalanceCategory.ENTERTAINMENT->"娱乐";BalanceCategory.LEARNING->"学习";BalanceCategory.LIFE->"生活";BalanceCategory.OTHER->"其他"}
private fun decisionStatus(x:DecisionStatus)=when(x){DecisionStatus.THINKING->"思考中";DecisionStatus.DECIDED->"已决定";DecisionStatus.WAITING->"等待结果";DecisionStatus.REVIEW_DUE->"待复盘";DecisionStatus.REVIEWED->"已复盘"}
