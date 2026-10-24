package com.qingqian.chonglema.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingqian.chonglema.stats.PERIOD_LABELS
import com.qingqian.chonglema.stats.PeriodReport
import com.qingqian.chonglema.stats.ReportPeriod
import com.qingqian.chonglema.stats.ReportRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDateFormatter = DateTimeFormatter.ofPattern("M/d")

/** 趋势对比：本期 vs 上一段（上一年 / 上一月 / 上一周）。 */
@Composable
internal fun TrendCard(
    current: PeriodReport,
    previous: PeriodReport,
    previousTitle: String,
    previousLabel: String,
) {
    val avgDelta = if (current.avgIntervalDays != null && previous.avgIntervalDays != null) {
        current.avgIntervalDays - previous.avgIntervalDays
    } else {
        null
    }
    val rows = listOf(
        TrendRow(
            label = "冲的次数",
            current = current.doneCount.toString() + " 次",
            previous = previous.doneCount.toString() + " 次",
            delta = (current.doneCount - previous.doneCount).toDouble(),
            decimals = 0,
            upIsBad = true,
        ),
        TrendRow(
            label = "忍住次数",
            current = current.resistCount.toString() + " 次",
            previous = previous.resistCount.toString() + " 次",
            delta = (current.resistCount - previous.resistCount).toDouble(),
            decimals = 0,
            upIsBad = false,
        ),
        TrendRow(
            label = "打卡天数",
            current = current.doneDays.toString() + " 天",
            previous = previous.doneDays.toString() + " 天",
            delta = (current.doneDays - previous.doneDays).toDouble(),
            decimals = 0,
            upIsBad = null,
        ),
        TrendRow(
            label = "平均一次",
            current = current.avgIntervalDays?.let { String.format(Locale.US, "%.1f 天", it) } ?: "—",
            previous = previous.avgIntervalDays?.let { String.format(Locale.US, "%.1f 天", it) } ?: "—",
            delta = avgDelta,
            decimals = 1,
            upIsBad = false,
        ),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "趋势对比",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "对比 " + previousTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            rows.forEach { row ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = row.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        val deltaColor = when {
                            row.delta == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            row.upIsBad == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            row.upIsBad == true -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                        Text(
                            text = row.deltaText(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = deltaColor,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = row.current,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "  " + previousLabel + " " + row.previous,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private data class TrendRow(
    val label: String,
    val current: String,
    val previous: String,
    val delta: Double?,
    val decimals: Int,
    val upIsBad: Boolean?,
) {
    fun deltaText(): String {
        val d = delta ?: return "—"
        if (kotlin.math.abs(d) < 0.05) return "持平"
        val text = if (decimals == 0) {
            kotlin.math.abs(d).toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", kotlin.math.abs(d))
        }
        return if (d > 0) "▲ " + text else "▼ " + text
    }

}

@Composable
internal fun HeatmapCard(
    range: ReportRange,
    report: PeriodReport,
    doneCounts: Map<LocalDate, Int>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
) {
    val title = when (range.period) {
        ReportPeriod.YEAR -> "年度热力图"
        ReportPeriod.MONTH -> "本月热力图"
        ReportPeriod.WEEK -> "本周热力图"
        ReportPeriod.ALL -> "热力图"
    }
    val badge = when (range.period) {
        ReportPeriod.YEAR -> (range.heatmapYear ?: today.year).toString() + " 年"
        else -> range.title
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "共 " + report.doneCount + " 次 · 打卡 " + report.doneDays + " 天",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            when (range.period) {
                ReportPeriod.YEAR -> YearHeatmap(
                    year = range.heatmapYear ?: today.year,
                    counts = doneCounts,
                    today = today,
                    onDayClick = onDayClick,
                )
                ReportPeriod.MONTH -> MonthHeatmap(
                    firstDay = range.startDate,
                    counts = doneCounts,
                    today = today,
                    onDayClick = onDayClick,
                )
                ReportPeriod.WEEK -> WeekHeatmap(
                    start = range.startDate,
                    counts = doneCounts,
                    today = today,
                    onDayClick = onDayClick,
                )
                ReportPeriod.ALL -> Unit
            }
        }
    }
}

@Composable
private fun YearHeatmap(
    year: Int,
    counts: Map<LocalDate, Int>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (month in 1..12) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = month.toString() + " 月",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant,
                    modifier = Modifier.width(30.dp),
                )
                // 每个月一行 31 个格子，交给一个 Canvas 画，避免 372 个 Box 节点
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .pointerInput(year, month) {
                            detectTapGestures { offset ->
                                val cellWidth = size.width / 31f
                                val day = (offset.x / cellWidth).toInt() + 1
                                if (day in 1..31) {
                                    val date = runCatching { LocalDate.of(year, month, day) }.getOrNull()
                                    if (date != null) onDayClick(date)
                                }
                            }
                        },
                ) {
                    val cellWidth = size.width / 31f
                    val inset = 1.dp.toPx()
                    val radius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    for (day in 1..31) {
                        val date = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: continue
                        val count = counts[date] ?: 0
                        val color = when {
                            date.isAfter(today) -> surfaceVariant.copy(alpha = 0.25f)
                            count >= 4 -> primary.copy(alpha = 0.95f)
                            count == 3 -> primary.copy(alpha = 0.72f)
                            count == 2 -> primary.copy(alpha = 0.52f)
                            count == 1 -> primary.copy(alpha = 0.3f)
                            date == today -> surfaceVariant
                            else -> surfaceVariant.copy(alpha = 0.45f)
                        }
                        drawRoundRect(
                            color = color,
                            topLeft = Offset((day - 1) * cellWidth + inset, 0f),
                            size = Size((cellWidth - inset * 2).coerceAtLeast(1f), size.height),
                            cornerRadius = radius,
                        )
                    }
                }
            }
        }
    }
}

/** 本月热力图：7 列等比方格，只靠颜色深浅表示次数，今天描边。 */
@Composable
private fun MonthHeatmap(
    firstDay: LocalDate,
    counts: Map<LocalDate, Int>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val first = firstDay.withDayOfMonth(1)
    val lead = first.dayOfWeek.value - 1
    val days = first.lengthOfMonth()
    val rows = (lead + days + 6) / 7
    val spacing = 4.dp
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cell = (maxWidth - spacing * 6) / 7
            // 整月的格子用一个 Canvas 画（含日期文字），节点数从 42 降到 1
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cell * rows + spacing * (rows - 1))
                    .pointerInput(first, rows) {
                        detectTapGestures { offset ->
                            val spacingPx = spacing.toPx()
                            val cellPx = (size.width - spacingPx * 6) / 7
                            val col = (offset.x / (cellPx + spacingPx)).toInt().coerceIn(0, 6)
                            val row = (offset.y / (cellPx + spacingPx)).toInt().coerceIn(0, rows - 1)
                            val dayOfMonth = row * 7 + col - lead + 1
                            if (dayOfMonth in 1..days) onDayClick(first.withDayOfMonth(dayOfMonth))
                        }
                    },
            ) {
                val spacingPx = spacing.toPx()
                val cellPx = (size.width - spacingPx * 6) / 7
                val radius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                val strokeWidth = 1.5.dp.toPx()
                val textPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 12.sp.toPx()
                }
                for (row in 0 until rows) {
                    for (col in 0 until 7) {
                        val dayOfMonth = row * 7 + col - lead + 1
                        if (dayOfMonth !in 1..days) continue
                        val date = first.withDayOfMonth(dayOfMonth)
                        val count = counts[date] ?: 0
                        val isFuture = date.isAfter(today)
                        val x = col * (cellPx + spacingPx)
                        val y = row * (cellPx + spacingPx)
                        val bg = when {
                            count >= 4 -> primary.copy(alpha = 0.92f)
                            count == 3 -> primary.copy(alpha = 0.72f)
                            count == 2 -> primary.copy(alpha = 0.5f)
                            count == 1 -> primary.copy(alpha = 0.24f)
                            isFuture -> surfaceVariant.copy(alpha = 0.18f)
                            else -> surfaceVariant.copy(alpha = 0.4f)
                        }
                        drawRoundRect(
                            color = bg,
                            topLeft = Offset(x, y),
                            size = Size(cellPx, cellPx),
                            cornerRadius = radius,
                        )
                        if (date == today) {
                            drawRoundRect(
                                color = primary,
                                topLeft = Offset(x + strokeWidth / 2f, y + strokeWidth / 2f),
                                size = Size(cellPx - strokeWidth, cellPx - strokeWidth),
                                cornerRadius = radius,
                                style = Stroke(width = strokeWidth),
                            )
                        }
                        val textColor = when {
                            count >= 2 -> Color.White
                            count == 1 -> primary
                            else -> onSurfaceVariant
                        }
                        textPaint.color = textColor.toArgb()
                        textPaint.isFakeBoldText = count >= 2
                        val metrics = textPaint.fontMetrics
                        val baseline = y + cellPx / 2f - (metrics.ascent + metrics.descent) / 2f
                        drawContext.canvas.nativeCanvas.drawText(
                            dayOfMonth.toString(),
                            x + cellPx / 2f,
                            baseline,
                            textPaint,
                        )
                    }
                }
            }
        }
    }
}


/** 本周热力图：7 个竖条色块，颜色深浅表示次数，今天描边。 */
@Composable
private fun WeekHeatmap(
    start: LocalDate,
    counts: Map<LocalDate, Int>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (i in 0 until 7) {
            val date = start.plusDays(i.toLong())
            val count = counts[date] ?: 0
            val bg = when {
                count >= 4 -> primary.copy(alpha = 0.92f)
                count == 3 -> primary.copy(alpha = 0.72f)
                count == 2 -> primary.copy(alpha = 0.5f)
                count == 1 -> primary.copy(alpha = 0.24f)
                date.isAfter(today) -> surfaceVariant.copy(alpha = 0.18f)
                else -> surfaceVariant.copy(alpha = 0.4f)
            }
            val textColor = when {
                count >= 2 -> Color.White
                count == 1 -> primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = labels[i],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDayClick(date) }
                        .background(bg)
                        .then(
                            if (date == today) {
                                Modifier.border(1.5.dp, primary, RoundedCornerShape(10.dp))
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PeriodDistributionCard(periodCounts: List<Int>) {
    val total = periodCounts.sum()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "时段分布",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            PERIOD_LABELS.forEachIndexed { index, label ->
                val value = periodCounts.getOrElse(index) { 0 }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(76.dp),
                    )
                    LinearProgressIndicator(
                        progress = { if (total > 0) value.toFloat() / total else 0f },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        drawStopIndicator = {},
                    )
                    Text(
                        text = value.toString() + " / " + percentOf(value, total),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(66.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
internal fun TypeDistributionCard(types: List<String>, typeCounts: Map<String, Int>) {
    // 已删除但还有历史记录的类型也要显示，否则数据看起来对不上
    val extraTypes = typeCounts.keys.filter { it !in types }
    val items = types + extraTypes
    val total = typeCounts.values.sum()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "类型分布",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            if (typeCounts.isEmpty()) {
                Text(
                    text = "这个周期还没有冲了的记录",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                items.forEach { item ->
                    val value = typeCounts[item] ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(60.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        LinearProgressIndicator(
                            progress = { if (total > 0) value.toFloat() / total else 0f },
                            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            drawStopIndicator = {},
                        )
                        Text(
                            text = value.toString() + " / " + percentOf(value, total),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(66.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BarCard(
    report: PeriodReport,
    period: ReportPeriod,
    onBarClick: (Int) -> Unit = {},
) {
    val title = when (period) {
        ReportPeriod.ALL -> "历年次数"
        ReportPeriod.YEAR -> "每月次数"
        ReportPeriod.MONTH -> "每日次数"
        ReportPeriod.WEEK -> "本周每日"
    }
    val peak = report.barPeak
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                LegendDot(color = MaterialTheme.colorScheme.primary, label = "冲了")
                Spacer(Modifier.width(10.dp))
                LegendDot(color = MaterialTheme.colorScheme.tertiary, label = "忍住")
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(96.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                report.barLabels.indices.forEach { index ->
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f).clickable { onBarClick(index) },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        // 只有一两年时限制单根柱子的最大宽度，避免「历年次数」糊满整张卡
                        val barWidth = minOf(maxWidth * 0.62f, 48.dp)
                        StackedBar(
                            done = report.barDone.getOrElse(index) { 0 },
                            resist = report.barResist.getOrElse(index) { 0 },
                            peak = peak,
                            height = 96.dp,
                            doneColor = MaterialTheme.colorScheme.primary,
                            resistColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.75f),
                            modifier = Modifier.width(barWidth),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            BarAxisLabels(
                labels = report.barLabels,
                period = period,
                modifier = Modifier.fillMaxWidth().height(14.dp),
            )
        }
    }
}

/** 柱状图 x 轴标签：用 Canvas 精确居中，避免 31 个窄格子把两位数字裁掉。 */
@Composable
private fun BarAxisLabels(
    labels: List<String>,
    period: ReportPeriod,
    modifier: Modifier,
) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val textSizePx = with(LocalDensity.current) { 9.sp.toPx() }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    Canvas(modifier = modifier) {
        if (labels.isEmpty()) return@Canvas
        paint.color = color.toArgb()
        paint.textSize = textSizePx
        paint.textAlign = Paint.Align.CENTER
        val cell = size.width / labels.size
        val baseline = size.height / 2f - (paint.ascent() + paint.descent()) / 2f
        labels.forEachIndexed { index, label ->
            val show = when (period) {
                ReportPeriod.MONTH -> {
                    val day = index + 1
                    day == 1 || day % 5 == 0
                }
                else -> true
            }
            if (show) {
                drawContext.canvas.nativeCanvas.drawText(label, (index + 0.5f) * cell, baseline, paint)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color),
    )
    Spacer(Modifier.width(4.dp))
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
