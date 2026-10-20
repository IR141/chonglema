package com.qingqian.chonglema.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qingqian.chonglema.data.GoalConfig
import com.qingqian.chonglema.data.GoalPeriod
import com.qingqian.chonglema.model.CheckInRecord
import com.qingqian.chonglema.stats.ChallengeState
import com.qingqian.chonglema.stats.GoalProgress
import com.qingqian.chonglema.stats.computeChallengeProgress
import com.qingqian.chonglema.stats.computeGoalProgress
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val goalDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

/** 目标与回顾：自律目标 + 挑战与徽章 + 年度报告。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    records: List<CheckInRecord>,
    today: LocalDate,
    goal: GoalConfig,
    onGoalChange: (GoalConfig) -> Unit,
    challenge: ChallengeState?,
    onOpenChallenge: () -> Unit,
    onOpenAnnual: () -> Unit,
    onBack: () -> Unit,
) {
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    // 编辑草稿：周期 / 次数 / 自定义日期都先改草稿，点「确认」才真正保存。
    var draft by remember(goal) { mutableStateOf(goal) }
    val dirty = draft != goal
    val toast = rememberAppToastState()
    val scope = rememberCoroutineScope()
    val goalProgress = remember(records, goal, today) { computeGoalProgress(records, goal, today) }
    val challengeProgress = remember(records, challenge, today) {
        challenge?.let { computeChallengeProgress(records, it, today) }
    }
    val challengeSummary = when {
        challengeProgress == null -> "还没有进行中的挑战，点进去开始"
        challengeProgress.failed -> "挑战失败：在 " + challengeProgress.failedOn + " 记录了"
        challengeProgress.completed -> "挑战成功！连续 " + challengeProgress.state.days + " 天"
        else -> "进行中 · " + challengeProgress.successDays + " / " + challengeProgress.state.days + " 天"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("目标与回顾", fontWeight = FontWeight.SemiBold) },
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
                    draft = draft,
                    savedGoal = goal,
                    progress = goalProgress,
                    dirty = dirty,
                    onPeriodChange = { period ->
                        if (draft.period != period) {
                            val defaultTarget = when (period) {
                                GoalPeriod.DAY -> 3
                                GoalPeriod.WEEK -> 10
                                GoalPeriod.MONTH -> 30
                                GoalPeriod.CUSTOM -> 30
                            }
                            // 还没开启目标、次数又是 0 时，切周期顺手给个默认值；0 是有效目标，开了就别动
                            val target = if (!draft.enabled && draft.target <= 0) defaultTarget else draft.target
                            val start = if (period == GoalPeriod.CUSTOM) (draft.start ?: today) else null
                            val end = if (period == GoalPeriod.CUSTOM) {
                                draft.end ?: start!!.plusDays(29)
                            } else {
                                null
                            }
                            draft = draft.copy(
                                period = period,
                                target = target,
                                start = start,
                                end = end,
                            )
                        }
                    },
                    onTargetChange = { value ->
                        draft = draft.copy(target = value.coerceIn(0, 999))
                    },
                    onPickStart = { pickStart = true },
                    onPickEnd = { pickEnd = true },
                    onClearGoal = { onGoalChange(goal.copy(enabled = false)) },
                    onConfirm = {
                        onGoalChange(draft.copy(enabled = true))
                        toast.show(scope, "自律目标已保存")
                    },
                )
                EntriesCard(
                    challengeSummary = challengeSummary,
                    onOpenChallenge = onOpenChallenge,
                    onOpenAnnual = onOpenAnnual,
                )
                Spacer(Modifier.height(8.dp))
            }
            AppToastHost(state = toast, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (pickStart) {
        GoalDatePickerDialog(
            initial = draft.start ?: today,
            onDismiss = { pickStart = false },
            onConfirm = { date ->
                val end = draft.end ?: date.plusDays(29)
                draft = draft.copy(start = date, end = if (end.isBefore(date)) date else end)
                pickStart = false
            },
        )
    }
    if (pickEnd) {
        GoalDatePickerDialog(
            initial = draft.end ?: draft.start ?: today,
            onDismiss = { pickEnd = false },
            onConfirm = { date ->
                val start = draft.start ?: today
                draft = draft.copy(start = start, end = if (date.isBefore(start)) start else date)
                pickEnd = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalCard(
    draft: GoalConfig,
    savedGoal: GoalConfig,
    progress: GoalProgress?,
    dirty: Boolean,
    onPeriodChange: (GoalPeriod) -> Unit,
    onTargetChange: (Int) -> Unit,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onClearGoal: () -> Unit,
    onConfirm: () -> Unit,
) {
    // 目标次数可以直接输入；输入框自己保存文本，避免输入 0 或中间态时被强行改回
    var targetText by remember(draft.target) { mutableStateOf(draft.target.toString()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "自律目标",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (savedGoal.isSet) {
                    TextButton(onClick = onClearGoal, contentPadding = PaddingValues(0.dp)) {
                        Text("关闭目标", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (progress != null) {
                Text(
                    text = progress.periodLabel + " 已冲 " + progress.doneCount + " 次，目标 " + progress.target + " 次",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = when {
                        progress.over -> "已超目标 " + (progress.doneCount - progress.target) + " 次"
                        progress.reached -> "不能再冲了"
                        else -> "还能冲 " + progress.remaining + " 次"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (progress.over) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    drawStopIndicator = {},
                )
            } else {
                Text(
                    text = "还没有设置自律目标：选一个周期，再定一个次数。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = draft.period == period,
                        onClick = { onPeriodChange(period) },
                        label = { Text(periodText(period)) },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "目标次数",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        val next = (draft.target - 1).coerceAtLeast(0)
                        targetText = next.toString()
                        onTargetChange(next)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) { Text("－", style = MaterialTheme.typography.titleMedium) }
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(3)
                        targetText = digits
                        val parsed = digits.toIntOrNull() ?: 0
                        if (parsed != draft.target) onTargetChange(parsed)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.width(84.dp),
                    shape = RoundedCornerShape(12.dp),
                )
                TextButton(
                    onClick = {
                        val next = (draft.target + 1).coerceAtMost(999)
                        targetText = next.toString()
                        onTargetChange(next)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) { Text("＋", style = MaterialTheme.typography.titleMedium) }
            }

            if (draft.period == GoalPeriod.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPickStart,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("开始 " + (draft.start?.format(goalDateFormatter) ?: "—"))
                    }
                    OutlinedButton(
                        onClick = onPickEnd,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("截止 " + (draft.end?.format(goalDateFormatter) ?: "—"))
                    }
                }
            }

            if (dirty) {
                Text(
                    text = "有未保存的修改，点下方「确认」生效",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Button(
                onClick = onConfirm,
                // 目标未开启时也允许确认：这样「0 次」也能作为目标保存下来
                enabled = dirty || !draft.enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = "确认",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun EntriesCard(
    challengeSummary: String,
    onOpenChallenge: () -> Unit,
    onOpenAnnual: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenChallenge)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("挑战与徽章", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = challengeSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAnnual)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("年度报告", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "每年一张成绩单，可以复制成文字",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun periodText(period: GoalPeriod): String = when (period) {
    GoalPeriod.DAY -> "每天"
    GoalPeriod.WEEK -> "每周"
    GoalPeriod.MONTH -> "每月"
    GoalPeriod.CUSTOM -> "自定义"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    ) {
        DatePicker(state = state)
    }
}
