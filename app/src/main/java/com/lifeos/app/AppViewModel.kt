package com.lifeos.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.domain.RetirementType
import com.lifeos.domain.UserSettings
import com.lifeos.domain.balance.BalanceItem
import com.lifeos.domain.lifetime.AnnualBalance
import com.lifeos.domain.lifetime.FutureTimeBudget
import com.lifeos.domain.lifetime.FutureTimeBudgetCalculator
import com.lifeos.domain.lifetime.CurrentAge
import com.lifeos.domain.lifetime.LifeNode
import com.lifeos.domain.lifetime.LifeTimeResult
import com.lifeos.domain.lifetime.LifeTimeCalculator
import com.lifeos.domain.retirement.RetirementCalculator
import com.lifeos.domain.retirement.RetirementResult
import com.lifeos.domain.salary.SalaryCalculator
import com.lifeos.domain.salary.SalaryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface ScreenState {
    data object Loading : ScreenState
    data class Failed(val message: String) : ScreenState
    data class Ready(
        val settings: UserSettings,
        val salary: SalaryResult,
        val balances: List<BalanceItem>,
        val nodes: List<LifeNode>,
        val retirement: RetirementResult?,
        val annual: AnnualBalance,
        val futureBudgets: Map<Int, FutureTimeBudget>,
        val currentAge: CurrentAge?,
        val nodeResults: Map<Long, LifeTimeResult>,
        val calendarDescription: String,
    ) : ScreenState
}

private data class Stored(
    val settings: UserSettings,
    val balances: List<BalanceItem>,
    val nodes: List<LifeNode>,
)

private data class DayCalculations(
    val annual: AnnualBalance,
    val future: Map<Int, FutureTimeBudget>,
)

class AppViewModel(private val app: LifeOsApplication) : ViewModel() {
    private val calculator = SalaryCalculator(app.calendar)
    private val clock: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now(Clock.systemDefaultZone()))
            delay(250)
        }
    }
    private var retirementKey: Triple<LocalDate?, RetirementType?, LocalDate>? = null
    private var retirementCache: RetirementResult? = null
    private var dayKey: LocalDate? = null
    private var dayCache: DayCalculations? = null
    private var nodeKey: Triple<List<LifeNode>, LocalDate?, LocalDate>? = null
    private var nodeCache: Map<Long, LifeTimeResult> = emptyMap()
    private val refresh = MutableStateFlow(0)

    val state: StateFlow<ScreenState> = combine(
        app.settings.settings,
        app.life.balances,
        app.life.nodes,
        refresh,
    ) { settings, balances, nodes, _ -> Stored(settings, balances, nodes) }
        .combine(clock) { data, now -> buildReady(data, now) as ScreenState }
        .catch { emit(ScreenState.Failed(it.message ?: "本地数据读取失败")) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScreenState.Loading)

    private fun buildReady(data: Stored, now: LocalDateTime): ScreenState.Ready {
        val today = now.toLocalDate()
        val retirement = Triple(data.settings.birthDate, data.settings.retirementType, today).let { key ->
            if (key != retirementKey) {
                retirementCache = if (key.first != null && key.second != null && key.first!! <= today) {
                    RetirementCalculator(app.calendar).calculate(key.first!!, key.second!!, today)
                } else null
                retirementKey = key
            }
            retirementCache
        }
        val day = if (dayKey == today) dayCache!! else {
            val futureCalculator = FutureTimeBudgetCalculator(app.calendar)
            DayCalculations(
                annual = LifeTimeCalculator(app.calendar).annual(today),
                future = listOf(90, 180, 365).associateWith { futureCalculator.calculate(today, it) },
            ).also { dayKey = today; dayCache = it }
        }
        val description = app.calendar.coveredYears.joinToString("、") { year ->
            val meta = app.calendar.metadata(year)!!
            "$year（${meta.version}，更新 ${meta.lastUpdated}）"
        }
        val lifeCalculator = LifeTimeCalculator(app.calendar)
        val age = data.settings.birthDate?.takeIf { it <= today }?.let { lifeCalculator.age(it, today) }
        val nodeResults = Triple(data.nodes, data.settings.birthDate, today).let { key ->
            if (key != nodeKey) {
                nodeCache = data.nodes.mapNotNull { node ->
                    runCatching { node.id to lifeCalculator.node(node, data.settings.birthDate, today) }.getOrNull()
                }.toMap()
                nodeKey = key
            }
            nodeCache
        }
        return ScreenState.Ready(
            settings = data.settings,
            salary = calculator.calculate(data.settings, now),
            balances = data.balances,
            nodes = data.nodes,
            retirement = retirement,
            annual = day.annual,
            futureBudgets = day.future,
            currentAge = age,
            nodeResults = nodeResults,
            calendarDescription = description,
        )
    }

    fun retry() { refresh.value++ }

    fun saveSettings(settings: UserSettings, done: (String?) -> Unit) = launch(done) { app.settings.save(settings) }
    fun saveBalance(item: BalanceItem, done: (String?) -> Unit) = launch(done) { app.life.save(item) }
    fun deleteBalance(id: Long, done: (String?) -> Unit) = launch(done) { app.life.deleteBalance(id) }
    fun saveNode(item: LifeNode, done: (String?) -> Unit) = launch(done) { app.life.save(item) }
    fun deleteNode(id: Long, done: (String?) -> Unit) = launch(done) { app.life.deleteNode(id) }

    private fun launch(done: (String?) -> Unit, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.fold({ done(null) }, { done(it.message ?: "操作失败") })
        }
    }
}
