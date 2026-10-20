package com.qingqian.chonglema.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

private val WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

/** 把一个月排成周列表，周一为一周起点，空位用 null 补齐。 */
fun buildMonthWeeks(month: YearMonth): List<List<LocalDate?>> {
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value + 6) % 7
    val cells = mutableListOf<LocalDate?>()
    repeat(lead) { cells.add(null) }
    for (day in 1..month.lengthOfMonth()) cells.add(month.atDay(day))
    while (cells.size % 7 != 0) cells.add(null)
    return cells.chunked(7)
}

@Composable
fun CalendarGrid(
    month: YearMonth,
    counts: Map<LocalDate, Int>,
    resistCounts: Map<LocalDate, Int> = emptyMap(),
    today: LocalDate,
    onDateClick: (LocalDate) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            WEEK_LABELS.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        buildMonthWeeks(month).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f).padding(2.dp)) {
                        if (date == null) {
                            Spacer(modifier = Modifier.fillMaxWidth().aspectRatio(0.82f))
                        } else {
                            DayCell(
                                date = date,
                                count = counts[date] ?: 0,
                                resist = resistCounts[date] ?: 0,
                                isToday = date == today,
                                isFuture = date.isAfter(today),
                                onClick = { onDateClick(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    count: Int,
    resist: Int,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)
    val background = when {
        count >= 5 -> primary.copy(alpha = 0.62f)
        count >= 3 -> primary.copy(alpha = 0.42f)
        count > 0 -> primary.copy(alpha = 0.24f)
        isToday -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clip(shape)
            .background(background)
            .border(
                border = if (isToday) BorderStroke(1.5.dp, primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                shape = shape,
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                count > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        if (count > 0 || resist > 0) {
            // 次数和忍住在同一行；两位数时缩小字号，避免挤出格子
            val countText = if (count > 0) "x" + count else ""
            val resistText = if (resist > 0) "忍" + resist else ""
            val chars = countText.length + resistText.length
            val smallFont = when {
                chars >= 7 -> 7.sp
                chars >= 5 -> 8.sp
                else -> 9.sp
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (countText.isNotEmpty()) {
                    Text(
                        text = countText,
                        fontSize = smallFont,
                        lineHeight = (smallFont.value + 1).sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    )
                }
                if (resistText.isNotEmpty()) {
                    Text(
                        text = resistText,
                        fontSize = smallFont,
                        lineHeight = (smallFont.value + 1).sp,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}
