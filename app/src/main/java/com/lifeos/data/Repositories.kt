package com.lifeos.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.lifeos.domain.RetirementType
import com.lifeos.domain.ThemeMode
import com.lifeos.domain.UserSettings
import com.lifeos.domain.calendar.CalendarRepository
import com.lifeos.domain.calendar.CalendarYear
import com.lifeos.domain.calendar.WorkDayType
import com.lifeos.domain.money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

private object SettingKeys {
    val salary = stringPreferencesKey("salary")
    val start = stringPreferencesKey("start")
    val lunchStart = stringPreferencesKey("lunch_start")
    val lunchEnd = stringPreferencesKey("lunch_end")
    val end = stringPreferencesKey("end")
    val lunchEnabled = booleanPreferencesKey("lunch_enabled")
    val payday = intPreferencesKey("payday")
    val birth = stringPreferencesKey("birth")
    val retirementType = stringPreferencesKey("retirement_type")
    val animations = booleanPreferencesKey("animations")
    val funMode = booleanPreferencesKey("fun_mode")
    val theme = stringPreferencesKey("theme")
    val onboarded = booleanPreferencesKey("onboarded")

    val freeEnabled = booleanPreferencesKey("free_enabled")
    val sleep = intPreferencesKey("sleep_minutes")
    val commute = intPreferencesKey("commute_minutes")
    val necessaryEnabled = booleanPreferencesKey("necessary_enabled")
    val necessary = intPreferencesKey("necessary_minutes")
    val currency = stringPreferencesKey("currency")
    val decimals = intPreferencesKey("money_decimals")
    val removedV3Names = setOf(freeEnabled, sleep, commute, necessaryEnabled, necessary, currency, decimals).map { it.name }.toSet()
}

private object V3SettingsMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData.asMap().keys.any { it.name in SettingKeys.removedV3Names }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val result = mutablePreferencesOf()
        currentData.asMap().forEach { (key, value) ->
            if (key.name !in SettingKeys.removedV3Names) copyPreference(result, key, value)
        }
        return result
    }

    override suspend fun cleanUp() = Unit
}

private val Context.settingsStore by preferencesDataStore(
    name = "user_settings",
    produceMigrations = { listOf(V3SettingsMigration) },
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<UserSettings> = context.settingsStore.data.map { p ->
        UserSettings(
            monthlySalary = BigDecimal(p[SettingKeys.salary] ?: "10000.00"),
            workStart = LocalTime.parse(p[SettingKeys.start] ?: "09:30"),
            lunchStart = LocalTime.parse(p[SettingKeys.lunchStart] ?: "12:30"),
            lunchEnd = LocalTime.parse(p[SettingKeys.lunchEnd] ?: "14:00"),
            workEnd = LocalTime.parse(p[SettingKeys.end] ?: "19:00"),
            lunchBreakEnabled = p[SettingKeys.lunchEnabled] ?: true,
            salaryDay = p[SettingKeys.payday] ?: 7,
            birthDate = p[SettingKeys.birth]?.let(LocalDate::parse),
            retirementType = p[SettingKeys.retirementType]?.let(RetirementType::valueOf),
            animationsEnabled = p[SettingKeys.animations] ?: true,
            funModeEnabled = p[SettingKeys.funMode] ?: true,
            themeMode = p[SettingKeys.theme]?.let(ThemeMode::valueOf) ?: ThemeMode.SYSTEM,
            onboarded = p[SettingKeys.onboarded] ?: false,
        )
    }

    suspend fun save(s: UserSettings) {
        context.settingsStore.edit { p ->
            p[SettingKeys.salary] = s.monthlySalary.money()
            p[SettingKeys.start] = s.workStart.toString()
            p[SettingKeys.lunchStart] = s.lunchStart.toString()
            p[SettingKeys.lunchEnd] = s.lunchEnd.toString()
            p[SettingKeys.end] = s.workEnd.toString()
            p[SettingKeys.lunchEnabled] = s.lunchBreakEnabled
            p[SettingKeys.payday] = s.salaryDay
            p[SettingKeys.animations] = s.animationsEnabled
            p[SettingKeys.funMode] = s.funModeEnabled
            p[SettingKeys.theme] = s.themeMode.name
            p[SettingKeys.onboarded] = s.onboarded
            if (s.birthDate == null) p.remove(SettingKeys.birth) else p[SettingKeys.birth] = s.birthDate.toString()
            val retirement = s.retirementType
            if (retirement == null) p.remove(SettingKeys.retirementType) else p[SettingKeys.retirementType] = retirement.name
            p.remove(SettingKeys.freeEnabled)
            p.remove(SettingKeys.sleep)
            p.remove(SettingKeys.commute)
            p.remove(SettingKeys.necessaryEnabled)
            p.remove(SettingKeys.necessary)
            p.remove(SettingKeys.currency)
            p.remove(SettingKeys.decimals)
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun copyPreference(target: androidx.datastore.preferences.core.MutablePreferences, key: Preferences.Key<*>, value: Any) {
    when (value) {
        is Boolean -> target[booleanPreferencesKey(key.name)] = value
        is Int -> target[intPreferencesKey(key.name)] = value
        is Long -> target[longPreferencesKey(key.name)] = value
        is Float -> target[floatPreferencesKey(key.name)] = value
        is Double -> target[doublePreferencesKey(key.name)] = value
        is String -> target[stringPreferencesKey(key.name)] = value
        is Set<*> -> target[stringSetPreferencesKey(key.name)] = value as Set<String>
        else -> error("不支持的 DataStore 值类型：${value::class}")
    }
}

class AssetCalendarRepository(private val context: Context) : CalendarRepository {
    override fun years(): List<CalendarYear> = context.assets.list("calendar").orEmpty()
        .filter { it.endsWith(".json") }
        .map { file ->
            val json = context.assets.open("calendar/$file").bufferedReader().use { JSONObject(it.readText()) }
            val days = json.getJSONObject("days")
            CalendarYear(
                json.getInt("year"),
                json.getString("calendarVersion"),
                json.getString("calendarLastUpdated"),
                days.keys().asSequence().associate { LocalDate.parse(it) to WorkDayType.valueOf(days.getString(it)) },
            )
        }
}

@Entity(tableName = "wishes")
data class LegacyWishEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: String,
    val remark: String,
    val status: String,
    val createdAt: String,
    val achievedAt: String?,
)

@Dao
interface LegacyWishDao {
    @Query("SELECT * FROM wishes ORDER BY createdAt DESC, id DESC")
    fun observe(): Flow<List<LegacyWishEntity>>
}

@Database(entities = [LegacyWishEntity::class], version = 1, exportSchema = true)
abstract class LegacyDatabase : RoomDatabase() {
    abstract fun wishes(): LegacyWishDao
}
