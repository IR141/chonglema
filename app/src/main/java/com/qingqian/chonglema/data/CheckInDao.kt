package com.qingqian.chonglema.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.qingqian.chonglema.model.CheckInRecord

/**
 * 打卡记录的数据访问对象，全部为阻塞调用，需在 IO 线程执行。
 */
class CheckInDao(context: Context) {

    private val helper = CheckInDb(context)

    fun getAll(): List<CheckInRecord> =
        helper.readableDatabase
            .query(CheckInDb.TABLE, null, null, null, null, null, "timestamp ASC")
            .use { cursor -> cursor.readAll() }

    fun getByDate(date: String): List<CheckInRecord> =
        helper.readableDatabase
            .query(CheckInDb.TABLE, null, "date = ?", arrayOf(date), null, null, "timestamp ASC")
            .use { cursor -> cursor.readAll() }

    fun insert(record: CheckInRecord): Long {
        val values = ContentValues().apply {
            put("date", record.date)
            put("time", record.time)
            put("type", record.type)
            put("note", record.note)
            put("timestamp", record.timestamp)
            put("outcome", record.outcome)
        }
        return helper.writableDatabase.insertOrThrow(CheckInDb.TABLE, null, values)
    }

    fun insertAll(records: List<CheckInRecord>) {
        if (records.isEmpty()) return
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            records.forEach { insert(it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun update(id: Long, time: String, type: String, note: String, timestamp: Long): Int {
        val values = ContentValues().apply {
            put("time", time)
            put("type", type)
            put("note", note)
            put("timestamp", timestamp)
        }
        return helper.writableDatabase.update(CheckInDb.TABLE, values, "id = ?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        helper.writableDatabase.delete(CheckInDb.TABLE, "id = ?", arrayOf(id.toString()))
    }

    fun deleteByDate(date: String) {
        helper.writableDatabase.delete(CheckInDb.TABLE, "date = ?", arrayOf(date))
    }

    fun deleteAll() {
        helper.writableDatabase.delete(CheckInDb.TABLE, null, null)
    }

    private fun Cursor.readAll(): List<CheckInRecord> = buildList {
        while (moveToNext()) {
            add(
                CheckInRecord(
                    id = getLong(getColumnIndexOrThrow("id")),
                    date = getString(getColumnIndexOrThrow("date")),
                    time = getString(getColumnIndexOrThrow("time")),
                    type = getString(getColumnIndexOrThrow("type")),
                    note = getString(getColumnIndexOrThrow("note")),
                    timestamp = getLong(getColumnIndexOrThrow("timestamp")),
                    outcome = getString(getColumnIndexOrThrow("outcome")),
                )
            )
        }
    }
}
