package com.lifeos.feature.wish

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.lifeos.app.ScreenState
import com.lifeos.core.ui.*
import com.lifeos.domain.*
import com.lifeos.domain.wish.WishCalculator
import java.time.Clock
import java.time.LocalDateTime

@Composable fun WishScreen(data: ScreenState.Ready, save: (WishItem,(String?)->Unit)->Unit, delete: (Long,(String?)->Unit)->Unit) {
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var editorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<WishItem?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val sorted=remember(data.wishes,sort) {
        when(sort) {
            0 -> data.wishes.sortedWith(compareByDescending<WishItem> {it.createdAt}.thenByDescending {it.id})
            else -> data.wishes.sortedWith(compareBy<WishItem> {it.price}.thenByDescending {it.createdAt})
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal=20.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("牛马兑换中心",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text("把打工时光，换成想要的生活。",Modifier.padding(vertical=8.dp))
        Button(onClick={editorId=0},modifier=Modifier.fillMaxWidth()) {Text("＋ 新增心愿")}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("最新","价格","工作时长").forEachIndexed { index,label -> FilterChip(selected=sort==index,onClick={sort=index},label={Text(label)}) }
        }
        CalendarWarning(data.salary.calendarWarning)
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        if(sorted.isEmpty()) Panel {
            Text("🎁 第一个心愿，留给自己",style=MaterialTheme.typography.titleLarge)
            Text("一杯咖啡、一副耳机，或者一次旅行。看看它需要多少牛马时光。")
        }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(vertical=12.dp)) {
            items(sorted,key={it.id}) { wish ->
                Panel {
                    Text(if(wish.status==WishStatus.ACHIEVED) "✓ ${wish.name} · 已实现" else wish.name,style=MaterialTheme.typography.titleLarge)
                    Text(currency(wish.price),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                    if(data.salary.salaryPerSecond.signum()>0) {
                        val result=WishCalculator.calculate(wish.price,data.salary)
                        Text("${decimal(result.hours)} 小时 · ${decimal(result.days)} 工作日",color=Forest)
                        Text("≈ ${decimal(result.months,2)} 个牛马月",style=MaterialTheme.typography.bodyMedium)
                        Text(MotivationTextProvider.wish(result.days),style=MaterialTheme.typography.bodySmall)
                    } else Text("本月无有效工作时间，暂不能换算。")
                    if(wish.remark.isNotBlank()) Text(wish.remark,style=MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick={editorId=wish.id},enabled=!busy) {Text("编辑")}
                        TextButton(onClick={
                            busy=true
                            val achieved=wish.status!=WishStatus.ACHIEVED
                            save(wish.copy(status=if(achieved) WishStatus.ACHIEVED else WishStatus.WISHING,
                                achievedAt=if(achieved) LocalDateTime.now(Clock.systemDefaultZone()) else null)) {error=it;busy=false}
                        },enabled=!busy) {Text(if(wish.status==WishStatus.ACHIEVED) "取消实现" else "已实现")}
                        TextButton(onClick={deleting=wish},enabled=!busy) {Text("删除",color=MaterialTheme.colorScheme.error)}
                    }
                }
            }
        }
    }
    editorId?.let { id ->
        val item=data.wishes.firstOrNull {it.id==id}
        WishEditor(item,close={editorId=null},save=save)
    }
    deleting?.let { wish -> AlertDialog(onDismissRequest={if(!busy) deleting=null},title={Text("删除这个心愿？")},
        text={Text("“${wish.name}”将从本机移除。")},confirmButton={TextButton(enabled=!busy,onClick={busy=true;delete(wish.id) {error=it;busy=false;if(it==null) deleting=null}}) {Text("删除")}},
        dismissButton={TextButton(onClick={deleting=null},enabled=!busy) {Text("保留")}}) }
}
@Composable private fun WishEditor(item: WishItem?,close: ()->Unit,save: (WishItem,(String?)->Unit)->Unit) {
    var name by rememberSaveable(item?.id) {mutableStateOf(item?.name ?: "")}
    var price by rememberSaveable(item?.id) {mutableStateOf(item?.price?.money() ?: "")}
    var remark by rememberSaveable(item?.id) {mutableStateOf(item?.remark ?: "")}
    var error by remember {mutableStateOf<String?>(null)}
    var busy by remember {mutableStateOf(false)}
    AlertDialog(onDismissRequest={if(!busy)close()},title={Text(if(item==null) "想要什么？" else "编辑心愿")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name,{name=it},label={Text("心愿名称")},singleLine=true)
            OutlinedTextField(price,{price=it},label={Text("价格（元，可为 0）")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true)
            OutlinedTextField(remark,{remark=it},label={Text("备注（可选）")},maxLines=4)
            error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        }
    },confirmButton={TextButton(enabled=!busy,onClick={
        runCatching {
            require(Regex("[0-9]+(\\.[0-9]{1,2})?").matches(price.trim())) {"请输入非负价格，最多两位小数"}
            WishItem(item?.id ?: 0,name.trim(),price.trim().toBigDecimal(),remark.trim(),item?.status ?: WishStatus.WISHING,
                item?.createdAt ?: LocalDateTime.now(Clock.systemDefaultZone()),item?.achievedAt)
        }.onFailure {error=it.message}.onSuccess {wish -> busy=true;save(wish) {error=it;busy=false;if(it==null)close()}}
    }) {Text(if(busy) "保存中…" else "保存")}},dismissButton={TextButton(onClick=close,enabled=!busy) {Text("取消")}})
}
