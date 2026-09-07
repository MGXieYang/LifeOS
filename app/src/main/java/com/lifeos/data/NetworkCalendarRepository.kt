package com.lifeos.data

import android.content.Context
import android.util.AtomicFile
import com.lifeos.domain.calendar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.time.LocalDate

/** Only public calendar data leaves/enters this boundary. No settings or wishes are accepted. */
class NetworkCalendarRepository(private val context: Context) {
    private val cache=AtomicFile(File(context.filesDir,"calendar-cache.json"))
    private fun cached(): JSONObject = if(cache.baseFile.exists())
        JSONObject(cache.openRead().bufferedReader().use {it.readText()}) else JSONObject()
    fun load(): WorkCalendar {
        val years=AssetCalendarRepository(context).years().associateBy {it.year}.toMutableMap()
        val stored=cached()
        val parsed=stored.keys().asSequence().sorted().map {year ->
            val entry=stored.getJSONObject(year)
            CalendarFeed.parse(entry.getJSONObject("data"),year.toInt(),entry.getString("updated"))
        }.toList()
        parsed.forEach { feed -> years[feed.year.year]=feed.year }
        // Next year's published arrangements can affect the previous December.
        parsed.forEach {feed -> feed.previousDecember.forEach { (date,type) ->
            years[date.year]?.let {previous -> years[date.year]=previous.copy(days=previous.days+(date to type))}
        }}
        return WorkCalendar(years.values.toList())
    }
    suspend fun update(year: Int): String = withContext(Dispatchers.IO) {
        require(year in 2007..2200)
        val root=fetch(year)
        val stamp=LocalDate.now().toString()
        CalendarFeed.parse(root,year,stamp) // Never replace a good cache with an invalid/incomplete response.
        val existing=cached()
        existing.put(year.toString(),JSONObject().put("data",root).put("updated",stamp))
        // A failure checking the next year does not discard the requested year's successful update.
        val next=runCatching {fetch(year+1).also {CalendarFeed.parse(it,year+1,stamp)}}.getOrNull()
        if(next!=null) existing.put((year+1).toString(),JSONObject().put("data",next).put("updated",stamp))
        val stream=cache.startWrite()
        try {stream.write(existing.toString().toByteArray(Charsets.UTF_8));cache.finishWrite(stream)}
        catch(e: Exception) {cache.failWrite(stream);throw e}
        "$year 年日历已更新并缓存${if(next!=null) "，同时收录 ${year+1} 年" else "；下一年暂未获取到完整数据"}。"
    }
    private fun fetch(year: Int): JSONObject {
        val connection=URL("https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/$year.json").openConnection() as HttpURLConnection
        connection.connectTimeout=10000;connection.readTimeout=10000
        connection.instanceFollowRedirects=false
        try {
            val code=connection.responseCode
            require(code==200) {if(code==404) "$year 年日历尚未发布" else "日历服务器返回 $code"}
            val bytes=connection.inputStream.use {it.readBytesLimited(256*1024)}
            return JSONObject(bytes.toString(Charsets.UTF_8))
        } finally {connection.disconnect()}
    }
}
private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val out=java.io.ByteArrayOutputStream()
    val buffer=ByteArray(8192)
    while(true) {val n=read(buffer);if(n<0)break;require(out.size()+n<=limit) {"日历文件过大"};out.write(buffer,0,n)}
    return out.toByteArray()
}
data class CalendarFeed(val year: CalendarYear,val previousDecember: Map<LocalDate,WorkDayType>) {
    companion object {
        fun parse(json: JSONObject,expectedYear: Int,updated: String): CalendarFeed {
            require(json.getInt("year")==expectedYear) {"日历年份不匹配"}
            val papers=json.getJSONArray("papers")
            require(papers.length()>0) {"尚未包含正式公告来源"}
            for(i in 0 until papers.length()) {
                val uri=URI(papers.getString(i))
                require(uri.scheme in listOf("https","http") && (uri.host=="gov.cn" || uri.host?.endsWith(".gov.cn")==true)) {"日历缺少政府公告来源"}
            }
            val array=json.getJSONArray("days")
            require(array.length() in 20..100) {"年度日历不完整或格式异常"}
            val days=mutableMapOf<LocalDate,WorkDayType>()
            val names=mutableSetOf<String>()
            for(i in 0 until array.length()) {
                val item=array.getJSONObject(i)
                val date=LocalDate.parse(item.getString("date"))
                require(date.year==expectedYear || date.year==expectedYear-1 && date.monthValue==12) {"日历日期越界"}
                require(item.get("isOffDay") is Boolean) {"日历状态格式错误"}
                require(date !in days) {"日历日期重复"}
                days[date]=if(item.getBoolean("isOffDay")) WorkDayType.HOLIDAY else WorkDayType.WORKDAY
                names+=item.getString("name")
            }
            require(listOf("元旦","春节","清明","劳动","端午","中秋","国庆").all {holiday -> names.any {it.contains(holiday)}}) {"尚未包含完整年度节假日，保留原日历"}
            val hash=MessageDigest.getInstance("SHA-256").digest(json.toString().toByteArray()).take(4).joinToString("") {"%02x".format(it)}
            return CalendarFeed(CalendarYear(expectedYear,"online-$hash",updated,days.filterKeys {it.year==expectedYear}),days.filterKeys {it.year!=expectedYear})
        }
    }
}
