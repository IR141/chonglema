package com.qingqian.chonglema

import android.app.Application
import com.qingqian.chonglema.data.CrashLog

class ChonglemaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}
