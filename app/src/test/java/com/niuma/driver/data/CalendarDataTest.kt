package com.niuma.driver.data

import com.niuma.driver.domain.UserSettings
import com.niuma.driver.domain.calendar.*
import com.niuma.driver.domain.salary.SalaryCalculator
import com.niuma.driver.domain.money
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.*

class CalendarDataTest {
    private fun load(year: Int): CalendarYear {
        val json=JSONObject(File("src/main/assets/calendar/$year.json").readText())
        val days=json.getJSONObject("days")
        return CalendarYear(year,json.getString("calendarVersion"),json.getString("calendarLastUpdated"),
            days.keys().asSequence().associate {LocalDate.parse(it) to WorkDayType.valueOf(days.getString(it))})
    }
    @Test fun bundledCalendarMatchesOfficialExamples() {
        val calendar=WorkCalendar(listOf(load(2025),load(2026)))
        assertEquals(22,calendar.workdays(YearMonth.of(2026,9)).size)
        listOf("2025-01-26","2025-02-08","2025-04-27","2025-09-28","2025-10-11",
            "2026-01-04","2026-02-14","2026-02-28","2026-05-09","2026-09-20","2026-10-10").forEach {
            assertTrue(it,calendar.isWorkday(LocalDate.parse(it)))
        }
        listOf("2025-01-28","2025-02-04","2025-06-02","2025-10-08","2026-01-02","2026-02-23","2026-09-25","2026-10-07").forEach {
            assertFalse(it,calendar.isWorkday(LocalDate.parse(it)))
        }
        for(year in 2025..2026) for(month in 1..12) {
            val last=YearMonth.of(year,month).atEndOfMonth().atTime(23,59,59)
            assertEquals("10000.00",SalaryCalculator(calendar).calculate(UserSettings(),last).monthEarned.money())
        }
    }
    @Test fun realOnlineFeedAgreesWithBundled2026OnEveryDate() {
        val json=JSONObject(javaClass.getResource("/holiday-cn-2026.json")!!.readText())
        val online=WorkCalendar(listOf(CalendarFeed.parse(json,2026,"2026-09-07").year))
        val local=WorkCalendar(listOf(load(2026)))
        var date=LocalDate.of(2026,1,1)
        while(date.year==2026) {
            assertEquals(date.toString(),local.isWorkday(date),online.isWorkday(date))
            date=date.plusDays(1)
        }
    }
    private fun feed(): JSONObject {
        val names=listOf("元旦","春节","清明节","劳动节","端午节","中秋节","国庆节")
        val days=JSONArray()
        repeat(21) {i -> days.put(JSONObject().put("date",LocalDate.of(2026,1,1).plusDays(i.toLong()).toString())
            .put("name",names[i%7]).put("isOffDay",i%2==0))}
        return JSONObject().put("year",2026).put("papers",JSONArray().put("https://www.gov.cn/official-notice.htm")).put("days",days)
    }
    @Test fun feedConvertsBooleansAndSeparatesPreviousDecember() {
        val json=feed()
        json.getJSONArray("days").put(JSONObject().put("date","2025-12-31").put("name","元旦").put("isOffDay",false))
        val parsed=CalendarFeed.parse(json,2026,"2026-09-07")
        assertEquals(WorkDayType.HOLIDAY,parsed.year.days[LocalDate.of(2026,1,1)])
        assertEquals(WorkDayType.WORKDAY,parsed.previousDecember[LocalDate.of(2025,12,31)])
        assertFalse(parsed.year.days.containsKey(LocalDate.of(2025,12,31)))
    }
    @Test(expected=IllegalArgumentException::class) fun wrongYearRejected() { CalendarFeed.parse(feed(),2027,"2026-09-07") }
    @Test(expected=IllegalArgumentException::class) fun emptySourcesRejected() { CalendarFeed.parse(feed().put("papers",JSONArray()),2026,"2026-09-07") }
    @Test(expected=IllegalArgumentException::class) fun unofficialSourceRejected() { CalendarFeed.parse(feed().put("papers",JSONArray().put("https://gov.cn.example.com/notice")),2026,"2026-09-07") }
    @Test(expected=IllegalArgumentException::class) fun partialYearRejected() { CalendarFeed.parse(feed().put("days",JSONArray()),2026,"2026-09-07") }
    @Test(expected=IllegalArgumentException::class) fun duplicateDatesRejected() {
        val f=feed();f.getJSONArray("days").put(f.getJSONArray("days").getJSONObject(0))
        CalendarFeed.parse(f,2026,"2026-09-07")
    }
}

