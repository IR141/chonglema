package com.qingqian.chonglema.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingqian.chonglema.data.GoalConfig
import com.qingqian.chonglema.data.GoalPeriod
import com.qingqian.chonglema.model.CheckInRecord
import com.qingqian.chonglema.stats.ChallengeAttempt
import com.qingqian.chonglema.stats.ChallengeProgress
import com.qingqian.chonglema.stats.computeGoalProgress
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val challengeDateFormatter = DateTimeFormatter.ofPattern("M 月 d 日")
private val challengeShortFormatter = DateTimeFormatter.ofPattern("M/d HH:mm")
private val challengeEndFormatter = DateTimeFormatter.ofPattern("M/d")


private val CHEER_TEXTS = listOf(
    "干得漂亮，这次是真的守住了 💪",
    "说到做到，这就叫自律 🎯",
    "你比想象中更能控制自己 🌱",
    "又赢了一次，保持住这个节奏 🔥",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    records: List<CheckInRecord>,
    today: LocalDate,
    goal: GoalConfig,
    onGoalChange: (GoalConfig) -> Unit,
    progresses: List<ChallengeProgress>,
    history: List<ChallengeProgress>,
    onDeleteHistory: (ChallengeAttempt) -> Unit,
    isCelebrated: (ChallengeAttempt) -> Boolean,
    onStartChallenge: (Int) -> Unit,
    onCancelChallenge: () -> Unit,
    onCelebrate: (ChallengeAttempt) -> Unit,
    onBack: () -> Unit,
) {
    val current = progresses.lastOrNull()
    val toast = rememberAppToastState()
    val scope = rememberCoroutineScope()
    var showCelebration by remember(current?.attempt?.startAt) { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var confirmCloseQuota by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ChallengeProgress?>(null) }

    // ---- 次数配额（改完点「确认」才生效）----
    var quotaDraft by remember(goal) { mutableStateOf(goal) }
    val quotaDirty = quotaDraft != goal
    val quotaProgress = remember(records, goal, today) { computeGoalProgress(records, goal, today) }
    var pickQuotaStart by remember { mutableStateOf(false) }
    var pickQuotaEnd by remember { mutableStateOf(false) }

    // 成功后弹一次鼓励，按「开始时刻」去重，弹过就不再弹
    LaunchedEffect(current?.attempt?.startAt, current?.completed) {
        val done = current
        if (done != null && done.completed && !isCelebrated(done.attempt)) {
            showCelebration = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("自律和配额", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GoalCard(
                    draft = quotaDraft,
                    savedGoal = goal,
                    progress = quotaProgress,
                    dirty = quotaDirty,
                    onPeriodChange = { period ->
                        if (quotaDraft.period != period) {
                            val defaultTarget = when (period) {
                                GoalPeriod.DAY -> 3
                                GoalPeriod.WEEK -> 10
                                GoalPeriod.MONTH -> 30
                                GoalPeriod.CUSTOM -> 30
                            }
                            val target = if (!quotaDraft.enabled && quotaDraft.target <= 1) defaultTarget else quotaDraft.target
                            val start = if (period == GoalPeriod.CUSTOM) (quotaDraft.start ?: today) else null
                            val end = if (period == GoalPeriod.CUSTOM) {
                                quotaDraft.end ?: start!!.plusDays(29)
                            } else {
                                null
                            }
                            quotaDraft = quotaDraft.copy(
                                period = period,
                                target = target,
                                start = start,
                                end = end,
                            )
                        }
                    },
                    onTargetChange = { value ->
                        quotaDraft = quotaDraft.copy(target = value.coerceIn(1, 999))
                    },
                    onPickStart = { pickQuotaStart = true },
                    onPickEnd = { pickQuotaEnd = true },
                    onClearGoal = { confirmCloseQuota = true },
                    onConfirm = {
                        onGoalChange(quotaDraft.copy(enabled = true))
                        toast.show(scope, "配额已保存")
                    },
                )

                when {
                    current == null -> StartCard(
                        onStart = { days ->
                            onStartChallenge(days)
                            toast.show(scope, "已开始 " + days + " 天挑战")
                        },
                    )

                    current.failed -> FailedCard(
                        onRestart = { days ->
                            onStartChallenge(days)
                            toast.show(scope, "已开始 " + days + " 天挑战")
                        },
                    )

                    current.completed -> SuccessCard(
                        progress = current,
                        onAgain = { days ->
                            onStartChallenge(days)
                            toast.show(scope, "已开始 " + days + " 天挑战")
                        },
                    )

                    else -> ActiveCard(
                        progress = current,
                        onCancel = { confirmCancel = true },
                    )
                }

                Text(
                    text = "挑战记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (history.isEmpty()) {
                    Text(
                        text = "还没有已结束的挑战。结束或放弃一次挑战后会出现在这里。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "长按一条记录可以删除",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    history.forEach { item ->
                        HistoryRow(item, onLongPress = { pendingDelete = item })
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
            AppToastHost(state = toast, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (confirmCancel) {
        DestructiveConfirmDialog(
            title = "放弃这次挑战？",
            message = "记录不会动，这次挑战会结束并留在「挑战记录」里。",
            confirmLabel = "确认放弃",
            dismissLabel = "继续挑战",
            onConfirm = {
                confirmCancel = false
                onCancelChallenge()
                toast.show(scope, "已放弃这次挑战")
            },
            onDismiss = { confirmCancel = false },
        )
    }

    if (confirmCloseQuota) {
        DestructiveConfirmDialog(
            title = "关闭次数配额？",
            message = "记录不会动，只是不再按这个周期提醒。",
            confirmLabel = "确认关闭",
            dismissLabel = "继续使用",
            onConfirm = {
                confirmCloseQuota = false
                onGoalChange(goal.copy(enabled = false))
                toast.show(scope, "已关闭配额")
            },
            onDismiss = { confirmCloseQuota = false },
        )
    }

    pendingDelete?.let { target ->
        DestructiveConfirmDialog(
            title = "删除这条挑战记录？",
            message = target.days.toString() + " 天挑战 · " +
                target.attempt.startAt.format(challengeShortFormatter) + " 开始 · " +
                target.endDate.format(challengeEndFormatter) + " 结束。删除后无法恢复。",
            confirmLabel = "确认删除",
            dismissLabel = "取消",
            onConfirm = {
                pendingDelete = null
                onDeleteHistory(target.attempt)
                toast.show(scope, "已删除这条挑战记录")
            },
            onDismiss = { pendingDelete = null },
        )
    }

    val celebrated = current
    if (showCelebration && celebrated != null) {
        AlertDialog(
            onDismissRequest = {
                showCelebration = false
                onCelebrate(celebrated.attempt)
            },
            title = { Text("挑战成功 🎉") },
            text = {
                Text(CHEER_TEXTS[Math.floorMod(celebrated.attempt.days, CHEER_TEXTS.size)])
            },
            // 右下角「确认」只关掉弹窗；左下角「去设置下一个挑战」留在成功卡继续设置。
            confirmButton = {
                TextButton(onClick = {
                    showCelebration = false
                    onCelebrate(celebrated.attempt)
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCelebration = false
                    onCelebrate(celebrated.attempt)
                }) { Text("去设置下一个挑战") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }

    if (pickQuotaStart) {
        QuotaDatePickerDialog(
            initial = quotaDraft.start ?: today,
            onDismiss = { pickQuotaStart = false },
            onConfirm = { date ->
                val end = quotaDraft.end ?: date.plusDays(29)
                quotaDraft = quotaDraft.copy(start = date, end = if (end.isBefore(date)) date else end)
                pickQuotaStart = false
            },
        )
    }
    if (pickQuotaEnd) {
        QuotaDatePickerDialog(
            initial = quotaDraft.end ?: quotaDraft.start ?: today,
            onDismiss = { pickQuotaEnd = false },
            onConfirm = { date ->
                val start = quotaDraft.start ?: today
                quotaDraft = quotaDraft.copy(start = start, end = if (date.isBefore(start)) start else date)
                pickQuotaEnd = false
            },
        )
    }
}

@Composable
private fun StartCard(onStart: (Int) -> Unit) {
    AutonomyCard(title = "挑战") {
        Text(
            text = "还没有进行中的挑战，选一个天数再确认。",
            style = MaterialTheme.typography.bodyMedium,
        )
        ChallengeDaysPicker(
            onConfirm = onStart,
            confirmLabel = "确认",
        )
    }
}

/**
 * 挑战天数选择：和次数配额一样，先选天数，再点下方确认按钮才真正开始，
 * 避免误触 7/14/30 天按钮直接开启挑战。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChallengeDaysPicker(
    onConfirm: (Int) -> Unit,
    confirmLabel: String,
) {
    var custom by rememberSaveable { mutableStateOf("") }
    var customSelected by rememberSaveable { mutableStateOf(false) }
    var selectedPreset by rememberSaveable { mutableStateOf(7) }
    val customDays = custom.toIntOrNull()?.coerceIn(0, 999)
    val chosenDays = if (customSelected) customDays else selectedPreset

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(7, 14, 30).forEach { days ->
            FilterChip(
                selected = !customSelected && selectedPreset == days,
                onClick = {
                    customSelected = false
                    selectedPreset = days
                },
                label = { Text(days.toString() + " 天") },
            )
        }
        FilterChip(
            selected = customSelected,
            onClick = { customSelected = true },
            label = { Text("自定义") },
        )
    }

    if (customSelected) {
        OutlinedTextField(
            value = custom,
            onValueChange = { text -> custom = text.filter { it.isDigit() }.take(3) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("自定义天数（0–999）") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }

    Button(
        onClick = { chosenDays?.let(onConfirm) },
        enabled = chosenDays != null,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = confirmLabel,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 2.dp),
        )
    }
}

@Composable
private fun ActiveCard(
    progress: ChallengeProgress,
    onCancel: () -> Unit,
) {
    AutonomyCard(
        title = "挑战",
        trailing = {
            AutonomyDangerButton(text = "放弃挑战", onClick = onCancel)
        },
    ) {
        Text(
            text = "进行中 · " + progress.days + " 天挑战",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = progress.dayIndex.toString(),
                fontSize = 40.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = " / " + progress.days + " 天",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "已守住 " + progress.cleanDays + " 天",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        LinearProgressIndicator(
            progress = { progress.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            drawStopIndicator = {},
        )
        Text(
            text = progress.startDate.format(challengeDateFormatter) + " 开始 · " +
                progress.endDate.format(challengeDateFormatter) + " 结束",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (progress.lastDay) "今晚守住就成功了" else "还剩 " + progress.daysLeft + " 天",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (progress.lastDay) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FailedCard(
    onRestart: (Int) -> Unit,
) {
    // 失败后不再显示「挑战中断 · 坚持 N 天」、失败日期说明、「重新开始」和「移除这次挑战」，
    // 只保留和开始挑战一样的选天数 + 确认入口。
    AutonomyCard(title = "挑战") {
        ChallengeDaysPicker(
            onConfirm = onRestart,
            confirmLabel = "确认",
        )
    }
}

@Composable
private fun SuccessCard(
    progress: ChallengeProgress,
    onAgain: (Int) -> Unit,
) {
    AutonomyCard(title = "挑战") {
        Text(
            text = "上一次挑战 · " + progress.days + " 天",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = progress.startDate.format(challengeDateFormatter) + " → " +
                progress.endDate.format(challengeDateFormatter),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "开始下一个挑战",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
        ChallengeDaysPicker(
            onConfirm = onAgain,
            confirmLabel = "确认",
        )
    }
}

@Composable
private fun HistoryRow(progress: ChallengeProgress, onLongPress: () -> Unit) {
    val cancelled = progress.attempt.cancelled
    val (emoji, status) = when {
        cancelled -> "🚫" to "已放弃"
        progress.completed -> "✅" to "成功"
        progress.failed -> "❌" to ("坚持 " + progress.cleanDays + " 天")
        else -> "⏳" to "进行中"
    }
    val statusColor = when {
        cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
        progress.completed -> MaterialTheme.colorScheme.onSurfaceVariant
        progress.failed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(progress.attempt.startAt, progress.attempt.days) {
                detectTapGestures(onLongPress = { onLongPress() })
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(emoji, fontSize = 20.sp) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = progress.days.toString() + " 天挑战",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = progress.attempt.startAt.format(challengeShortFormatter) + " 开始 · " +
                        progress.endDate.format(challengeEndFormatter) + " 结束",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                textAlign = TextAlign.End,
            )
        }
    }
}
