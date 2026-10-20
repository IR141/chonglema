package com.qingqian.chonglema.data

import com.qingqian.chonglema.model.CheckInRecord
import com.qingqian.chonglema.model.OUTCOME_DONE
import com.qingqian.chonglema.model.OUTCOME_RESIST
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** 一次导入的结果：新增了多少条、跳过了多少条重复/无效记录。 */
data class ImportResult(val added: Int, val skipped: Int)

/**
 * JSON 备份的读写：整库导出成一个可读的 JSON 文件，导入时按内容去重。
 */
object BackupManager {

    const val APP_TAG = "chonglema"
    const val FORMAT_VERSION = 1

    fun toJson(records: List<CheckInRecord>): String {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject().apply {
                    put("date", record.date)
                    put("time", record.time)
                    put("type", record.type)
                    put("note", record.note)
                    put("timestamp", record.timestamp)
                    put("outcome", record.outcome)
                }
            )
        }
        return JSONObject().apply {
            put("app", APP_TAG)
            put("format", FORMAT_VERSION)
            put("exportedAt", LocalDateTime.now().toString())
            put("count", records.size)
            put("records", array)
        }.toString(2)
    }

    /** 解析备份文件；不是本 App 的备份时返回 null。 */
    fun parse(json: String): List<CheckInRecord>? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val array = root.optJSONArray("records") ?: return null
        val result = mutableListOf<CheckInRecord>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val date = obj.optString("date").trim()
            val time = obj.optString("time").trim()
            if (date.isEmpty()) continue
            val outcome = if (obj.optString("outcome") == OUTCOME_RESIST) OUTCOME_RESIST else OUTCOME_DONE
            val type = obj.optString("type").ifBlank {
                if (outcome == OUTCOME_RESIST) "忍住" else "自慰"
            }
            val note = obj.optString("note")
            val timestamp = obj.optLong("timestamp", 0L)
                .takeIf { it > 0L }
                ?: timestampOf(date, time)
                ?: continue
            result.add(
                CheckInRecord(
                    date = date,
                    time = time,
                    type = type,
                    note = note,
                    timestamp = timestamp,
                    outcome = outcome,
                )
            )
        }
        return result
    }

    /** 过滤掉已经存在的记录（按 日期+时间+类型+结果+备注 判定重复），返回新增列表与跳过条数。 */
    fun dedupe(existing: List<CheckInRecord>, imported: List<CheckInRecord>): Pair<List<CheckInRecord>, Int> {
        val seen = existing.mapTo(mutableSetOf()) { key(it) }
        val fresh = mutableListOf<CheckInRecord>()
        var skipped = 0
        imported.forEach { record ->
            if (seen.add(key(record))) fresh.add(record) else skipped++
        }
        return fresh to skipped
    }

    private fun key(record: CheckInRecord): String =
        record.date + "|" + record.time + "|" + record.type + "|" + record.outcome + "|" + record.note

    private fun timestampOf(date: String, time: String): Long? = runCatching {
        LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time.take(5)))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}
