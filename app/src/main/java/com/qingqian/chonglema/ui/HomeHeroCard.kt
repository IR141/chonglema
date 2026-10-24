package com.qingqian.chonglema.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingqian.chonglema.stats.ChallengeProgress
import com.qingqian.chonglema.ui.theme.LocalDarkTheme
import java.time.LocalDate

/** 首页顶部的大卡片：今日次数、挑战进度和两个主按钮。 */
@Composable
internal fun TodayHeroCard(
    count: Int,
    resistCount: Int,
    lastTime: String?,
    challenge: ChallengeProgress?,
    challengeHidden: Boolean = false,
    onCheckIn: () -> Unit,
    onResist: () -> Unit,
    onOpenReport: () -> Unit,
    onChallengeClick: () -> Unit,
) {
    val isDark = LocalDarkTheme.current
    val haptics = LocalHapticFeedback.current
    // 数字翻动一下，别硬邦邦地跳
    val animatedCount by animateIntAsState(
        targetValue = count,
        animationSpec = tween(durationMillis = 320),
        label = "todayCount",
    )
    val gradient = Brush.linearGradient(
        colors = if (isDark) {
            listOf(Color(0xFFFF5C8A), Color(0xFF8E5CFF))
        } else {
            listOf(Color(0xFF2F7DFF), Color(0xFF5B7CFF))
        },
    )
    // 挑战相关点击统一进「自律和配额」页，选天数并点确认后才开始
    val challengeAction = onChallengeClick
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(gradient)
            .clickable { onOpenReport() },
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "今天 · " + dateLabel(LocalDate.now()),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = animatedCount.toString(),
                    fontSize = 56.sp,
                    lineHeight = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                )
                Text(
                    text = " 次",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Spacer(Modifier.weight(1f))
                // 挑战失败后不再显示「挑战中断 / 坚持 N 天」，只在下方保留「挑战失败」提示。
                if (!challengeHidden && challenge?.failed != true) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = challengeAction)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = challengeTitle(challenge),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                        Text(
                            text = challengeValue(challenge),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }
                }
            }
            Text(
                text = buildString {
                    if (count == 0 && resistCount == 0) {
                        append("今天还没有打卡，记录第一次吧")
                    } else if (count == 0) {
                        append("今天一次都没冲，继续保持")
                    } else {
                        append("上一发：" + (lastTime ?: "--:--"))
                    }
                    if (resistCount > 0) {
                        append(" · 忍住 " + resistCount + " 次")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f),
            )
            if (!challengeHidden) {
                Spacer(Modifier.height(10.dp))
                if (challenge != null) {
                    LinearProgressIndicator(
                    progress = { challenge.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.28f),
                    drawStopIndicator = {},
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = challengeHint(challenge),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (challenge.failed) Color(0xFFFFDCDC)
                    else Color.White.copy(alpha = 0.85f),
                    fontWeight = if (challenge.failed) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = challengeAction)
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                )
                } else {
                    Text(
                        text = "还没有进行中的挑战 · 点这里选天数",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = challengeAction)
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCheckIn()
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = if (isDark) Color(0xFFB3124B) else Color(0xFF1D4ED8),
                    ),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("记一次", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onResist()
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("忍住了", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun challengeTitle(challenge: ChallengeProgress?): String = when {
    challenge == null -> "挑战"
    challenge.completed -> "挑战成功"
    challenge.failed -> "挑战中断"
    challenge.lastDay -> "挑战 · 最后一天"
    else -> "挑战进度"
}

private fun challengeValue(challenge: ChallengeProgress?): String = when {
    challenge == null -> "未开始"
    challenge.completed -> challenge.days.toString() + " / " + challenge.days + " 天"
    challenge.failed -> "坚持 " + challenge.cleanDays + " 天"
    else -> challenge.dayIndex.toString() + " / " + challenge.days + " 天"
}

private fun challengeHint(challenge: ChallengeProgress): String = when {
    challenge.completed -> "挑战成功 🎉 点这里再来一次"
    challenge.failed -> "挑战失败"
    challenge.lastDay -> "今晚守住就成功了"
    else -> "还剩 " + challenge.daysLeft + " 天"
}
