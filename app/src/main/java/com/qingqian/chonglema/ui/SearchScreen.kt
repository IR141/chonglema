package com.qingqian.chonglema.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qingqian.chonglema.model.CheckInRecord
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** 搜索范围：全部 / 今年 / 本月。 */
private enum class SearchScope(val label: String) { ALL("全部时间"), YEAR("今年"), MONTH("本月") }

/** 结果：冲了 / 忍住 / 全部。 */
private enum class SearchOutcome(val label: String) { ALL("全部"), DONE("冲了"), RESIST("忍住") }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    records: List<CheckInRecord>,
    today: LocalDate,
    types: List<String>,
    onAddRecord: (LocalDate, LocalTime, String, String) -> Unit,
    onUpdateRecord: (Long, LocalDate, LocalTime, String, String) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onClearDate: (LocalDate) -> Unit,
    onResist: (LocalDate, LocalTime) -> Unit,
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var scope by rememberSaveable { mutableStateOf(SearchScope.ALL.name) }
    var outcome by rememberSaveable { mutableStateOf(SearchOutcome.ALL.name) }
    var typeFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetDate by remember { mutableStateOf<LocalDate?>(null) }

    val results = remember(records, query, scope, outcome, typeFilter, today) {
        val q = query.trim()
        records.filter { record ->
            val date = LocalDate.parse(record.date)
            val scopeOk = when (scope) {
                SearchScope.YEAR.name -> date.year == today.year
                SearchScope.MONTH.name -> YearMonth.from(date) == YearMonth.from(today)
                else -> true
            }
            val outcomeOk = when (outcome) {
                SearchOutcome.DONE.name -> !record.isResist
                SearchOutcome.RESIST.name -> record.isResist
                else -> true
            }
            val typeOk = typeFilter == null || (!record.isResist && record.type == typeFilter)
            val textOk = q.isEmpty() || buildList {
                add(dateLabel(date))
                add(record.time)
                add(record.type)
                add(record.note)
                if (record.isResist) add("忍住了")
            }.any { it.contains(q, ignoreCase = true) }
            scopeOk && outcomeOk && typeOk && textOk
        }.sortedByDescending { it.timestamp }
    }
    val doneCount = results.count { !it.isResist }
    val resistCount = results.count { it.isResist }
    // 按日期分组，列表里同一天的记录聚在标题下面
    val grouped = remember(results) {
        results.groupBy { it.date }.toSortedMap(compareByDescending { it })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("搜索记录", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 18.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜备注、类型、日期或时间") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "清空")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchScope.entries.forEach { option ->
                    FilterChip(
                        selected = scope == option.name,
                        onClick = { scope = option.name },
                        label = { Text(option.label) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchOutcome.entries.forEach { option ->
                    FilterChip(
                        selected = outcome == option.name,
                        onClick = { outcome = option.name },
                        label = { Text(option.label) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = typeFilter == null,
                    onClick = { typeFilter = null },
                    label = { Text("全部类型") },
                )
                types.forEach { name ->
                    FilterChip(
                        selected = typeFilter == name,
                        onClick = { typeFilter = if (typeFilter == name) null else name },
                        label = { Text(name) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "共 " + results.size + " 条 · 冲了 " + doneCount + " 次 · 忍住 " + resistCount + " 次",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            if (results.isEmpty()) {
                Text(
                    text = if (query.isBlank()) "这个筛选下还没有记录" else "没有找到包含「" + query + "」的记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 30.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    grouped.forEach { (dateText, dayRecords) ->
                        val date = LocalDate.parse(dateText)
                        item(key = "h-" + dateText) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = dateLabel(date),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = dayRecords.count { it.isResist }.let { resists ->
                                        val done = dayRecords.size - resists
                                        if (resists > 0) done.toString() + " 冲 · " + resists + " 忍" else done.toString() + " 次"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(dayRecords, key = { it.id.toString() }) { record ->
                            SearchRow(record = record, onClick = { sheetDate = date })
                        }
                        item(key = "s-" + dateText) { Spacer(Modifier.height(6.dp)) }
                    }
                }
            }
        }
    }

    sheetDate?.let { date ->
        val existing = records.filter { it.date == date.toString() }
        AddRecordSheet(
            date = date,
            existing = existing,
            editMode = true,
            dismissOnAdd = false,
            showExisting = true,
            types = types,
            onAddType = { null },
            onRemoveType = { null },
            onMoveType = { _, _ -> },
            onResetTypes = {},
            onAdd = { time, type, note -> onAddRecord(date, time, type, note) },
            onUpdate = { id, time, newType, newNote -> onUpdateRecord(id, date, time, newType, newNote) },
            onDelete = onDeleteRecord,
            onClearDay = { onClearDate(date) },
            onResist = { time -> onResist(date, time) },
            onDismiss = { sheetDate = null },
        )
    }
}

@Composable
private fun SearchRow(record: CheckInRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = record.time,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (record.isResist) "忍住了" else record.type,
                style = MaterialTheme.typography.labelMedium,
                color = if (record.isResist) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            if (record.note.isNotBlank()) {
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
