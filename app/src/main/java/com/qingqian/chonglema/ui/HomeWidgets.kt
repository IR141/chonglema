package com.qingqian.chonglema.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingqian.chonglema.model.CheckInRecord
import java.time.LocalDateTime
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.delay

/** 首页大卡片下面那排小入口。 */
@Composable
internal fun EntryButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 「距上次」小卡：每 10 秒刷新一次，分钟数会自己往上走。 */
@Composable
internal fun SinceLastStatTile(
    modifier: Modifier,
    lastAt: LocalDateTime?,
    lastSeenText: String,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(lastAt) {
        now = LocalDateTime.now()
        while (true) {
            delay(10_000)
            now = LocalDateTime.now()
        }
    }
    StatTile(
        modifier = modifier,
        value = lastAt?.let { formatSinceLast(it, now) } ?: "—",
        label = "距上次",
        sub = lastSeenText,
    )
}

/** 首页三张统计小卡：数值按长度缩字号，三张卡用 IntrinsicSize.Max 拉平。 */
@Composable
internal fun StatTile(
    modifier: Modifier,
    value: String,
    label: String,
    sub: String,
) {
    val valueSize = when {
        value.length >= 7 -> 14.sp
        value.length == 6 -> 16.sp
        value.length == 5 -> 18.sp
        else -> 22.sp
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = value,
                fontSize = valueSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun MonthSummaryCard(
    monthCount: Int,
    activeDays: Int,
    avgDays: Double?,
    maxDayText: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "本月小结",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            SummaryLine("本月总次数", monthCount.toString() + " 次")
            SummaryLine("打卡天数", activeDays.toString() + " 天")
            SummaryLine(
                "平均一次",
                avgDays?.let { String.format(Locale.US, "%.1f 天", it) } ?: "—",
            )
            SummaryLine("最猛的一天", maxDayText)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun RecentRecordRow(
    record: CheckInRecord,
    onDelete: () -> Unit,
) {
    val date = LocalDate.parse(record.date)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.time,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (record.isResist) "忍住了" else record.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (record.isResist) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = dateLabel(date) + if (record.note.isBlank()) "" else (" · " + record.note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
