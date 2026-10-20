package com.qingqian.chonglema.data

import android.content.Context
import com.qingqian.chonglema.model.CheckInRecord
import com.qingqian.chonglema.model.OUTCOME_DONE
import com.qingqian.chonglema.model.OUTCOME_RESIST
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 全局数据仓库：读写数据库并把最新记录推给 UI。
 *
 * 个人自用 App，记录量很小，直接整表读入内存，统计都在内存里算。
 * 写操作只做内存增量更新，不再整表重读，避免每次打卡都查一遍数据库。
 */
class CheckInStore(context: Context) {

    private val dao = CheckInDao(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _records = MutableStateFlow<List<CheckInRecord>>(emptyList())
    val records: StateFlow<List<CheckInRecord>> = _records.asStateFlow()

    /** 首次从数据库读完之前为 false，闪屏会一直挂在上面。 */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            _records.value = dao.getAll()
            _loaded.value = true
        }
    }

    fun addRecord(date: LocalDate, time: LocalTime, type: String, note: String) {
        scope.launch {
            val record = CheckInRecord(
                date = date.format(ISO_DATE),
                time = time.format(HOUR_MINUTE),
                type = type,
                note = note.trim(),
                timestamp = timestampOf(date, time),
                outcome = OUTCOME_DONE,
            )
            val id = dao.insert(record)
            _records.value = insertSorted(_records.value, record.copy(id = id))
        }
    }

    /** 记录一次「忍住了」（欲望来了但没冲）。 */
    fun addResist(date: LocalDate, time: LocalTime) {
        scope.launch {
            val record = CheckInRecord(
                date = date.format(ISO_DATE),
                time = time.format(HOUR_MINUTE),
                type = "忍住",
                note = "",
                timestamp = timestampOf(date, time),
                outcome = OUTCOME_RESIST,
            )
            val id = dao.insert(record)
            _records.value = insertSorted(_records.value, record.copy(id = id))
        }
    }

    fun updateRecord(id: Long, date: LocalDate, time: LocalTime, type: String, note: String) {
        scope.launch {
            val timestamp = timestampOf(date, time)
            dao.update(
                id = id,
                time = time.format(HOUR_MINUTE),
                type = type,
                note = note.trim(),
                timestamp = timestamp,
            )
            _records.value = _records.value.map { old ->
                if (old.id == id) {
                    old.copy(time = time.format(HOUR_MINUTE), type = type, note = note.trim(), timestamp = timestamp)
                } else {
                    old
                }
            }
        }
    }

    fun deleteRecord(id: Long) {
        scope.launch {
            dao.delete(id)
            _records.value = _records.value.filterNot { it.id == id }
        }
    }

    fun clearDate(date: LocalDate) {
        scope.launch {
            val dateText = date.format(ISO_DATE)
            dao.deleteByDate(dateText)
            _records.value = _records.value.filterNot { it.date == dateText }
        }
    }

    /** 导入一批记录（外部已去重），整批写入后刷新。 */
    fun importRecords(newRecords: List<CheckInRecord>) {
        if (newRecords.isEmpty()) return
        scope.launch {
            dao.insertAll(newRecords)
            // 批量导入没有逐条自增 id 可拿，重读一次表最省事，也不在热路径上
            _records.value = dao.getAll()
        }
    }

    fun clearAll() {
        scope.launch {
            dao.deleteAll()
            _records.value = emptyList()
        }
    }

    /** 按 timestamp 插入到正确位置，保持与数据库查询（timestamp ASC）一致的顺序。 */
    private fun insertSorted(
        current: List<CheckInRecord>,
        record: CheckInRecord,
    ): List<CheckInRecord> {
        val result = ArrayList<CheckInRecord>(current.size + 1)
        var added = false
        for (item in current) {
            if (!added && item.timestamp > record.timestamp) {
                result.add(record)
                added = true
            }
            result.add(item)
        }
        if (!added) result.add(record)
        return result
    }

    private fun timestampOf(date: LocalDate, time: LocalTime): Long =
        LocalDateTime.of(date, time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private companion object {
        val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
