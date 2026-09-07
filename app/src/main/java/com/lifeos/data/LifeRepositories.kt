package com.lifeos.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifeos.domain.balance.BalanceItem
import com.lifeos.domain.balance.UsageFrequencyType
import com.lifeos.domain.lifetime.LifeNode
import com.lifeos.domain.lifetime.LifeNodeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "balance_history")
data class BalanceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val price: String,
    val usageMonths: Int?,
    val frequencyType: String?,
    val frequencyValue: String?,
    val createdAt: String,
) {
    fun domain(): BalanceItem {
        val rawFrequency = frequencyType
        val legacyYearly = rawFrequency == "YEARLY"
        val migratedFrequency = when {
            legacyYearly -> UsageFrequencyType.MONTHLY
            rawFrequency == null -> null
            else -> runCatching { UsageFrequencyType.valueOf(rawFrequency) }.getOrNull()
        }
        val migratedValue = frequencyValue?.let(::BigDecimal)?.let {
            if (legacyYearly) it.divide(BigDecimal("12"), java.math.MathContext.DECIMAL128) else it
        }
        return BalanceItem(
            id = id,
            name = name.orEmpty(),
            price = BigDecimal(price),
            expectedUsagePeriodMonths = usageMonths,
            usageFrequencyType = migratedFrequency,
            usageFrequencyValue = migratedValue,
            createdAt = LocalDateTime.parse(createdAt),
        )
    }

    companion object {
        fun of(x: BalanceItem) = BalanceHistoryEntity(
            id = x.id,
            name = x.name.takeIf(String::isNotBlank),
            price = x.price.toPlainString(),
            usageMonths = x.expectedUsagePeriodMonths,
            frequencyType = x.usageFrequencyType?.name,
            frequencyValue = x.usageFrequencyValue?.toPlainString(),
            createdAt = x.createdAt.toString(),
        )
    }
}

@Entity(tableName = "life_nodes")
data class LifeNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String,
    val targetAge: Int?,
    val targetDate: String?,
    val createdAt: String,
) {
    fun domain() = LifeNode(id, title, LifeNodeType.valueOf(type), targetAge, targetDate?.let(LocalDate::parse), LocalDateTime.parse(createdAt))

    companion object {
        fun of(x: LifeNode) = LifeNodeEntity(x.id, x.title, x.type.name, x.targetAge, x.targetDate?.toString(), x.createdAt.toString())
    }
}

@Dao
interface LifeDao {
    @Query("SELECT * FROM balance_history ORDER BY createdAt DESC, id DESC")
    fun balances(): Flow<List<BalanceHistoryEntity>>

    @Query("SELECT * FROM life_nodes ORDER BY createdAt")
    fun nodes(): Flow<List<LifeNodeEntity>>

    @Upsert suspend fun saveBalance(x: BalanceHistoryEntity): Long
    @Query("DELETE FROM balance_history WHERE id = :id") suspend fun deleteBalance(id: Long)
    @Upsert suspend fun saveNode(x: LifeNodeEntity): Long
    @Query("DELETE FROM life_nodes WHERE id = :id") suspend fun deleteNode(id: Long)
}

@Database(entities = [BalanceHistoryEntity::class, LifeNodeEntity::class], version = 2, exportSchema = true)
abstract class LifeDatabase : RoomDatabase() {
    abstract fun dao(): LifeDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS balance_history_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT,
                price TEXT NOT NULL,
                usageMonths INTEGER,
                frequencyType TEXT,
                frequencyValue TEXT,
                createdAt TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO balance_history_new (id, name, price, usageMonths, frequencyType, frequencyValue, createdAt)
            SELECT id, NULLIF(name, ''), price, usageMonths, frequencyType, frequencyValue, createdAt
            FROM balance_items
        """.trimIndent())
        db.execSQL("DROP TABLE balance_items")
        db.execSQL("ALTER TABLE balance_history_new RENAME TO balance_history")
        db.execSQL("DROP TABLE decision_reviews")
        db.execSQL("DROP TABLE decisions")
    }
}

class LifeRepository(db: LifeDatabase) {
    private val dao = db.dao()
    val balances = dao.balances().map { rows -> rows.map(BalanceHistoryEntity::domain) }
    val nodes = dao.nodes().map { rows -> rows.map(LifeNodeEntity::domain) }

    suspend fun save(x: BalanceItem) = dao.saveBalance(BalanceHistoryEntity.of(x))
    suspend fun deleteBalance(id: Long) = dao.deleteBalance(id)
    suspend fun save(x: LifeNode) = dao.saveNode(LifeNodeEntity.of(x))
    suspend fun deleteNode(id: Long) = dao.deleteNode(id)
}
