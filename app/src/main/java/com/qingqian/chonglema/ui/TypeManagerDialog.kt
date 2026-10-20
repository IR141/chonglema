package com.qingqian.chonglema.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

private val ROW_HEIGHT = 44.dp

/**
 * 管理「记录打卡」里的类型词条：可以添加新词条、删除已有词条、长按拖动排序。
 * 删除词条只影响以后的选择，不会删掉已经记录的数据。
 *
 * 拖动排序：手指还没松开时被拖的词条跟着手指走，其它词条平滑让位；
 * 拖动过程中不重建列表，松手后才把「起始位置 → 目标位置」一次性写进存储，
 * 这样不会出现拖动残影。
 */
@Composable
fun TypeManagerDialog(
    types: List<String>,
    onAdd: (String) -> String?,
    onRemove: (String) -> String?,
    onMove: (Int, Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    var dragFrom by remember { mutableIntStateOf(-1) }
    var dragTo by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { ROW_HEIGHT.toPx() }

    /** 开始拖动：记住起始槽位，拖动中不改列表顺序。 */
    fun startDrag(index: Int) {
        if (index < 0) return
        dragFrom = index
        dragTo = index
        dragOffset = 0f
    }

    /** 拖动中：只累加位移并算出目标槽位，位移不会无限累加到列表外面。 */
    fun dragBy(dy: Float) {
        if (dragFrom < 0) return
        val last = types.lastIndex
        if (last <= 0) return
        dragOffset = (dragOffset + dy).coerceIn(
            -dragFrom * rowHeightPx,
            (last - dragFrom) * rowHeightPx,
        )
        dragTo = (dragFrom + (dragOffset / rowHeightPx).roundToInt()).coerceIn(0, last)
    }

    fun resetDrag() {
        dragFrom = -1
        dragTo = -1
        dragOffset = 0f
    }

    /** 松手：把「起始槽位 → 目标槽位」一次性交给 TypeStore。 */
    fun endDrag() {
        val from = dragFrom
        val to = dragTo
        resetDrag()
        if (from >= 0 && to >= 0 && from != to) onMove(from, to)
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("恢复默认词条？") },
            text = { Text("自定义的词条会被清掉，恢复成「自慰 · 梦遗 · 性行为 · 其它」，已经记录的数据不受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    onReset()
                    confirmReset = false
                    input = ""
                    error = null
                }) { Text("恢复") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("打卡类型") },
        text = {
            Column {
                Text(
                    text = "这些词条会出现在记录打卡的「类型」里。删掉词条只是以后不再显示，已经记录的数据不会丢。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "长按词条可以拖动排序，其它词条会自动让位。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.heightIn(min = 8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    types.forEachIndexed { index, item ->
                        val isDragging = index == dragFrom
                        // 被拖到别的槽位时，中间的词条朝反方向让出一行
                        val shiftSlots = when {
                            dragFrom < 0 || isDragging -> 0
                            dragTo > dragFrom && index in (dragFrom + 1)..dragTo -> -1
                            dragTo < dragFrom && index in dragTo until dragFrom -> 1
                            else -> 0
                        }
                        val shiftPx by animateFloatAsState(
                            targetValue = shiftSlots * rowHeightPx,
                            label = "typeRowShift",
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ROW_HEIGHT)
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffset else shiftPx
                                }
                                .then(
                                    if (isDragging) {
                                        Modifier
                                            .shadow(6.dp, RoundedCornerShape(10.dp))
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                shape = RoundedCornerShape(10.dp),
                                            )
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(vertical = 1.dp)
                                .pointerInput(item) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { startDrag(types.indexOf(item)) },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragBy(amount.y)
                                        },
                                        onDragEnd = { endDrag() },
                                        onDragCancel = { resetDrag() },
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = "拖动手柄",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    val message = onRemove(item)
                                    error = message
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "删除词条 " + item,
                                    modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.heightIn(min = 6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            error = null
                        },
                        singleLine = true,
                        placeholder = { Text("请输入新词条") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val message = onAdd(input)
                            if (message == null) {
                                input = ""
                                error = null
                            } else {
                                error = message
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("添加")
                    }
                }
                error?.let {
                    Spacer(Modifier.heightIn(min = 4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
        dismissButton = {
            TextButton(onClick = { confirmReset = true }) { Text("恢复默认") }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
