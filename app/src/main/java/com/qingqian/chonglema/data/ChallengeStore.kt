package com.qingqian.chonglema.data

import android.content.Context
import android.content.SharedPreferences
import com.qingqian.chonglema.stats.ChallengeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** 挑战（连续几天没冲）持久化：只存开始日期和天数。 */
class ChallengeStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chonglema_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<ChallengeState?> = _state

    private fun load(): ChallengeState? {
        val days = prefs.getInt(KEY_DAYS, 0)
        val start = prefs.getString(KEY_START, null) ?: return null
        if (days <= 0) return null
        val date = try {
            LocalDate.parse(start)
        } catch (_: Exception) {
            return null
        }
        return ChallengeState(days = days, start = date)
    }

    fun start(days: Int) {
        prefs.edit()
            .putInt(KEY_DAYS, days)
            .putString(KEY_START, LocalDate.now().toString())
            .apply()
        _state.value = load()
    }

    fun cancel() {
        prefs.edit().remove(KEY_DAYS).remove(KEY_START).apply()
        _state.value = null
    }

    private companion object {
        const val KEY_DAYS = "challenge_days"
        const val KEY_START = "challenge_start"
    }
}
