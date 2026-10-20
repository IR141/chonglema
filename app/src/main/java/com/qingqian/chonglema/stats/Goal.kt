package com.qingqian.chonglema.stats

import com.qingqian.chonglema.data.GoalConfig
import com.qingqian.chonglema.data.GoalPeriod
import com.qingqian.chonglema.model.CheckInRecord
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** 自律目标在当前周期的完成情况。 */
data class GoalProgress(
    val config: GoalConfig,
    val label: String,
    val start: LocalDate,
    val end: LocalDate,
    val doneCount: Int,
) {
    val target: Int get() = config.target
    val remaining: Int get() = (target - doneCount).coerceAtLeast(0)
    // 目标 0 次也是有效目标：一次都没记算达成，记了任何一次都算超
    val over: Boolean get() = doneCount > target
    val reached: Boolean get() = doneCount >= target
    val progress: Float
        get() = if (target <= 0) 1f else (doneCount.toFloat() / target).coerceIn(0f, 1f)

    /** 周期文案，如「今天」「本周」「10/20–11/20」。 */
    val periodLabel: String get() = label
}

/** 目标对应的日期区间；未设置目标返回 null。 */
fun goalDateRange(config: GoalConfig, today: LocalDate): Pair<LocalDate, LocalDate>? {
    return when (config.period) {
        GoalPeriod.DAY -> today to today
        GoalPeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to
            today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        GoalPeriod.MONTH -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
        GoalPeriod.CUSTOM -> {
            val start = config.start ?: today
            val end = config.end ?: start
            if (end.isBefore(start)) end to start else start to end
        }
    }
}

fun goalPeriodLabel(config: GoalConfig, start: LocalDate, end: LocalDate): String {
    return when (config.period) {
        GoalPeriod.DAY -> "今天"
        GoalPeriod.WEEK -> "本周"
        GoalPeriod.MONTH -> "本月"
        GoalPeriod.CUSTOM -> start.monthValue.toString() + "/" + start.dayOfMonth +
            "–" + end.monthValue + "/" + end.dayOfMonth
    }
}

fun computeGoalProgress(
    records: List<CheckInRecord>,
    config: GoalConfig,
    today: LocalDate = LocalDate.now(),
): GoalProgress? {
    if (!config.isSet) return null
    val (start, end) = goalDateRange(config, today) ?: return null
    val doneCount = records.count { record ->
        if (record.isResist) return@count false
        val date = parseDate(record.date) ?: return@count false
        !date.isBefore(start) && !date.isAfter(end)
    }
    return GoalProgress(
        config = config,
        label = goalPeriodLabel(config, start, end),
        start = start,
        end = end,
        doneCount = doneCount,
    )
}
