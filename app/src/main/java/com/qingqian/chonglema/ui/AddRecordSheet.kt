package com.qingqian.chonglema.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingqian.chonglema.model.CheckInRecord
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

private fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.US, "%02d:%02d", hour, minute)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddRecordSheet(
    date: LocalDate,
    existing: List<CheckInRecord>,
    editMode: Boolean = false,
    dismissOnAdd: Boolean = false,
    // 从首页「记一次」进来时只需要表单，不显示当天已有记录和历史提示
    showExisting: Boolean = true,
    onAdd: (LocalTime, String, String) -> Unit,
    onUpdate: (Long, LocalTime, String, String) -> Unit,
    onDelete: (Long) -> Unit,
    onClearDay: () -> Unit,
    // 补记历史日期时，除了「冲了」也能补一条「忍住了」
    onResist: (LocalTime) -> Unit,
    types: List<String>,
    onAddType: (String) -> String?,
    onRemoveType: (String) -> String?,
    onMoveType: (Int, Int) -> Unit,
    onResetTypes: () -> Unit,
    onDismiss: () -> Unit,
) {
    val now = remember { LocalTime.now() }
    var hour by rememberSaveable { mutableIntStateOf(now.hour) }
    var minute by rememberSaveable { mutableIntStateOf(now.minute) }
    // 类型词条由用户自己维护（可在下方「管理」里增删）
    val fallbackType = types.firstOrNull() ?: "自慰"
    var type by rememberSaveable { mutableStateOf(fallbackType) }
    var showTypeEditor by remember { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var confirmClearDay by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CheckInRecord?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    // 「次数」只算真的冲了；忍住记录单独展示，只读（只能删除）
    val doneExisting = existing.filter { !it.isResist }
    val resistExisting = existing.filter { it.isResist }
    val editingRecord = existing.firstOrNull { it.id == editingId && !it.isResist }
    // 正在编辑的记录如果用了已经被删掉的词条，临时把它加回来，避免类型被悄悄改掉
    val selectableTypes = remember(types, editingRecord) {
        val current = editingRecord?.type
        if (current != null && current.isNotBlank() && current !in types) types + current else types
    }

    LaunchedEffect(types) {
        if (type !in selectableTypes) type = selectableTypes.firstOrNull() ?: fallbackType
    }

    fun startEdit(record: CheckInRecord) {
        editingId = record.id
        hour = record.time.substringBefore(":").toIntOrNull() ?: 0
        minute = record.time.substringAfter(":").toIntOrNull() ?: 0
        type = record.type.ifBlank { fallbackType }
        note = record.note
    }

    fun resetForm() {
        editingId = null
        hour = now.hour
        minute = now.minute
        type = fallbackType
        note = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (editMode) "编辑当日记录" else "记录打卡",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = dateLabel(date) + " · 当天已有 " + doneExisting.size + " 次" +
                            if (resistExisting.isNotEmpty()) " · 忍住 " + resistExisting.size + " 次" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (showExisting && existing.isNotEmpty()) {
                    TextButton(onClick = { confirmClearDay = true }) {
                        Text("清空当天", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (showExisting && existing.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                existing.forEach { record ->
                    if (record.isResist) {
                        // 忍住记录只读：不提供编辑，避免把结果改成「冲了」
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = record.time,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "忍住了",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = { pendingDelete = record },
                                    modifier = Modifier.size(30.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "删除这一条",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        val selected = record.id == editingId
                        Surface(
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { startEdit(record) },
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = record.time,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = record.type,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (record.note.isNotBlank()) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = record.note,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "编辑",
                                    modifier = Modifier.size(15.dp),
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                IconButton(
                                    onClick = { pendingDelete = record },
                                    modifier = Modifier.size(30.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "删除这一条",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "点一行可修改时间 / 类型 / 备注，右侧 ✕ 删除这一条；忍住记录只能删除",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }

            if (editingRecord != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "正在编辑 " + editingRecord.time + " 的记录",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { resetForm() }) { Text("取消编辑") }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "时间",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatTime(hour, minute),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "类型",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showTypeEditor = true }) { Text("管理词条") }
            }
            Spacer(Modifier.height(2.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selectableTypes.forEach { item ->
                    FilterChip(
                        selected = type == item,
                        onClick = { type = item },
                        label = { Text(item) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（选填）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val time = LocalTime.of(hour, minute)
                    if (editingRecord != null) {
                        onUpdate(editingRecord.id, time, type, note)
                        resetForm()
                    } else {
                        onAdd(time, type, note)
                        if (dismissOnAdd) {
                            onDismiss()
                        } else {
                            resetForm()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = if (editingRecord != null) "保存修改" else "打卡 +1",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            // 补记过去的日子：除了「冲了」，也能补「忍住了」；编辑旧记录时同样可以补
            if (date.isBefore(LocalDate.now())) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onResist(LocalTime.of(hour, minute))
                        if (dismissOnAdd) onDismiss() else resetForm()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = "补记一次「忍住了」",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }

    if (showTypeEditor) {
        TypeManagerDialog(
            types = types,
            onAdd = { name ->
                val message = onAddType(name)
                if (message == null) type = name.trim()
                message
            },
            onRemove = onRemoveType,
            onMove = onMoveType,
            onReset = onResetTypes,
            onDismiss = { showTypeEditor = false },
        )
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条记录？") },
            text = {
                Text(
                    dateLabel(date) + " " + record.time + " · " + (if (record.isResist) "忍住了" else record.type) +
                        "，删除后无法恢复。",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editingId == record.id) resetForm()
                    onDelete(record.id)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            hour = hour,
            minute = minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { newHour, newMinute ->
                hour = newHour
                minute = newMinute
                showTimePicker = false
            },
        )
    }

    if (confirmClearDay) {
        AlertDialog(
            onDismissRequest = { confirmClearDay = false },
            title = { Text("清空 " + dateLabel(date) + " 的全部记录？") },
            text = { Text("这一天的 " + existing.size + " 条记录会被删除，无法恢复。只删这一条日期的话，用每行右侧的 ✕。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearDay = false
                    resetForm()
                    onClearDay()
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearDay = false }) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}
