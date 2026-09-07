package com.niuma.driver.app

import android.app.Application
import androidx.room.Room
import com.niuma.driver.data.*
import com.niuma.driver.domain.calendar.WorkCalendar

class NiuMaApplication: Application() {
    val settings by lazy { SettingsRepository(this) }
    val wishes by lazy { WishRepository(Room.databaseBuilder(this,NiuMaDatabase::class.java,"niuma.db").build().wishes()) }
    val calendarRepository by lazy { NetworkCalendarRepository(this) }
    @Volatile private var loadedCalendar: WorkCalendar? = null
    val calendar: WorkCalendar get() = loadedCalendar ?: calendarRepository.load().also { loadedCalendar=it }
    fun reloadCalendar() { loadedCalendar=calendarRepository.load() }
}

