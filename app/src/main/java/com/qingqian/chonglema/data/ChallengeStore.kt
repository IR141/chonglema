package com.qingqian.chonglema.data

import android.content.Context
import com.qingqian.chonglema.stats.ChallengeAttempt
import com.qingqian.chonglema.stats.challengeKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 挑战只需要存「尝试列表」：每次开始的时间 + 天数。
 * 成功/失败永远由记录实时算，所以删掉一条「冲了」记录，挑战就会自动复活。
 */
class ChallengeStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("chonglema_settings", Context.MODE_PRIVATE)

    private val _attempts = MutableStateFlow(load())
    val attempts: StateFlow<List<ChallengeAttempt>> = _attempts

    fun start(
        days: Int,
        startAt: LocalDateTime = LocalDateTime.now(),
        startRecordId: Long? = null,
    ) {
        val safeDays = days.coerceIn(0, 999)
        // 不能把开始时间截到分钟：同一分钟内点「重新开始」时，新开始时刻会早于
        // 那条导致失败的记录，Day 0 会立刻再次判失败，看起来像按钮没反应。
        // 开始时刻保留完整精度，同时记下记录 id 水位，用来判断开始当天同一分钟内
        // 的记录是开始前就有的、还是开始后才写进去的。
        val next = (_attempts.value + ChallengeAttempt(startAt, safeDays, startRecordId))
            .takeLast(MAX_ATTEMPTS)
        _attempts.value = next
        save(next)
    }

    /** 放弃 / 移除当前挑战：不删除记录，只标记成「已放弃」保留在挑战记录里。 */
    fun cancel() {
        val list = _attempts.value
        val index = list.indexOfLast { !it.cancelled }
        if (index < 0) return
        val next = list.toMutableList()
        next[index] = next[index].copy(cancelled = true)
        _attempts.value = next
        save(next)
    }

    /** 从「挑战记录」里删掉一条历史挑战；不会动任何打卡记录。 */
    fun delete(attempt: ChallengeAttempt) {
        val next = _attempts.value.filterNot { it == attempt }
        if (next.size == _attempts.value.size) return
        _attempts.value = next
        save(next)
    }

    fun celebratedKeys(): Set<String> {
        val raw = prefs.getString(KEY_CELEBRATED, null) ?: return emptySet()
        return runCatching {
            val array = JSONArray(raw)
            val result = HashSet<String>(array.length())
            for (i in 0 until array.length()) {
                array.optString(i).takeIf { it.isNotBlank() }?.let { result.add(it) }
            }
            result
        }.getOrDefault(emptySet())
    }

    /**
     * 首页是否隐藏挑战信息：只要当前没有「进行中」的挑战（没有 / 已失败 / 已成功），
     * 下次冷启动时首页大卡右上角就不显示挑战信息；开始新挑战会把它重置成 false。
     */
    fun homeChallengeHidden(): Boolean = prefs.getBoolean(KEY_HOME_HIDDEN_FLAG, false)

    fun setHomeChallengeHidden(hidden: Boolean) {
        prefs.edit().putBoolean(KEY_HOME_HIDDEN_FLAG, hidden).apply()
    }

    fun markCelebrated(attempt: ChallengeAttempt) {
        val next = (celebratedKeys() + challengeKey(attempt)).toList().takeLast(MAX_CELEBRATED)
        val array = JSONArray()
        next.forEach { array.put(it) }
        prefs.edit().putString(KEY_CELEBRATED, array.toString()).apply()
    }

    private fun load(): List<ChallengeAttempt> {
        val raw = prefs.getString(KEY_ATTEMPTS, null)
        if (!raw.isNullOrBlank()) {
            runCatching {
                val array = JSONArray(raw)
                val result = ArrayList<ChallengeAttempt>(array.length())
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val startAt = item.optString("startAt").takeIf { it.isNotBlank() }
                        ?.let { runCatching { LocalDateTime.parse(it, DATE_TIME_FORMATTER) }.getOrNull() }
                        ?: continue
                    val days = item.optInt("days", 0)
                    val recordId = if (item.has("startRecordId")) item.optLong("startRecordId") else null
                    val cancelled = item.optBoolean("cancelled", false)
                    if (days >= 0) {
                        result.add(ChallengeAttempt(startAt, days.coerceIn(0, 999), recordId, cancelled))
                    }
                }
                return result.takeLast(MAX_ATTEMPTS)
            }
        }
        return legacyAttempt()
    }

    /** 老版本只存了「天数 + 开始日期」，读出来当作当天 0 点开始的挑战。 */
    private fun legacyAttempt(): List<ChallengeAttempt> {
        val days = prefs.getInt(KEY_OLD_DAYS, 0)
        val startText = prefs.getString(KEY_OLD_START, null)
        if (days <= 0 || startText.isNullOrBlank()) return emptyList()
        val date = runCatching { LocalDate.parse(startText) }.getOrNull() ?: return emptyList()
        return listOf(ChallengeAttempt(date.atStartOfDay(), days))
    }

    private fun save(list: List<ChallengeAttempt>) {
        val array = JSONArray()
        list.forEach { attempt ->
            array.put(
                JSONObject().apply {
                    put("startAt", attempt.startAt.format(DATE_TIME_FORMATTER))
                    put("days", attempt.days)
                    attempt.startRecordId?.let { put("startRecordId", it) }
                    if (attempt.cancelled) put("cancelled", true)
                }
            )
        }
        prefs.edit().putString(KEY_ATTEMPTS, array.toString()).apply()
    }

    private companion object {
        const val MAX_ATTEMPTS = 30
        const val MAX_CELEBRATED = 50
        const val KEY_ATTEMPTS = "challenge_attempts"
        const val KEY_CELEBRATED = "challenge_celebrated"
        const val KEY_HOME_HIDDEN_FLAG = "challenge_home_hidden_flag"
        const val KEY_OLD_DAYS = "challenge_days"
        const val KEY_OLD_START = "challenge_start"
        val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }
}
