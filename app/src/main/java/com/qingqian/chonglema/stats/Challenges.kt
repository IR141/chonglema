package com.qingqian.chonglema.stats

import com.qingqian.chonglema.model.CheckInRecord
import java.time.LocalDate

/** 一个连续忍住挑战：从 start 起连续 days 天没有「冲了」记录。 */
data class ChallengeState(val days: Int, val start: LocalDate) {
    val endDate: LocalDate get() = start.plusDays(days - 1L)
}

data class ChallengeProgress(
    val state: ChallengeState,
    /** 已连续成功天数（失败后停在失败前一天） */
    val successDays: Int = 0,
    /** 已经过去的天数（含今天，最多 days） */
    val elapsedDays: Int = 0,
    val failed: Boolean = false,
    val failedOn: LocalDate? = null,
    val completed: Boolean = false,
) {
    val remainingDays: Int get() = (state.days - successDays).coerceAtLeast(0)
    val progressFraction: Float
        get() = if (state.days <= 0) 0f else (successDays.toFloat() / state.days).coerceIn(0f, 1f)
}

fun computeChallengeProgress(
    records: List<CheckInRecord>,
    state: ChallengeState,
    today: LocalDate = LocalDate.now(),
): ChallengeProgress {
    val doneDates = records.filter { !it.isResist }
        .mapNotNull { parseDate(it.date) }
        .toHashSet()
    var elapsed = 0
    var success = 0
    var cursor = state.start
    while (elapsed < state.days) {
        val dayOk = !doneDates.contains(cursor)
        if (cursor.isAfter(today)) break
        elapsed++
        if (!dayOk) {
            return ChallengeProgress(
                state = state,
                successDays = success,
                elapsedDays = elapsed,
                failed = true,
                failedOn = cursor,
            )
        }
        success++
        cursor = cursor.plusDays(1)
    }
    val completed = elapsed >= state.days && success == state.days && !today.isBefore(state.endDate)
    return ChallengeProgress(
        state = state,
        successDays = success,
        elapsedDays = elapsed,
        completed = completed,
    )
}
