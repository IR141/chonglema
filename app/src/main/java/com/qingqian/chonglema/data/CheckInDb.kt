package com.qingqian.chonglema.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 本地 SQLite 数据库，只存一张打卡表。
 */
class CheckInDb(context: Context) : SQLiteOpenHelper(context.applicationContext, NAME, null, VERSION) {

    init {
        // WAL：单条小事务写入不必等 fsync 落盘就能返回，读写也能并行
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                time TEXT NOT NULL,
                type TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                timestamp INTEGER NOT NULL,
                outcome TEXT NOT NULL DEFAULT 'DONE'
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_records_date ON $TABLE(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_records_ts ON $TABLE(timestamp)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 -> v2：新增「冲了 / 忍住了」结果列，老记录一律视为冲了
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN outcome TEXT NOT NULL DEFAULT 'DONE'")
        }
    }

    companion object {
        const val NAME = "chonglema.db"
        const val VERSION = 2
        const val TABLE = "records"
    }
}
