package com.qingqian.chonglema.stats

import com.qingqian.chonglema.model.CheckInRecord
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** 年度报告数据（只统计「冲了」，忍住单独统计）。 */
data class AnnualReport(
    val year: Int,
    val totalCount: Int = 0,
    val activeDays: Int = 0,
    val resistCount: Int = 0,
    val bestStreak: Int = 0,
    val maxPerDay: Int = 0,
    val maxPerDayDate: LocalDate? = null,
    /** 1~12 月，冲了次数 */
    val monthlyCounts: List<Int> = List(12) { 0 },
    /** 1~12 月，忍住次数 */
    val monthlyResist: List<Int> = List(12) { 0 },
    /** 周一~周日（下标 0 为周一） */
    val weekdayCounts: List<Int> = List(7) { 0 },
    val periodCounts: List<Int> = listOf(0, 0, 0, 0),
    val typeCounts: Map<String, Int> = emptyMap(),
    val firstAt: LocalDateTime? = null,
    val lastAt: LocalDateTime? = null,
    val avgIntervalHours: Double = 0.0,
)

val YEAR_MONTH_LABELS = listOf("1 月", "2 月", "3 月", "4 月", "5 月", "6 月", "7 月", "8 月", "9 月", "10 月", "11 月", "12 月")

val WEEKDAY_LABELS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

fun computeAnnualReport(records: List<CheckInRecord>, year: Int): AnnualReport {
    val inYear = records.filter { parseDate(it.date)?.year == year }
    val done = inYear.filter { !it.isResist }
    val resist = inYear.filter { it.isResist }
    val doneCounts = countByDate(done)

    val monthly = IntArray(12)
    val monthlyResist = IntArray(12)
    done.forEach { record -> parseDate(record.date)?.let { monthly[it.monthValue - 1]++ } }
    resist.forEach { record -> parseDate(record.date)?.let { monthlyResist[it.monthValue - 1]++ } }

    val weekday = IntArray(7)
    done.forEach { record -> parseDate(record.date)?.let { weekday[it.dayOfWeek.value - 1]++ } }

    val periods = IntArray(4)
    done.forEach { record -> periods[recordDateTime(record).hour / 6]++ }

    // 年度内最长连续打卡
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    doneCounts.keys.sorted().forEach { date ->
        run = if (previous != null && previous!!.plusDays(1) == date) run + 1 else 1
        if (run > best) best = run
        previous = date
    }

    val maxEntry = doneCounts.entries.maxByOrNull { it.value }
    val timestamps = done.map { it.timestamp }.sorted()
    val avgInterval = if (timestamps.size >= 2) {
        (timestamps.last() - timestamps.first()) / 3_600_000.0 / (timestamps.size - 1)
    } else {
        0.0
    }
    fun toDateTime(ms: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())

    return AnnualReport(
        year = year,
        totalCount = done.size,
        activeDays = doneCounts.size,
        resistCount = resist.size,
        bestStreak = best,
        maxPerDay = maxEntry?.value ?: 0,
        maxPerDayDate = maxEntry?.key,
        monthlyCounts = monthly.toList(),
        monthlyResist = monthlyResist.toList(),
        weekdayCounts = weekday.toList(),
        periodCounts = periods.toList(),
        typeCounts = done.groupingBy { it.type }.eachCount(),
        firstAt = timestamps.firstOrNull()?.let { toDateTime(it) },
        lastAt = timestamps.lastOrNull()?.let { toDateTime(it) },
        avgIntervalHours = avgInterval,
    )
}

/** 年度报告里的几句话总结。 */
fun annualHighlights(report: AnnualReport): List<String> {
    val lines = mutableListOf<String>()
    if (report.totalCount == 0) {
        return listOf("这一年还没有记录，新的一年可以开始记第一笔。")
    }
    val bestMonthIndex = report.monthlyCounts.maxOrNull() ?: 0
    if (bestMonthIndex > 0) {
        val index = report.monthlyCounts.indexOf(bestMonthIndex)
        lines += "最活跃的月份是 " + YEAR_MONTH_LABELS[index] + "，共 " + bestMonthIndex + " 次。"
    }
    val weekdaySorted = report.weekdayCounts.sortedDescending()
    val bestWeekday = weekdaySorted.getOrElse(0) { 0 }
    if (bestWeekday > 0) {
        val tie = weekdaySorted.size > 1 && weekdaySorted[1] == bestWeekday
        if (tie) {
            lines += "这一年在星期上分布得比较平均，没有明显的高发日。"
        } else {
            val index = report.weekdayCounts.indexOf(bestWeekday)
            lines += "最常发生的是" + WEEKDAY_LABELS[index] + "，累计 " + bestWeekday + " 次。"
        }
    }
    val bestPeriod = report.periodCounts.maxOrNull() ?: 0
    if (bestPeriod > 0) {
        val index = report.periodCounts.indexOf(bestPeriod)
        lines += "高峰时段是" + PERIOD_LABELS[index] + "，占全年的 " + (bestPeriod * 100 / report.totalCount) + "%。"
    }
    lines += "全年打卡 " + report.activeDays + " 天，最长连续 " + report.bestStreak + " 天。"
    if (report.resistCount > 0) {
        lines += "忍住了 " + report.resistCount + " 次。"
    } else {
        lines += "这一年还没记过「忍住了」，下次冲动时可以试试记录一下。"
    }
    return lines
}