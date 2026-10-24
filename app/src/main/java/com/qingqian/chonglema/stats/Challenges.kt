package com.qingqian.chonglema.stats

import com.qingqian.chonglema.model.CheckInRecord
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 一次「连续 N 天不冲」的挑战尝试。
 *
 * 判定口径（v1.1 起）：
 * - 起点是点「开始挑战」的那一刻，而不是当天 0 点；
 * - 开始当天是 Day 0：当天从这一刻起剩下的时间也会被检查，但不计入完整天数；
 * - Day 1..Day N 是开始日之后的 N 个自然日，所以 endDate = startDate + N；
 * - 从 startAt 到 endDate 当天结束，任何一天出现「冲了」都算失败；
 * - 全部守住、并且今天已经过了 endDate，才算成功。
 */
data class ChallengeAttempt(
    val startAt: LocalDateTime,
    val days: Int,
    /**
     * 点「开始挑战」那一刻数据库里最大的记录 id。
     *
     * 记录时间只精确到分钟（时间选择器只到分钟），所以开始当天同一分钟内、
     * 开始之前写入的记录，时间戳会小于开始时刻。用这个 id 水位判断记录
     * 到底是开始前就存在的，还是开始之后才写的。
     * 老版本数据没有这个字段，为 null 时退回只比较时间戳。
     */
    val startRecordId: Long? = null,
    /** 用户主动「放弃 / 移除」过；仍保留在挑战记录里，但不再算当前挑战。 */
    val cancelled: Boolean = false,
) {
    val startDate: LocalDate get() = startAt.toLocalDate()
    val endDate: LocalDate get() = startDate.plusDays(days.toLong())
}

/**
 * 由记录实时算出来的挑战状态。不落库，所以删掉某条「冲了」记录挑战会自动复活。
 */
data class ChallengeProgress(
    val attempt: ChallengeAttempt,
    val dayIndex: Int,      // 0..days，今天进行到第几天
    val cleanDays: Int,     // 已经完整守住的整天数（不含今天）
    val failed: Boolean,
    val failedOn: LocalDate?,
    val completed: Boolean,
) {
    val days: Int get() = attempt.days
    val startDate: LocalDate get() = attempt.startDate
    val endDate: LocalDate get() = attempt.endDate

    val daysLeft: Int get() = (days - dayIndex).coerceAtLeast(0)

    /** 今天就是最后一个需要守住的晚上。 */
    val lastDay: Boolean get() = !failed && !completed && dayIndex == days

    /** 进度条比例：dayIndex / days，成功时满格。 */
    val progressFraction: Float
        get() = if (days <= 0) 0f else (dayIndex.toFloat() / days).coerceIn(0f, 1f)
}

fun computeChallengeProgress(
    records: List<CheckInRecord>,
    attempt: ChallengeAttempt,
    today: LocalDate = LocalDate.now(),
): ChallengeProgress {
    val startDate = attempt.startDate
    val endDate = attempt.endDate
    val startMillis = attempt.startAt
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val lastDayToCheck = if (today.isAfter(endDate)) endDate else today
    var clean = 0
    var day = startDate
    while (!day.isAfter(lastDayToCheck)) {
        val hit = records.any { record ->
            !record.isResist &&
                record.date == day.toString() &&
                // 开始当天只看「点开始」之后的记录；之后的整天都算
                (day != startDate || recordIsAfterStart(record, startMillis, attempt.startRecordId))
        }
        if (hit) {
            return ChallengeProgress(
                attempt = attempt,
                dayIndex = DayCount.between(startDate, day),
                cleanDays = clean,
                failed = true,
                failedOn = day,
                completed = false,
            )
        }
        if (day.isBefore(today)) clean++
        day = day.plusDays(1)
    }

    val completed = today.isAfter(endDate)
    val index = if (completed) attempt.days else DayCount.between(startDate, today)
    return ChallengeProgress(
        attempt = attempt,
        dayIndex = index,
        cleanDays = clean,
        failed = false,
        failedOn = null,
        completed = completed,
    )
}

/**
 * 开始当天的记录是否落在「点开始」之后。
 *
 * 记录时间只到分钟：开始于 13:16:50 时，13:16:10 保存的记录时间戳是 13:16:00，
 * 比开始时刻小，但它其实是开始之后才写进去的，应该判失败。
 * 反过来，开始前同一分钟已有的记录 id 不会大于水位，不能误判。
 */
private fun recordIsAfterStart(
    record: CheckInRecord,
    startMillis: Long,
    startRecordId: Long?,
): Boolean {
    if (record.timestamp >= startMillis) return true
    if (startRecordId == null) return false
    // 只有同一分钟内的记录才可能是「先有记录、后点开始」这种歧义情况
    if (record.timestamp / MINUTE_MILLIS != startMillis / MINUTE_MILLIS) return false
    return record.id > startRecordId
}

private const val MINUTE_MILLIS = 60_000L

/** 用来标记「这次挑战的鼓励弹窗已经弹过了」。 */
fun challengeKey(attempt: ChallengeAttempt): String =
    attempt.startAt.toString() + "|" + attempt.days

private object DayCount {
    fun between(from: LocalDate, to: LocalDate): Int =
        ChronoUnit.DAYS.between(from, to).toInt().coerceAtLeast(0)
}
