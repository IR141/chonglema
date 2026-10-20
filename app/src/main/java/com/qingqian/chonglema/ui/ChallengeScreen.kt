package com.qingqian.chonglema.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingqian.chonglema.model.CheckInRecord
import com.qingqian.chonglema.stats.Badge
import com.qingqian.chonglema.stats.ChallengeProgress
import com.qingqian.chonglema.stats.ChallengeState
import com.qingqian.chonglema.stats.CheckInStats
import com.qingqian.chonglema.stats.computeBadges
import com.qingqian.chonglema.stats.computeChallengeProgress
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val challengeDateFormatter = DateTimeFormatter.ofPattern("M 月 d 日")

/** 挑战 + 徽章页。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    records: List<CheckInRecord>,
    today: LocalDate,
    stats: CheckInStats,
    challenge: ChallengeState?,
    onStartChallenge: (Int) -> Unit,
    onCancelChallenge: () -> Unit,
    onBack: () -> Unit,
) {
    // 只在 records / 挑战状态 / 日期变化时重算，而不是每次重组都算一遍
    val progress = remember(records, challenge, today) {
        challenge?.let { computeChallengeProgress(records, it, today) }
    }
    val badges = remember(stats) { computeBadges(stats) }
    var pendingStart by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("挑战与徽章", fontWeight = FontWeight.Bold) },
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
            if (progress == null) {
                StartChallengeCard(
                    warnTodayDone = stats.todayCount > 0,
                    onStartChallenge = { days ->
                        if (stats.todayCount > 0) pendingStart = days else onStartChallenge(days)
                    },
                )
            } else {
                ActiveChallengeCard(progress = progress, onCancel = onCancelChallenge)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "徽章 · 已解锁 " + badges.count { it.unlocked } + " / " + badges.size,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                badges.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { badge ->
                            BadgeCard(badge = badge, modifier = Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    pendingStart?.let { days ->
        AlertDialog(
            onDismissRequest = { pendingStart = null },
            title = { Text("今天已经有记录了") },
            text = { Text("从今天开始算的话，挑战会立刻判定失败。要不要先去当日编辑里删掉今天的记录，或者换个日子再开始？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingStart = null
                    onStartChallenge(days)
                }) { Text("仍然开始") }
            },
            dismissButton = {
                TextButton(onClick = { pendingStart = null }) { Text("算了") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}

@Composable
private fun StartChallengeCard(warnTodayDone: Boolean, onStartChallenge: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "开始一个挑战",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "规则：从今天开始，连续 N 天没有「冲了」的记录就算成功。中途记录了就算挑战失败，可以重新开始。" +
                    if (warnTodayDone) "\n今天已经有记录，开始后会立刻判定失败。" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(7, 14, 30).forEach { days ->
                    OutlinedButton(
                        onClick = { onStartChallenge(days) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(days.toString() + " 天")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveChallengeCard(progress: ChallengeProgress, onCancel: () -> Unit) {
    val state = progress.state
    val statusText = when {
        progress.failed -> "挑战失败：在 " + (progress.failedOn?.format(challengeDateFormatter) ?: "") + " 记录了"
        progress.completed -> "挑战成功！连续 " + state.days + " 天没有记录"
        else -> "进行中 · 还剩 " + progress.remainingDays + " 天"
    }
    val accent = when {
        progress.failed -> MaterialTheme.colorScheme.error
        progress.completed -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.days.toString() + " 天挑战",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "开始于 " + state.start.format(challengeDateFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = progress.successDays.toString(),
                    fontSize = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                )
                Text(
                    text = " / " + state.days + " 天",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress.progressFraction },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("结束这个挑战")
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: Badge, modifier: Modifier) {
    val alpha = if (badge.unlocked) 1f else 0.4f
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = if (badge.unlocked) badge.emoji else "🔒", fontSize = 18.sp)
            }
            Spacer(Modifier.size(10.dp))
            Column {
                Text(
                    text = badge.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
                Text(
                    text = badge.desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            }
        }
    }
}