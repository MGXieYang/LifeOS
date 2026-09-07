package com.lifeos.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import com.lifeos.domain.*
import com.lifeos.domain.calendar.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private val Context.settingsStore by preferencesDataStore("user_settings")
class SettingsRepository(private val context: Context) {
    private object Keys {
        val salary = stringPreferencesKey("salary")
        val start = stringPreferencesKey("start")
        val lunchStart = stringPreferencesKey("lunch_start")
        val lunchEnd = stringPreferencesKey("lunch_end")
        val end = stringPreferencesKey("end")
        val lunchEnabled = booleanPreferencesKey("lunch_enabled")
        val payday = intPreferencesKey("payday")
        val birth = stringPreferencesKey("birth")
        val type = stringPreferencesKey("retirement_type")
        val freeEnabled = booleanPreferencesKey("free_enabled")
        val sleep = intPreferencesKey("sleep_minutes")
        val commute = intPreferencesKey("commute_minutes")
        val necessaryEnabled = booleanPreferencesKey("necessary_enabled")
        val necessary = intPreferencesKey("necessary_minutes")
        val animations = booleanPreferencesKey("animations")
        val funMode = booleanPreferencesKey("fun_mode")
        val currency = stringPreferencesKey("currency")
        val decimals = intPreferencesKey("money_decimals")
        val onboarded = booleanPreferencesKey("onboarded")
    }
    val settings: Flow<UserSettings> = context.settingsStore.data.map { p ->
        UserSettings(
            monthlySalary=BigDecimal(p[Keys.salary] ?: "10000.00"),
            workStart=LocalTime.parse(p[Keys.start] ?: "09:30"),
            lunchStart=LocalTime.parse(p[Keys.lunchStart] ?: "12:30"),
            lunchEnd=LocalTime.parse(p[Keys.lunchEnd] ?: "14:00"),
            workEnd=LocalTime.parse(p[Keys.end] ?: "19:00"),
            lunchBreakEnabled=p[Keys.lunchEnabled] ?: true,
            salaryDay=p[Keys.payday] ?: 7,
            birthDate=p[Keys.birth]?.let(LocalDate::parse),
            retirementType=RetirementType.valueOf(p[Keys.type] ?: "MALE"),
            freeTimeEnabled=p[Keys.freeEnabled] ?: false,
            sleepMinutes=p[Keys.sleep] ?: 480,
            commuteMinutes=p[Keys.commute] ?: 0,
            necessaryLifeEnabled=p[Keys.necessaryEnabled] ?: true,
            necessaryLifeMinutes=p[Keys.necessary] ?: 120,
            animationsEnabled=p[Keys.animations] ?: true,
            funModeEnabled=p[Keys.funMode] ?: true,
            currencySymbol=p[Keys.currency] ?: "¥",
            moneyDecimals=p[Keys.decimals] ?: 2,
            onboarded=p[Keys.onboarded] ?: false,
        )
    }
    suspend fun save(s: UserSettings) {
        context.settingsStore.edit { p ->
            p[Keys.salary]=s.monthlySalary.money()
            p[Keys.start]=s.workStart.toString(); p[Keys.end]=s.workEnd.toString()
            p[Keys.lunchStart]=s.lunchStart.toString(); p[Keys.lunchEnd]=s.lunchEnd.toString()
            p[Keys.lunchEnabled]=s.lunchBreakEnabled
            p[Keys.payday]=s.salaryDay; p[Keys.type]=s.retirementType.name
            p[Keys.onboarded]=s.onboarded
            p[Keys.freeEnabled]=s.freeTimeEnabled; p[Keys.sleep]=s.sleepMinutes; p[Keys.commute]=s.commuteMinutes
            p[Keys.necessaryEnabled]=s.necessaryLifeEnabled; p[Keys.necessary]=s.necessaryLifeMinutes
            p[Keys.animations]=s.animationsEnabled; p[Keys.funMode]=s.funModeEnabled
            p[Keys.currency]=s.currencySymbol; p[Keys.decimals]=s.moneyDecimals
            if(s.birthDate == null) p.remove(Keys.birth) else p[Keys.birth]=s.birthDate.toString()
        }
    }
}
class AssetCalendarRepository(private val context: Context): CalendarRepository {
    override fun years(): List<CalendarYear> = context.assets.list("calendar").orEmpty()
        .filter { it.endsWith(".json") }.map { file ->
            val json = context.assets.open("calendar/$file").bufferedReader().use { JSONObject(it.readText()) }
            val days = json.getJSONObject("days")
            CalendarYear(json.getInt("year"), json.getString("calendarVersion"), json.getString("calendarLastUpdated"),
                days.keys().asSequence().associate { LocalDate.parse(it) to WorkDayType.valueOf(days.getString(it)) })
        }
}
@Entity(tableName="wishes")
data class WishEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val name: String, val price: String, val remark: String,
    val status: String, val createdAt: String, val achievedAt: String?,
) {
    fun toDomain() = WishItem(id,name,BigDecimal(price),remark,WishStatus.valueOf(status),
        LocalDateTime.parse(createdAt),achievedAt?.let(LocalDateTime::parse))
    companion object {
        fun from(item: WishItem) = WishEntity(item.id,item.name,item.price.money(),item.remark,item.status.name,
            item.createdAt.toString(),item.achievedAt?.toString())
    }
}
@Dao
interface WishDao {
    @Query("SELECT * FROM wishes ORDER BY createdAt DESC, id DESC") fun observe(): Flow<List<WishEntity>>
    @Upsert suspend fun upsert(entity: WishEntity)
    @Query("DELETE FROM wishes WHERE id = :id") suspend fun delete(id: Long)
}
@Database(entities=[WishEntity::class], version=1, exportSchema=true)
abstract class LegacyDatabase: RoomDatabase() { abstract fun wishes(): WishDao }
class WishRepository(private val dao: WishDao) {
    val wishes = dao.observe().map { list -> list.map(WishEntity::toDomain) }
    suspend fun save(item: WishItem) = dao.upsert(WishEntity.from(item))
    suspend fun delete(id: Long) = dao.delete(id)
}
