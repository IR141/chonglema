package com.qingqian.chonglema.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/** 自律目标的周期。 */
enum class GoalPeriod { DAY, WEEK, MONTH, CUSTOM }

/**
 * 自律目标：period + target（最小 1 次，最大 999 次）；自定义周期用 start / end。
 * enabled 单独记录「有没有开启目标」，没开启时 target 只是草稿值。
 */
data class GoalConfig(
    val period: GoalPeriod = GoalPeriod.DAY,
    val target: Int = 1,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val enabled: Boolean = false,
) {
    val isSet: Boolean get() = enabled
}

/** 用户偏好设置：自律目标、启动标题索引等，与主题设置共用同一个 SharedPreferences 文件。 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("chonglema_settings", Context.MODE_PRIVATE)

    private val _goal = MutableStateFlow(loadGoal())
    val goal: StateFlow<GoalConfig> = _goal.asStateFlow()

    fun setGoal(config: GoalConfig) {
        val safe = config.copy(target = config.target.coerceIn(1, MAX_TARGET))
        prefs.edit()
            .putString(KEY_GOAL_PERIOD, safe.period.name)
            .putInt(KEY_GOAL_TARGET, safe.target)
            .putString(KEY_GOAL_START, safe.start?.toString())
            .putString(KEY_GOAL_END, safe.end?.toString())
            .putBoolean(KEY_GOAL_ENABLED, safe.enabled)
            .apply()
        _goal.value = safe
    }

    /** 冷启动时取一句标题，并把索引往后挪一位，下次冷启动换下一句。 */
    fun nextLaunchTitle(): String {
        val index = prefs.getInt(KEY_TITLE_INDEX, 0)
        val safeIndex = Math.floorMod(index, TITLE_SENTENCES.size)
        prefs.edit().putInt(KEY_TITLE_INDEX, (safeIndex + 1) % TITLE_SENTENCES.size).apply()
        return TITLE_SENTENCES[safeIndex]
    }

    private fun loadGoal(): GoalConfig {
        val period = runCatching {
            GoalPeriod.valueOf(prefs.getString(KEY_GOAL_PERIOD, null) ?: GoalPeriod.DAY.name)
        }.getOrDefault(GoalPeriod.DAY)
        val rawTarget = prefs.getInt(KEY_GOAL_TARGET, 1)
        val target = rawTarget.coerceIn(1, MAX_TARGET)
        val start = prefs.getString(KEY_GOAL_START, null)?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }
        val end = prefs.getString(KEY_GOAL_END, null)?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }
        // 老版本没有 enabled 字段：只有确实存过目标（target > 0）才继续当作已开启。
        // 全新安装没有 key，默认是「未开启」；旧版本保存过 0 次目标且 enabled=true 的，
        // target 会被下面的最小 1 次修正。
        val enabled = if (prefs.contains(KEY_GOAL_ENABLED)) {
            prefs.getBoolean(KEY_GOAL_ENABLED, false)
        } else {
            prefs.contains(KEY_GOAL_TARGET) && rawTarget > 0
        }
        return GoalConfig(period, target, start, end, enabled)
    }

    companion object {
        const val MAX_TARGET = 999
        private const val KEY_GOAL_PERIOD = "goal_period"
        private const val KEY_GOAL_TARGET = "goal_target"
        private const val KEY_GOAL_START = "goal_start"
        private const val KEY_GOAL_END = "goal_end"
        private const val KEY_GOAL_ENABLED = "goal_enabled"
        private const val KEY_TITLE_INDEX = "launch_title_index"

        /** 顶栏标题：每次冷启动换下一句。 */
        val TITLE_SENTENCES = listOf(
            "记录每一次，了解自己",
            "克制，是自由的开始",
            "今天也要做个自律的人",
            "别让冲动替你做决定",
            "每一次记录都算数",
            "身体是革命的本钱",
            "慢慢来，比较快",
            "你比欲望更强大",
            "对自己温柔一点",
            "冲动之前先深呼吸",
            "少一点失控，多一点清醒",
            "记录，是为了更好出发",
        )
    }
}
