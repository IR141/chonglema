package com.qingqian.chonglema.stats

import com.qingqian.chonglema.model.CheckInRecord
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** 统计报表的周期。 */
enum class ReportPeriod { ALL, YEAR, MONTH, WEEK }

/** 报表当前选中的时间范围。 */
data class ReportRange(
    val period: ReportPeriod,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val title: String,
    /** 只有「年」模式才显示年度热力图 */
    val heatmapYear: Int? = null,
)

/** 某个周期内的统计结果。 */
data class PeriodReport(
    val doneCount: Int = 0,
    val resistCount: Int = 0,
    val doneDays: Int = 0,
    val maxPerDay: Int = 0,
    val maxPerDayDate: LocalDate? = null,
    val bestStreak: Int = 0,
    /** 平均多少天一次；记录少于 2 条时为 null */
    val avgIntervalDays: Double? = null,
    val periodCounts: List<Int> = listOf(0, 0, 0, 0),
    val typeCounts: Map<String, Int> = emptyMap(),
    val barLabels: List<String> = emptyList(),
    val barDone: List<Int> = emptyList(),
    val barResist: List<Int> = emptyList(),
) {
    val barPeak: Int
        get() = barDone.indices.maxOfOrNull { barDone[it] + barResist.getOrElse(it) { 0 } }
            ?.coerceAtLeast(1) ?: 1
}

fun thisWeekStart(today: LocalDate): LocalDate =
    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * 上一段用来做趋势对比的范围：年→上一年、月→上个月、周→上一周。
 * 全部记录没有可比的上一段，返回 null。
 */
fun previousReportRange(range: ReportRange): ReportRange? = when (range.period) {
    ReportPeriod.ALL -> null
    ReportPeriod.YEAR -> {
        val year = range.startDate.year - 1
        ReportRange(ReportPeriod.YEAR, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), year.toString() + " 年")
    }
    ReportPeriod.MONTH -> {
        val ym = YearMonth.from(range.startDate).minusMonths(1)
        ReportRange(
            ReportPeriod.MONTH,
            ym.atDay(1),
            ym.atEndOfMonth(),
            ym.year.toString() + " 年 " + ym.monthValue + " 月",
        )
    }
    ReportPeriod.WEEK -> {
        val start = range.startDate.minusWeeks(1)
        val end = start.plusDays(6)
        ReportRange(
            ReportPeriod.WEEK,
            start,
            end,
            start.monthValue.toString() + "/" + start.dayOfMonth + "–" + end.monthValue + "/" + end.dayOfMonth,
        )
    }
}

fun buildReportRange(
    period: ReportPeriod,
    today: LocalDate,
    year: Int,
    month: Int,
    weekStart: LocalDate,
    earliestDate: LocalDate? = null,
): ReportRange {
    val week = if (weekStart.isAfter(thisWeekStart(today))) thisWeekStart(today) else weekStart
    return when (period) {
        ReportPeriod.ALL -> {
            val start = earliestDate ?: today
            ReportRange(ReportPeriod.ALL, start, today, "全部记录")
        }
        ReportPeriod.YEAR -> ReportRange(
            ReportPeriod.YEAR,
            LocalDate.of(year, 1, 1),
            LocalDate.of(year, 12, 31),
            year.toString() + " 年",
            heatmapYear = year,
        )
        ReportPeriod.MONTH -> {
            val ym = YearMonth.of(year, month)
            ReportRange(
                ReportPeriod.MONTH,
                ym.atDay(1),
                ym.atEndOfMonth(),
                ym.year.toString() + " 年 " + ym.monthValue + " 月",
            )
        }
        ReportPeriod.WEEK -> ReportRange(
            ReportPeriod.WEEK,
            week,
            week.plusDays(6),
            week.monthValue.toString() + "/" + week.dayOfMonth + "–" +
                week.plusDays(6).monthValue + "/" + week.plusDays(6).dayOfMonth,
        )
    }
}

fun computePeriodReport(
    records: List<CheckInRecord>,
    range: ReportRange,
    today: LocalDate = LocalDate.now(),
): PeriodReport {
    val inRange = if (range.period == ReportPeriod.ALL) {
        records
    } else {
        records.filter { record ->
            val date = parseDate(record.date) ?: return@filter false
            !date.isBefore(range.startDate) && !date.isAfter(range.endDate)
        }
    }
    val done = inRange.filter { !it.isResist }
    val resist = inRange.filter { it.isResist }
    val doneCounts = countByDate(done)
    val resistCounts = countByDate(resist)

    var best = 0
    var run = 0
    var previous: LocalDate? = null
    doneCounts.keys.sorted().forEach { date ->
        run = if (previous != null && previous!!.plusDays(1) == date) run + 1 else 1
        if (run > best) best = run
        previous = date
    }
    val maxEntry = doneCounts.entries
        .sortedWith(compareByDescending<Map.Entry<LocalDate, Int>> { it.value }.thenByDescending { it.key })
        .firstOrNull()

    // 平均多少天一次 = 这段时间的天数 ÷ 这段时间的次数。
    // 周期起点取报表的起始日期，终点最多算到今天（未来的日期不参与统计）。
    val periodEnd = if (range.endDate.isAfter(today)) today else range.endDate
    val spanDays = if (range.startDate.isAfter(periodEnd)) {
        0L
    } else {
        ChronoUnit.DAYS.between(range.startDate, periodEnd) + 1
    }
    val avgDays = if (done.isNotEmpty() && spanDays > 0) {
        spanDays.toDouble() / done.size
    } else {
        null
    }
    val periods = IntArray(4)
    done.forEach { record -> periods[recordDateTime(record).hour / 6]++ }

    val labels = mutableListOf<String>()
    val barDone = mutableListOf<Int>()
    val barResist = mutableListOf<Int>()
    when (range.period) {
        ReportPeriod.WEEK -> {
            for (i in 0 until 7) {
                val date = range.startDate.plusDays(i.toLong())
                labels += "一二三四五六日"[i].toString()
                barDone += doneCounts[date] ?: 0
                barResist += resistCounts[date] ?: 0
            }
        }
        ReportPeriod.MONTH -> {
            for (day in 1..range.startDate.lengthOfMonth()) {
                val date = range.startDate.withDayOfMonth(day)
                labels += day.toString()
                barDone += doneCounts[date] ?: 0
                barResist += resistCounts[date] ?: 0
            }
        }
        ReportPeriod.YEAR -> {
            for (monthIndex in 1..12) {
                labels += monthIndex.toString()
                var d = 0
                var r = 0
                doneCounts.forEach { (date, value) -> if (date.year == range.startDate.year && date.monthValue == monthIndex) d += value }
                resistCounts.forEach { (date, value) -> if (date.year == range.startDate.year && date.monthValue == monthIndex) r += value }
                barDone += d
                barResist += r
            }
        }
        ReportPeriod.ALL -> {
            val maxYear = range.endDate.year
            val minYear = minOf(range.startDate.year, maxYear)
            for (y in minYear..maxYear) {
                labels += y.toString()
                var d = 0
                var r = 0
                doneCounts.forEach { (date, value) -> if (date.year == y) d += value }
                resistCounts.forEach { (date, value) -> if (date.year == y) r += value }
                barDone += d
                barResist += r
            }
        }
    }

    return PeriodReport(
        doneCount = done.size,
        resistCount = resist.size,
        doneDays = doneCounts.size,
        maxPerDay = maxEntry?.value ?: 0,
        maxPerDayDate = maxEntry?.key,
        bestStreak = best,
        avgIntervalDays = avgDays,
        periodCounts = periods.toList(),
        typeCounts = done.groupingBy { it.type }.eachCount(),
        barLabels = labels,
        barDone = barDone,
        barResist = barResist,
    )
}
