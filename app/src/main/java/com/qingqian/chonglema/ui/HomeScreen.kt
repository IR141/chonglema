package com.qingqian.chonglema.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingqian.chonglema.data.GoalConfig
import com.qingqian.chonglema.model.CheckInRecord
import com.qingqian.chonglema.stats.CheckInStats
import com.qingqian.chonglema.stats.computeGoalProgress
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateLabelFormatter = DateTimeFormatter.ofPattern("M 月 d 日 EEEE", Locale.CHINESE)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val shortDateFormatter = DateTimeFormatter.ofPattern("M/d")

fun dateLabel(date: LocalDate): String = date.format(dateLabelFormatter)

/** 距上次多久：不足 1 小时按分钟，不足 24 小时按小时，再往上按天；小数保留 1 位。 */
fun formatSinceLast(last: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
    val duration = Duration.between(last, now)
    val minutes = duration.toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> minutes.toString() + " 分钟"
        duration.toHours() < 24 -> String.format(Locale.US, "%.1f 小时", minutes / 60.0)
        else -> String.format(Locale.US, "%.1f 天", duration.toHours() / 24.0)
    }
}

/** 彩蛋文案：某些次数给一句吐槽。 */
fun eggMessage(count: Int): String? = when (count) {
    2 -> "第 2 次了，记得多喝水 💧"
    3 -> "第 3 次……兄弟，身体是革命的本钱"
    4 -> "第 4 次？你确定不是手滑？"
    6 -> "第 6 次，今天的你有点放纵"
    10 -> "第 10 次，今天已经超越人类了 🚀"
    else -> if (count > 10 && count % 5 == 0) "第 " + count + " 次，注意休息吧少年" else null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    records: List<CheckInRecord>,
    today: LocalDate,
    stats: CheckInStats,
    goal: GoalConfig,
    types: List<String>,
    onAddType: (String) -> String?,
    onRemoveType: (String) -> String?,
    onMoveType: (Int, Int) -> Unit,
    onResetTypes: () -> Unit,
    launchTitle: String,
    onAddRecord: (LocalDate, LocalTime, String, String) -> Unit,
    onUpdateRecord: (Long, LocalDate, LocalTime, String, String) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onClearDate: (LocalDate) -> Unit,
    onResist: (LocalDate, LocalTime) -> Unit,
    onOpenReport: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenChallenge: () -> Unit,
    onOpenAnnual: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    // today / stats 由 App 统一算好传进来，这里不再重复统计
    val doneCounts = stats.doneCounts
    val resistCounts = stats.resistCounts
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val month = remember(monthOffset, today) { YearMonth.from(today).plusMonths(monthOffset.toLong()) }

    var sheetDate by remember { mutableStateOf<LocalDate?>(null) }
    var sheetEditMode by remember { mutableStateOf(false) }
    // 从首页大卡片「记一次」进来：保存后自动收起面板；从日历补记 / 编辑进来则留在面板里
    var sheetDismissOnAdd by remember { mutableStateOf(false) }
    var forgotDate by remember { mutableStateOf<LocalDate?>(null) }
    val toastState = rememberAppToastState()
    val scope = rememberCoroutineScope()

    fun toast(message: String) {
        toastState.show(scope, message)
    }

    fun quickResist() {
        val now = LocalTime.now()
        onResist(today, now)
        toast("忍住了，厉害 💪 · " + now.format(timeFormatter))
    }

    fun hasAnyRecord(date: LocalDate): Boolean =
        (doneCounts[date] ?: 0) + (resistCounts[date] ?: 0) > 0

    val todayRecords = remember(records, today) {
        records.filter { it.date == today.toString() && !it.isResist }
    }
    val lastToday = todayRecords.lastOrNull()
    val monthCounts = remember(doneCounts, month) { doneCounts.filterKeys { YearMonth.from(it) == month } }
    val isCurrentMonth = month == YearMonth.from(today)
    val elapsedDays = if (isCurrentMonth) today.dayOfMonth else month.lengthOfMonth()
    val monthCount = monthCounts.values.sum()
    // 平均多少天一次 = 本月已过天数 ÷ 本月次数（本月 1 号 → 今天）
    val monthAvgDays = if (monthCount > 0) elapsedDays.toDouble() / monthCount else null
    val maxDayEntry = monthCounts.maxByOrNull { it.value }
    val recent = remember(records) { records.sortedByDescending { it.timestamp }.take(3) }
    val goalProgress = remember(records, goal, today) { computeGoalProgress(records, goal, today) }
    val lastSeenText = stats.lastAt?.let {
        it.toLocalDate().format(shortDateFormatter) + " " + it.toLocalTime().format(timeFormatter)
    } ?: "还没有记录"
    var pendingDelete by remember { mutableStateOf<CheckInRecord?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { AppToastHost(toastState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = launchTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = when {
                                stats.todayCount > 0 && stats.resistToday > 0 ->
                                    "今天第 " + stats.todayCount + " 次 · 忍住 " + stats.resistToday + " 次"
                                stats.todayCount > 0 -> "今天第 " + stats.todayCount + " 次"
                                stats.resistToday > 0 -> "今天忍住了 " + stats.resistToday + " 次"
                                else -> "今天还没有记录"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索记录", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置", tint = MaterialTheme.colorScheme.primary)
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
            TodayHeroCard(
                count = stats.todayCount,
                resistCount = stats.resistToday,
                lastTime = lastToday?.time,
                goal = goalProgress,
                onCheckIn = {
                    sheetDismissOnAdd = true
                    sheetEditMode = false
                    sheetDate = today
                },
                onResist = { quickResist() },
                onOpenReport = onOpenReport,
            )
            Spacer(Modifier.height(10.dp))
            // 目标与回顾的三个入口，从大卡片下面直接进
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EntryButton("自律目标", Modifier.weight(1f), onOpenGoals)
                EntryButton("挑战与徽章", Modifier.weight(1f), onOpenChallenge)
                EntryButton("年度报告", Modifier.weight(1f), onOpenAnnual)
            }
            Spacer(Modifier.height(10.dp))
            // 三张卡等高：以最高的那张为准，避免「44.5 天」把第三张撑高
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    value = stats.monthCount.toString(),
                    label = "本月次数",
                    sub = "打卡 " + stats.monthDays + " 天",
                )
                SinceLastStatTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    lastAt = stats.lastAt,
                    lastSeenText = lastSeenText,
                )
                StatTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    value = monthAvgDays?.let { String.format(Locale.US, "%.1f 天", it) } ?: "—",
                    label = "平均一次",
                    sub = "本月频率",
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = month.year.toString() + " 年 " + month.monthValue + " 月",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { monthOffset -= 1 }) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "上个月", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { monthOffset += 1 }, enabled = !isCurrentMonth) {
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = "下个月",
                        tint = if (isCurrentMonth) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CalendarGrid(
                month = month,
                counts = doneCounts,
                resistCounts = resistCounts,
                today = today,
                onDateClick = { date ->
                    when {
                        date.isAfter(today) -> toast("未来的日子，到了再说吧")
                        !hasAnyRecord(date) && date.isBefore(today) -> forgotDate = date
                        hasAnyRecord(date) -> {
                            sheetDismissOnAdd = false
                            sheetEditMode = true
                            sheetDate = date
                        }
                        else -> {
                            sheetDismissOnAdd = false
                            sheetEditMode = false
                            sheetDate = date
                        }
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "点击日期补记 / 查看记录",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(14.dp))
            MonthSummaryCard(
                monthCount = monthCount,
                activeDays = monthCounts.size,
                avgDays = monthAvgDays,
                maxDayText = maxDayEntry?.let { dateLabel(it.key) + " · " + it.value + " 次" } ?: "还没有记录",
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "最近记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            if (recent.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Text(
                        text = "还没有任何记录，点上面的「记一次」开始吧",
                        modifier = Modifier.padding(18.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                recent.forEach { record ->
                    RecentRecordRow(
                        record = record,
                        onDelete = { pendingDelete = record },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    forgotDate?.let { date ->
        AlertDialog(
            onDismissRequest = { forgotDate = null },
            title = { Text("忘记打卡了吗？") },
            text = { Text(dateLabel(date) + " 还没有记录，要补上吗？") },
            confirmButton = {
                TextButton(onClick = {
                    forgotDate = null
                    sheetDismissOnAdd = false
                    sheetDate = date
                }) { Text("补上") }
            },
            dismissButton = {
                TextButton(onClick = { forgotDate = null }) { Text("算啦") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }

    pendingDelete?.let { record ->
        val date = LocalDate.parse(record.date)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条记录？") },
            text = {
                Text(
                    dateLabel(date) + " " + record.time + " · " +
                        (if (record.isResist) "忍住了" else record.type) + "，删除后无法恢复。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteRecord(record.id)
                    pendingDelete = null
                    toast("已删除")
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }

    sheetDate?.let { date ->
        val existing = records.filter { it.date == date.toString() }
        AddRecordSheet(
            date = date,
            existing = existing,
            editMode = sheetEditMode,
            dismissOnAdd = sheetDismissOnAdd,
            showExisting = !sheetDismissOnAdd,
            types = types,
            onAddType = onAddType,
            onRemoveType = onRemoveType,
            onMoveType = onMoveType,
            onResetTypes = onResetTypes,
            onAdd = { time, type, note ->
                onAddRecord(date, time, type, note)
                val next = existing.count { !it.isResist } + 1
                toast(eggMessage(next) ?: (dateLabel(date) + " 已记录 " + time.format(timeFormatter)))
            },
            onUpdate = { id, time, newType, newNote ->
                onUpdateRecord(id, date, time, newType, newNote)
                toast("已保存修改")
            },
            onDelete = onDeleteRecord,
            onClearDay = {
                onClearDate(date)
                toast("已清空 " + dateLabel(date))
            },
            onResist = { time ->
                onResist(date, time)
                toast(dateLabel(date) + " 补记了「忍住了」· " + time.format(timeFormatter))
            },
            onDismiss = {
                sheetDate = null
                sheetEditMode = false
                sheetDismissOnAdd = false
            },
        )
    }
}
