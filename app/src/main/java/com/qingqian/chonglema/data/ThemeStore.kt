package com.qingqian.chonglema.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 主题模式：跟随系统 / 白天 / 夜间。 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 用 SharedPreferences 持久化主题选择，并把当前值暴露成 StateFlow。 */
class ThemeStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(readMode())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        if (mode == _mode.value) return
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _mode.value = mode
    }

    private fun readMode(): ThemeMode {
        val raw = prefs.getString(KEY_MODE, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    companion object {
        private const val PREFS_NAME = "chonglema_settings"
        private const val KEY_MODE = "theme_mode"
    }
}
