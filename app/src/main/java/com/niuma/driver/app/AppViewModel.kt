package com.niuma.driver.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niuma.driver.domain.*
import com.niuma.driver.domain.salary.*
import com.niuma.driver.domain.retirement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime

sealed interface ScreenState {
    data object Loading: ScreenState
    data class Failed(val message: String): ScreenState
    data class Ready(val settings: UserSettings, val salary: SalaryResult, val wishes: List<WishItem>,
        val retirement: RetirementResult?, val calendarDescription: String): ScreenState
}
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppViewModel(private val app: NiuMaApplication): ViewModel() {
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
        combine(app.settings.settings, app.wishes.wishes, clock) { settings,wishes,now ->
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
                }) as ScreenState
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
}


