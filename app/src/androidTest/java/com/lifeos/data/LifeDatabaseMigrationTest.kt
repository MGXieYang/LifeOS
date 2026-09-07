package com.lifeos.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LifeDatabaseMigrationTest {
    private val databaseName = "v3-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LifeDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrationOneToTwoPreservesBalanceAndNodesAndDropsDecisionTables() {
        helper.createDatabase(databaseName, 1).apply {
            execSQL(
                """INSERT INTO balance_items
                    (id,name,price,category,remark,status,isPinned,usageMonths,frequencyType,frequencyValue,oneTimeExtra,monthlyHolding,annualHolding,createdAt,updatedAt,purchasedAt)
                    VALUES (1,'耳机','899','DIGITAL','','CONSIDERING',0,24,'DAILY','1','0','0','0','2026-09-07T12:00','2026-09-07T12:00',NULL)""",
            )
            execSQL("INSERT INTO life_nodes (id,title,type,targetAge,targetDate,createdAt) VALUES (2,'四十岁','AGE',40,NULL,'2026-09-07T12:00')")
            execSQL("INSERT INTO decisions (id,title,category,background,initialPreference,customPreference,confidence,mainReason,biggestRisk,expectedResult,status,finalDecision,createdAt,decidedAt,reviewDate) VALUES (3,'旧决策',NULL,'','UNDECIDED','',NULL,'','','','THINKING','','2026-09-07T12:00',NULL,NULL)")
            close()
        }

        val db = helper.runMigrationsAndValidate(databaseName, 2, true, MIGRATION_1_2)
        db.query("SELECT name, price, usageMonths FROM balance_history WHERE id=1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("耳机", cursor.getString(0))
            assertEquals("899", cursor.getString(1))
            assertEquals(24, cursor.getInt(2))
        }
        db.query("SELECT title FROM life_nodes WHERE id=2").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("四十岁", cursor.getString(0))
        }
        db.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            val names = buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
            assertFalse("decisions" in names)
            assertFalse("decision_reviews" in names)
        }
        db.close()
    }
}

