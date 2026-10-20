package com.qingqian.chonglema.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingqian.chonglema.model.CheckInRecord
import com.qingqian.chonglema.stats.PeriodReport
import com.qingqian.chonglema.stats.ReportPeriod
import com.qingqian.chonglema.stats.buildReportRange
import com.qingqian.chonglema.stats.computePeriodReport
import com.qingqian.chonglema.stats.countByDate
import com.qingqian.chonglema.stats.parseDate
import com.qingqian.chonglema.stats.previousReportRange
import com.qingqian.chonglema.stats.thisWeekStart
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDateFormatter = DateTimeFormatter.ofPattern("M/d")

/** 报表可以回看到的最早年份：允许查看没有记录的年份/月份。 */
private const val MIN_REPORT_YEAR = 2000

private val PERIOD_TABS = listOf(
    ReportPeriod.ALL to "总",
    ReportPeriod.YEAR to "年",
    ReportPeriod.MONTH to "月",
    ReportPeriod.WEEK to "周",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    records: List<CheckInRecord>,
    types: List<String>,
    today: LocalDate,
    onBack: () -> Unit,
) {
    val earliestDate = remember(records) {
        records.mapNotNull { parseDate(it.date) }.minOrNull()
    }
    var period by rememberSaveable { mutableStateOf(ReportPeriod.ALL) }
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    var month by rememberSaveable { mutableIntStateOf(today.monthValue) }
    var weekEpoch by rememberSaveable { mutableStateOf(thisWeekStart(today).toEpochDay()) }
    var showPicker by remember { mutableStateOf(false) }

    val weekStart = LocalDate.ofEpochDay(weekEpoch)
    val range = remember(period, year, month, weekEpoch, today, earliestDate) {
        buildReportRange(period, today, year, month, weekStart, earliestDate)
    }
    val report = remember(records, range, today) { computePeriodReport(records, range, today) }
    val previousRange = remember(range) { previousReportRange(range) }
    val previousReport = remember(records, previousRange, today) {
        previousRange?.let { computePeriodReport(records, it, today) }
    }
    val allDoneCounts = remember(records) { countByDate(records.filter { !it.isResist }) }
    val allResistCounts = remember(records) { countByDate(records.filter { it.isResist }) }
    // 点热力图里某一天 → 弹窗显示当天冲了/忍住各多少次
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    val currentMonth = remember(today) { YearMonth.from(today) }
    // 允许回看没有记录的年/月/周，只限制不早于 MIN_REPORT_YEAR
    val canPrev = when (period) {
        ReportPeriod.ALL -> false
        ReportPeriod.YEAR -> year > MIN_REPORT_YEAR
        ReportPeriod.MONTH -> YearMonth.of(year, month) > YearMonth.of(MIN_REPORT_YEAR, 1)
        ReportPeriod.WEEK -> weekStart > LocalDate.of(MIN_REPORT_YEAR, 1, 1)
    }
    val canNext = when (period) {
        ReportPeriod.ALL -> false
        ReportPeriod.YEAR -> year < today.year
        ReportPeriod.MONTH -> YearMonth.of(year, month) < currentMonth
        ReportPeriod.WEEK -> weekStart < thisWeekStart(today)
    }

    fun goPrev() {
        when (period) {
            ReportPeriod.ALL -> Unit
            ReportPeriod.YEAR -> year -= 1
            ReportPeriod.MONTH -> {
                if (month == 1) {
                    month = 12
                    year -= 1
                } else {
                    month -= 1
                }
            }
            ReportPeriod.WEEK -> weekEpoch -= 7
        }
    }

    fun goNext() {
        when (period) {
            ReportPeriod.ALL -> Unit
            ReportPeriod.YEAR -> year += 1
            ReportPeriod.MONTH -> {
                if (month == 12) {
                    month = 1
                    year += 1
                } else {
                    month += 1
                }
            }
            ReportPeriod.WEEK -> weekEpoch += 7
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("统计报表", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PERIOD_TABS.forEach { (value, label) ->
                    FilterChip(
                        selected = period == value,
                        onClick = { period = value },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            RangeSwitcher(
                title = range.title,
                canPrev = canPrev,
                canNext = canNext,
                showArrows = period != ReportPeriod.ALL,
                onPrev = ::goPrev,
                onNext = ::goNext,
                onTitleClick = { if (period != ReportPeriod.ALL) showPicker = true },
            )
            Spacer(Modifier.height(14.dp))
            ReportGrid(report)
            if (period != ReportPeriod.ALL && previousReport != null && previousRange != null) {
                Spacer(Modifier.height(16.dp))
                TrendCard(
                    current = report,
                    previous = previousReport,
                    previousTitle = previousRange.title,
                    previousLabel = when (period) {
                        ReportPeriod.YEAR -> "上一年"
                        ReportPeriod.MONTH -> "上一月"
                        ReportPeriod.WEEK -> "上一周"
                        ReportPeriod.ALL -> "上期"
                    },
                )
            }
            if (period != ReportPeriod.ALL) {
                Spacer(Modifier.height(20.dp))
                HeatmapCard(
                    range = range,
                    report = report,
                    doneCounts = allDoneCounts,
                    today = today,
                    onDayClick = { selectedDay = it },
                )
            }
            Spacer(Modifier.height(20.dp))
            PeriodDistributionCard(report.periodCounts)
            Spacer(Modifier.height(20.dp))
            TypeDistributionCard(types = types, typeCounts = report.typeCounts)
            Spacer(Modifier.height(20.dp))
            BarCard(report = report, period = period)
            Spacer(Modifier.height(32.dp))
        }
    }

    selectedDay?.let { day ->
        val done = allDoneCounts[day] ?: 0
        val resist = allResistCounts[day] ?: 0
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            title = { Text(dateLabel(day), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    if (done == 0 && resist == 0) {
                        Text(
                            text = "这天没有记录。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        DayCountRow("冲了", done, MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        DayCountRow("忍住", resist, MaterialTheme.colorScheme.tertiary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDay = null }) { Text("关闭") }
            },
        )
    }

    if (showPicker) {
        RangePickerDialog(
            period = period,
            today = today,
            year = year,
            month = month,
            weekStart = weekStart,
            onPickYear = { year = it; showPicker = false },
            onPickMonth = { y, m -> year = y; month = m; showPicker = false },
            onPickWeek = { weekEpoch = it.toEpochDay(); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun DayCountRow(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString() + " 次",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun RangeSwitcher(
    title: String,
    canPrev: Boolean,
    canNext: Boolean,
    showArrows: Boolean = true,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onTitleClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showArrows) {
            IconButton(onClick = onPrev, enabled = canPrev) {
                Icon(
                    Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "上一个周期",
                    tint = if (canPrev) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onTitleClick)
                .padding(vertical = 8.dp),
        )
        if (showArrows) {
            IconButton(onClick = onNext, enabled = canNext) {
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = "下一个周期",
                    tint = if (canNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                )
            }
        }
    }
}

@Composable
private fun ReportGrid(report: PeriodReport) {
    val avg = report.avgIntervalDays?.let { String.format(Locale.US, "%.1f 天", it) } ?: "—"
    val maxDay = if (report.maxPerDayDate != null) {
        report.maxPerDay.toString() + " 次 · " + report.maxPerDayDate.format(shortDateFormatter)
    } else {
        "—"
    }
    val tiles = listOf(
        "累计次数" to (report.doneCount.toString() + " 次"),
        "忍住次数" to (report.resistCount.toString() + " 次"),
        "累计天数" to (report.doneDays.toString() + " 天"),
        "单日最多" to maxDay,
        "最长连续" to (report.bestStreak.toString() + " 天"),
        "平均一次" to avg,
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(2).forEach { rowTiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowTiles.forEach { (label, value) ->
                    StatCard(label = label, value = value, modifier = Modifier.weight(1f))
                }
                if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RangePickerDialog(
    period: ReportPeriod,
    today: LocalDate,
    year: Int,
    month: Int,
    weekStart: LocalDate,
    onPickYear: (Int) -> Unit,
    onPickMonth: (Int, Int) -> Unit,
    onPickWeek: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (period) {
                    ReportPeriod.YEAR -> "选择年份"
                    ReportPeriod.MONTH -> "选择月份"
                    ReportPeriod.WEEK -> "选择周"
                    ReportPeriod.ALL -> "全部记录"
                },
            )
        },
        text = {
            when (period) {
                ReportPeriod.YEAR -> {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        (today.year downTo MIN_REPORT_YEAR).forEach { y ->
                            Text(
                                text = y.toString() + " 年",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (y == year) FontWeight.Bold else FontWeight.Normal,
                                color = if (y == year) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPickYear(y) }
                                    .padding(vertical = 10.dp),
                            )
                        }
                    }
                }
                ReportPeriod.MONTH -> {
                    var pickerYear by remember { mutableIntStateOf(year) }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (pickerYear > MIN_REPORT_YEAR) pickerYear -= 1 },
                                enabled = pickerYear > MIN_REPORT_YEAR,
                            ) {
                                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "上一年")
                            }
                            Text(
                                text = pickerYear.toString() + " 年",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { if (pickerYear < today.year) pickerYear += 1 },
                                enabled = pickerYear < today.year,
                            ) {
                                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "下一年")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        (1..12).chunked(3).forEach { rowMonths ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                rowMonths.forEach { m ->
                                    val ym = YearMonth.of(pickerYear, m)
                                    val enabled = !ym.isAfter(YearMonth.from(today))
                                    Text(
                                        text = m.toString() + " 月",
                                        textAlign = TextAlign.Center,
                                        fontWeight = if (pickerYear == year && m == month) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            pickerYear == year && m == month -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(enabled = enabled) { onPickMonth(pickerYear, m) }
                                            .padding(vertical = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                ReportPeriod.WEEK -> {
                    val currentWeek = thisWeekStart(today)
                    Column(
                        modifier = Modifier
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        (0 until 16).forEach { offset ->
                            val start = currentWeek.minusWeeks(offset.toLong())
                            val end = start.plusDays(6)
                            Text(
                                text = start.monthValue.toString() + "/" + start.dayOfMonth + "–" +
                                    end.monthValue + "/" + end.dayOfMonth +
                                    if (offset == 0) "（本周）" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (start == weekStart) FontWeight.Bold else FontWeight.Normal,
                                color = if (start == weekStart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPickWeek(start) }
                                    .padding(vertical = 10.dp),
                            )
                        }
                    }
                }
                ReportPeriod.ALL -> Unit
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
