package com.qingqian.chonglema.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.qingqian.chonglema.stats.GoalProgress
import com.qingqian.chonglema.stats.computeGoalProgress
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val quotaDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

/** 次数配额：一段时间内最多冲几次。达到/超过只提醒，不禁止记录。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotaScreen(
    records: List<CheckInRecord>,
    today: LocalDate,
    goal: GoalConfig,
    onGoalChange: (GoalConfig) -> Unit,
    onBack: () -> Unit,
) {
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    var confirmCloseQuota by remember { mutableStateOf(false) }
    // 编辑草稿：周期 / 次数 / 自定义日期都先改草稿，点「确认」才真正保存。
    var draft by remember(goal) { mutableStateOf(goal) }
    val dirty = draft != goal
    val toast = rememberAppToastState()
    val scope = rememberCoroutineScope()
    val progress = remember(records, goal, today) { computeGoalProgress(records, goal, today) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("次数配额", fontWeight = FontWeight.SemiBold) },
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
                    progress = progress,
                    dirty = dirty,
                    onPeriodChange = { period ->
                        if (draft.period != period) {
                            val defaultTarget = when (period) {
                                GoalPeriod.DAY -> 3
                                GoalPeriod.WEEK -> 10
                                GoalPeriod.MONTH -> 30
                                GoalPeriod.CUSTOM -> 30
                            }
                            // 还没开启、次数还是初始的 1 时，切周期顺手给个默认值；开了就别动
                            val target = if (!draft.enabled && draft.target <= 1) defaultTarget else draft.target
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
                        draft = draft.copy(target = value.coerceIn(1, 999))
                    },
                    onPickStart = { pickStart = true },
                    onPickEnd = { pickEnd = true },
                    onClearGoal = { confirmCloseQuota = true },
                    onConfirm = {
                        onGoalChange(draft.copy(enabled = true))
                        toast.show(scope, "配额已保存")
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
            AppToastHost(state = toast, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (pickStart) {
        QuotaDatePickerDialog(
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
        QuotaDatePickerDialog(
            initial = draft.end ?: draft.start ?: today,
            onDismiss = { pickEnd = false },
            onConfirm = { date ->
                val start = draft.start ?: today
                draft = draft.copy(start = start, end = if (date.isBefore(start)) start else date)
                pickEnd = false
            },
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GoalCard(
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
    // 配额次数可以直接输入；输入框自己保存文本，避免输入 0 或中间态时被强行改回
    var targetText by remember(draft.target) { mutableStateOf(draft.target.toString()) }
    AutonomyCard(
        title = "次数配额",
        trailing = {
            if (savedGoal.isSet) {
                AutonomyDangerButton(text = "关闭配额", onClick = onClearGoal)
            }
        },
    ) {
        if (progress != null) {
                Text(
                    text = progress.periodLabel + " 已冲 " + progress.doneCount + " 次，配额 " + progress.target + " 次",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when {
                        progress.over -> "已超配额 " + (progress.doneCount - progress.target) + " 次"
                        progress.reached -> "正好到配额"
                        else -> "还剩 " + progress.remaining + " 次"
                    },
                    style = MaterialTheme.typography.bodyMedium,
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
                    text = "还没有设置配额：选一个周期，再定一个次数。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 设置好配额后只保留状态和进度，不再显示周期 / 次数等设置项，
            // 需要修改时先点右上角「关闭配额」，再重新设置。
            val showConfig = !savedGoal.isSet || dirty
            if (showConfig) {
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
                        text = "配额次数",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            val next = (draft.target - 1).coerceAtLeast(1)
                            targetText = next.toString()
                            onTargetChange(next)
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                    ) { Text("－", style = MaterialTheme.typography.titleMedium) }
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(3)
                            if (digits.isEmpty()) {
                                // 最少 1 次：删空时直接回到 1，避免出现 0 或空白配额
                                targetText = "1"
                                if (draft.target != 1) onTargetChange(1)
                            } else {
                                targetText = digits
                                val parsed = digits.toIntOrNull() ?: 1
                                if (parsed != draft.target) onTargetChange(parsed)
                            }
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
                            Text("开始 " + (draft.start?.format(quotaDateFormatter) ?: "—"))
                        }
                        OutlinedButton(
                            onClick = onPickEnd,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("截止 " + (draft.end?.format(quotaDateFormatter) ?: "—"))
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

                // 未设置配额、或改了周期 / 次数 / 日期时才出现确认按钮。
                if (dirty || !draft.enabled) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
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
}

internal fun periodText(period: GoalPeriod): String = when (period) {
    GoalPeriod.DAY -> "每天"
    GoalPeriod.WEEK -> "每周"
    GoalPeriod.MONTH -> "每月"
    GoalPeriod.CUSTOM -> "自定义"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuotaDatePickerDialog(
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
