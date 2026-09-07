package com.lifeos.app

import android.app.Application
import androidx.room.Room
import com.lifeos.data.*
import com.lifeos.domain.calendar.WorkCalendar
import com.lifeos.domain.balance.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class LifeOsApplication: Application() {
    val settings by lazy { SettingsRepository(this) }
    val wishes by lazy { WishRepository(Room.databaseBuilder(this,LegacyDatabase::class.java,"legacy-v1.db").build().wishes()) }
    val life by lazy { LifeRepository(Room.databaseBuilder(this,LifeDatabase::class.java,"life-os.db").build()) }
    val backup by lazy { BackupRepository(this,settings,life) }
    val calendarRepository by lazy { NetworkCalendarRepository(this) }
    @Volatile private var loadedCalendar: WorkCalendar? = null
    val calendar: WorkCalendar get() = loadedCalendar ?: calendarRepository.load().also { loadedCalendar=it }
    fun reloadCalendar() { loadedCalendar=calendarRepository.load() }
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            val year=LocalDate.now().year
            listOf(year,year+1).forEach { runCatching { calendarRepository.update(it) } }
            reloadCalendar()
        }
        val migration=getSharedPreferences("v2_migration",MODE_PRIVATE)
        if(!migration.getBoolean("wishes_done",false)) CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            runCatching {
                val existing=life.balances.first()
                if(existing.isEmpty()) wishes.wishes.first().forEach { old ->
                    val updated=old.achievedAt ?: old.createdAt
                    life.save(BalanceItem(name=old.name,price=old.price,remark=old.remark,
                        status=if(old.status==com.lifeos.domain.WishStatus.ACHIEVED) BalanceStatus.PURCHASED else BalanceStatus.CONSIDERING,
                        createdAt=old.createdAt,updatedAt=updated,purchasedAt=old.achievedAt))
                }
                migration.edit().putBoolean("wishes_done",true).apply()
            }
        }
    }
}

