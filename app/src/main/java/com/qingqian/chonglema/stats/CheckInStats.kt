package com.qingqian.chonglema.stats

import com.qingqian.chonglema.model.CheckInRecord
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** 时段标签，与 [CheckInStats.periodCounts] 一一对应。 */
val PERIOD_LABELS = listOf("凌晨 0-6", "上午 6-12", "下午 12-18", "晚上 18-24")

/**
 * 全部统计数据。
 */
data class CheckInStats(
    val todayCount: Int = 0,
    val weekCount: Int = 0,
    val monthCount: Int = 0,
    val monthDays: Int = 0,
    val totalCount: Int = 0,
    val totalDays: Int = 0,
    /** 连续打卡天数（今天没打卡时从昨天往前数） */
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val maxPerDay: Int = 0,
    val maxPerDayDate: LocalDate? = null,
    /** 近 4 周平均每周次数 */
    val avgPerWeek: Double = 0.0,
    /** 相邻两次的平均间隔（小时） */
    val avgIntervalHours: Double = 0.0,
    val lastAt: LocalDateTime? = null,
    val periodCounts: List<Int> = listOf(0, 0, 0, 0),
    /** 真正冲了的次数（不含忍住） */
    val doneCount: Int = 0,
    /** 忍住次数 */
    val resistCount: Int = 0,
    /** 自控率 = 忍住 / (忍住 + 冲了)，0..1 */
    val selfControlRate: Double = 0.0,
    /** 今天忍住的次数 */
    val resistToday: Int = 0,
    /** 冲了的记录按类型计数 */
    val typeCounts: Map<String, Int> = emptyMap(),
    /** 日期 -> 当天「冲了」次数（首页日历 / 报表热力图共用，避免每个页面重复统计） */
    val doneCounts: Map<LocalDate, Int> = emptyMap(),
    /** 日期 -> 当天「忍住」次数 */
    val resistCounts: Map<LocalDate, Int> = emptyMap(),
)

fun parseDate(text: String): LocalDate? = try {
    LocalDate.parse(text)
} catch (_: Exception) {
    null
}

fun recordDateTime(record: CheckInRecord): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp), ZoneId.systemDefault())

/** 日期 -> 当天次数。 */
fun countByDate(records: List<CheckInRecord>): Map<LocalDate, Int> {
    val map = HashMap<LocalDate, Int>()
    records.forEach { record ->
        parseDate(record.date)?.let { date -> map[date] = (map[date] ?: 0) + 1 }
    }
    return map
}

fun computeStats(records: List<CheckInRecord>, today: LocalDate = LocalDate.now()): CheckInStats {
    // 「次数」只统计真的冲了；忍住单独统计，作为自控率的分母
    val done = records.filter { !it.isResist }
    val resist = records.filter { it.isResist }
    val counts = countByDate(done)
    val resistCounts = countByDate(resist)
    val month = YearMonth.from(today)
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val todayCount = counts[today] ?: 0
    val weekCount = counts.entries
        .filter { !it.key.isBefore(weekStart) && !it.key.isAfter(today) }
        .sumOf { it.value }
    val monthEntries = counts.entries.filter { YearMonth.from(it.key) == month }
    val monthCount = monthEntries.sumOf { it.value }

    // 当前连续天数
    var streak = 0
    var cursor = if (todayCount > 0) today else today.minusDays(1)
    while ((counts[cursor] ?: 0) > 0) {
        streak++
        cursor = cursor.minusDays(1)
    }

    // 历史最长连续
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    counts.keys.sorted().forEach { date ->
        run = if (previous != null && previous!!.plusDays(1) == date) run + 1 else 1
        if (run > best) best = run
        previous = date
    }

    val maxEntry = counts.entries.maxByOrNull { it.value }

    // 平均间隔
    val timestamps = done.map { it.timestamp }.sorted()
    val avgInterval = if (timestamps.size >= 2) {
        val spanHours = (timestamps.last() - timestamps.first()) / 3_600_000.0
        spanHours / (timestamps.size - 1)
    } else {
        0.0
    }

    // 近 4 周频率
    val from = today.minusDays(27)
    val recentCount = counts.entries
        .filter { !it.key.isBefore(from) && !it.key.isAfter(today) }
        .sumOf { it.value }
    val avgPerWeek = recentCount / 4.0

    val periods = IntArray(4)
    done.forEach { record ->
        val hour = recordDateTime(record).hour
        periods[hour / 6]++
    }

    val lastAt = timestamps.lastOrNull()?.let {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
    }

    return CheckInStats(
        todayCount = todayCount,
        weekCount = weekCount,
        monthCount = monthCount,
        monthDays = monthEntries.size,
        totalCount = done.size,
        totalDays = counts.size,
        currentStreak = streak,
        bestStreak = best,
        maxPerDay = maxEntry?.value ?: 0,
        maxPerDayDate = maxEntry?.key,
        avgPerWeek = avgPerWeek,
        avgIntervalHours = avgInterval,
        lastAt = lastAt,
        periodCounts = periods.toList(),
        doneCount = done.size,
        resistCount = resist.size,
        selfControlRate = if (done.size + resist.size > 0) {
            resist.size.toDouble() / (done.size + resist.size)
        } else {
            0.0
        },
        resistToday = resistCounts[today] ?: 0,
        typeCounts = done.groupingBy { it.type }.eachCount(),
        doneCounts = counts,
        resistCounts = resistCounts,
    )
}
