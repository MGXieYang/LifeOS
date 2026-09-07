package com.lifeos.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.domain.*
import com.lifeos.domain.salary.*
import com.lifeos.domain.retirement.*
import com.lifeos.domain.balance.*
import com.lifeos.domain.decision.*
import com.lifeos.domain.freetime.*
import com.lifeos.domain.lifetime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime
import android.net.Uri

sealed interface ScreenState {
    data object Loading: ScreenState
    data class Failed(val message: String): ScreenState
    data class Ready(val settings: UserSettings, val salary: SalaryResult, val wishes: List<WishItem>,
        val retirement: RetirementResult?, val calendarDescription: String,
        val balances:List<BalanceItem>, val nodes:List<LifeNode>, val decisions:List<Decision>,
        val reviews:List<DecisionReview>, val freeTime:FreeTimeResult, val annual:AnnualBalance): ScreenState
}
private data class Stored(val settings:UserSettings,val wishes:List<WishItem>,val balances:List<BalanceItem>,val nodes:List<LifeNode>,val decisions:List<Decision>,val reviews:List<DecisionReview>)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppViewModel(private val app: LifeOsApplication): ViewModel() {
    private val calculator get() = SalaryCalculator(app.calendar)
    private val clock = flow {
        while (true) {
            // Read the current zone every time: Clock.systemDefaultZone cached once would miss zone changes.
            emit(LocalDateTime.now(Clock.systemDefaultZone()))
            delay(250)
        }
    }
    private var retirementKey: Triple<java.time.LocalDate?, RetirementType, java.time.LocalDate>? = null
    private var retirementCache: RetirementResult? = null
    private val refresh = MutableStateFlow(0)
    val state: StateFlow<ScreenState> = refresh.flatMapLatest {
        val stored=combine(app.settings.settings,app.wishes.wishes,app.life.balances,app.life.nodes,app.life.decisions,app.life.reviews) { values ->
            @Suppress("UNCHECKED_CAST") Stored(values[0] as UserSettings,values[1] as List<WishItem>,values[2] as List<BalanceItem>,values[3] as List<LifeNode>,values[4] as List<Decision>,values[5] as List<DecisionReview>)
        }
        combine(stored, clock) { data,now ->
            val settings=data.settings; val wishes=data.wishes
            val key = Triple(settings.birthDate,settings.retirementType,now.toLocalDate())
            if (key != retirementKey) {
                retirementCache = settings.birthDate?.takeIf { it <= now.toLocalDate() }?.let {
                    RetirementCalculator(app.calendar).calculate(it,settings.retirementType,now.toLocalDate())
                }
                retirementKey = key
            }
            ScreenState.Ready(settings,calculator.calculate(settings,now),wishes,retirementCache,
                app.calendar.coveredYears.joinToString("、") { year ->
                    val meta = app.calendar.metadata(year)!!
                    "$year（${meta.version}，更新 ${meta.lastUpdated}）"
                },data.balances,data.nodes,data.decisions,data.reviews,
                FreeTimeCalculator.calculate(settings),LifeTimeCalculator(app.calendar).annual(now.toLocalDate())) as ScreenState
        }.catch { emit(ScreenState.Failed(it.message ?: "本地数据读取失败")) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope,SharingStarted.WhileSubscribed(0),ScreenState.Loading)
    fun retry() { refresh.value++ }
    fun updateCalendar(year: Int, done: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val result=app.calendarRepository.update(year)
                kotlinx.coroutines.withContext(Dispatchers.IO) { app.reloadCalendar() }
                retirementKey=null
                refresh.value++
                result
            }.fold(done, {done("更新失败，保留原有日历：${it.message ?: "请检查网络后重试"}")})
        }
    }
    fun saveSettings(settings: UserSettings, done: (String?) -> Unit) {
        viewModelScope.launch { runCatching { app.settings.save(settings) }.fold({ done(null) }, { done(it.message ?: "保存失败，请重试") }) }
    }
    fun saveWish(wish: WishItem, done: (String?) -> Unit) {
        viewModelScope.launch { runCatching { app.wishes.save(wish) }.fold({ done(null) }, { done(it.message ?: "保存失败，请重试") }) }
    }
    fun deleteWish(id: Long, done: (String?) -> Unit) {
        viewModelScope.launch { runCatching { app.wishes.delete(id) }.fold({ done(null) }, { done(it.message ?: "删除失败，请重试") }) }
    }
    fun saveBalance(item:BalanceItem,done:(String?)->Unit)=launch(done){app.life.save(item)}
    fun deleteBalance(id:Long,done:(String?)->Unit)=launch(done){app.life.deleteBalance(id)}
    fun pinBalance(id:Long?,done:(String?)->Unit)=launch(done){app.life.pin(id)}
    fun saveNode(item:LifeNode,done:(String?)->Unit)=launch(done){app.life.save(item)}
    fun deleteNode(id:Long,done:(String?)->Unit)=launch(done){app.life.deleteNode(id)}
    fun saveDecision(item:Decision,done:(String?)->Unit)=launch(done){app.life.save(item)}
    fun deleteDecision(id:Long,done:(String?)->Unit)=launch(done){app.life.deleteDecision(id)}
    fun saveReview(item:DecisionReview,done:(String?)->Unit)=launch(done){app.life.save(item)}
    fun exportBackup(uri:Uri,done:(String?)->Unit)=launch(done){app.backup.export(uri)}
    fun importBackup(uri:Uri,done:(String?)->Unit)=launch(done){app.backup.import(uri)}
    private fun launch(done:(String?)->Unit,block:suspend()->Unit){viewModelScope.launch{runCatching{block()}.fold({done(null)},{done(it.message?:"操作失败")})}}
}


