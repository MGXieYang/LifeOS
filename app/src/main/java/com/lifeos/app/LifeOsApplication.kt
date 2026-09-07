package com.lifeos.app

import android.app.Application
import androidx.room.Room
import androidx.core.content.edit
import com.lifeos.data.AssetCalendarRepository
import com.lifeos.data.LegacyDatabase
import com.lifeos.data.LifeDatabase
import com.lifeos.data.LifeRepository
import com.lifeos.data.MIGRATION_1_2
import com.lifeos.data.SettingsRepository
import com.lifeos.domain.balance.BalanceItem
import com.lifeos.domain.calendar.WorkCalendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime

class LifeOsApplication : Application() {
    val settings by lazy { SettingsRepository(this) }
    val life by lazy {
        LifeRepository(
            Room.databaseBuilder(this, LifeDatabase::class.java, "life-os.db")
                .addMigrations(MIGRATION_1_2)
                .build(),
        )
    }
    private val legacy by lazy {
        Room.databaseBuilder(this, LegacyDatabase::class.java, "legacy-v1.db").build().wishes()
    }
    val calendar: WorkCalendar by lazy { WorkCalendar(AssetCalendarRepository(this).years()) }

    override fun onCreate() {
        super.onCreate()
        val migration = getSharedPreferences("v3_migration", MODE_PRIVATE)
        if (!migration.getBoolean("legacy_wishes_done", false)) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching {
                    if (life.balances.first().isEmpty()) {
                        legacy.observe().first().forEach { old ->
                            life.save(
                                BalanceItem(
                                    name = old.name,
                                    price = BigDecimal(old.price),
                                    createdAt = LocalDateTime.parse(old.createdAt),
                                ),
                            )
                        }
                    }
                    migration.edit { putBoolean("legacy_wishes_done", true) }
                }
            }
        }
    }
}
