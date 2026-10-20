package com.qingqian.chonglema.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingqian.chonglema.model.CheckInRecord
import com.qingqian.chonglema.stats.PERIOD_LABELS
import com.qingqian.chonglema.stats.WEEKDAY_LABELS
import com.qingqian.chonglema.stats.AnnualReport
import com.qingqian.chonglema.stats.annualHighlights
import com.qingqian.chonglema.stats.computeAnnualReport
import java.time.LocalDate

/** 年度报告页。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnualReportScreen(records: List<CheckInRecord>, types: List<String>, onBack: () -> Unit) {
    val context = LocalContext.current
    var year by remember { mutableIntStateOf(LocalDate.now().year) }
    val report = remember(records, year) { computeAnnualReport(records, year) }
    val highlights = remember(report) { annualHighlights(report) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("年度报告", fontWeight = FontWeight.Bold) },
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
            YearSwitcher(
                year = year,
                onPrevious = { year -= 1 },
                onNext = { year += 1 },
            )
            Spacer(Modifier.height(14.dp))
            HeroCard(report)
            Spacer(Modifier.height(14.dp))
            MonthlyCard(report)
            Spacer(Modifier.height(14.dp))
            WeekdayCard(report)
            Spacer(Modifier.height(14.dp))
            PeriodCard(report)
            Spacer(Modifier.height(14.dp))
            TypeCard(report = report, types = types)
            Spacer(Modifier.height(14.dp))
            HighlightsCard(highlights)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val text = buildSummary(report, highlights)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("chonglema annual report", text))
                    Toast.makeText(context, "已复制 $year 年度总结", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("复制文字总结")
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun buildSummary(report: AnnualReport, highlights: List<String>): String {
    val builder = StringBuilder()
    builder.append("冲了吗 · ").append(report.year).append(" 年度报告\n")
    builder.append("全年 ").append(report.totalCount).append(" 次 · 打卡 ")
        .append(report.activeDays).append(" 天 · 忍住 ")
        .append(report.resistCount).append(" 次\n")
    highlights.forEach { line -> builder.append("· ").append(line).append("\n") }
    return builder.toString()
}

@Composable
private fun YearSwitcher(year: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "上一年")
        }
        Text(
            text = year.toString() + " 年",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "下一年")
        }
    }
}

@Composable
private fun HeroCard(report: AnnualReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = report.year.toString() + " 年，全年一共",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = report.totalCount.toString(),
                    fontSize = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = " 次",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                HeroSmallValue("打卡天数", report.activeDays.toString() + " 天")
                HeroSmallValue("忍住", report.resistCount.toString() + " 次")
            }
        }
    }
}

@Composable
private fun HeroSmallValue(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MonthlyCard(report: AnnualReport) {
    val peak = (1..12)
        .maxOfOrNull { report.monthlyCounts[it - 1] + report.monthlyResist[it - 1] }
        ?.coerceAtLeast(1) ?: 1
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("每月次数", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                repeat(12) { index ->
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            StackedBar(
                                done = report.monthlyCounts[index],
                                resist = report.monthlyResist[index],
                                peak = peak,
                                height = 86.dp,
                                doneColor = MaterialTheme.colorScheme.primary,
                                resistColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.fillMaxWidth(0.7f),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = (index + 1).toString(),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendDot("冲了", MaterialTheme.colorScheme.primary)
                LegendDot("忍住", MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun WeekdayCard(report: AnnualReport) {
    val total = report.totalCount
    val barArea = 72.dp
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("星期分布", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            // 只放柱子：固定高度 + 底部对齐，保证所有柱子从同一条基线开始
            Row(
                modifier = Modifier.fillMaxWidth().height(barArea),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                report.weekdayCounts.forEach { count ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((barArea.value * count / total.coerceAtLeast(1)).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                report.weekdayCounts.forEachIndexed { index, count ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = WEEKDAY_LABELS[index].removePrefix("周"),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = percentOf(count, total),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodCard(report: AnnualReport) {
    val total = report.totalCount
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("时段分布", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            report.periodCounts.forEachIndexed { index, count ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = PERIOD_LABELS[index],
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(76.dp),
                    )
                    LinearProgressIndicator(
                        progress = { if (total > 0) count.toFloat() / total else 0f },
                        modifier = Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        drawStopIndicator = {},
                    )
                    Text(
                        text = count.toString() + " / " + percentOf(count, total),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(66.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeCard(report: AnnualReport, types: List<String>) {
    if (report.typeCounts.isEmpty()) return
    val total = report.typeCounts.values.sum()
    // 自定义类型按用户维护的顺序显示，已删除但有历史数据的类型排在后面
    val items = types + report.typeCounts.keys.filter { it !in types }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("类型分布", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            items.forEach { type ->
                val count = report.typeCounts[type] ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = type,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(64.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    LinearProgressIndicator(
                        progress = { if (total > 0) count.toFloat() / total else 0f },
                        modifier = Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        drawStopIndicator = {},
                    )
                    Text(
                        text = count.toString() + " / " + percentOf(count, total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(66.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightsCard(highlights: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("这一年", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            highlights.forEach { line ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(
                        text = "· ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(text = line, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}