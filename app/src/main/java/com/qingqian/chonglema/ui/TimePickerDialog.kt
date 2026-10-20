package com.qingqian.chonglema.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.Locale

/**
 * 无表盘的时间选择：只保留「时 : 分」两个大数字卡片，用卡片上下箭头（长按连发）调整。
 * 这样比 Material 表盘更干净，也不会因为表盘太小点错。
 */
@Composable
fun TimePickerDialog(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var hourValue by remember { mutableIntStateOf(hour) }
    var minuteValue by remember { mutableIntStateOf(minute) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "选择时间",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "点按箭头调整，长按可以快速加减",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(18.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumberCard(
                        value = hourValue,
                        unit = "时",
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                        onUp = { hourValue = (hourValue + 1) % 24 },
                        onDown = { hourValue = (hourValue + 23) % 24 },
                    )
                    Text(
                        text = ":",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .offset(y = (-8).dp),
                    )
                    NumberCard(
                        value = minuteValue,
                        unit = "分",
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                        onUp = { minuteValue = (minuteValue + 1) % 60 },
                        onDown = { minuteValue = (minuteValue + 59) % 60 },
                    )
                }

                Spacer(Modifier.height(10.dp))
                TextButton(onClick = {
                    val now = LocalTime.now()
                    hourValue = now.hour
                    minuteValue = now.minute
                }) {
                    Text("设为现在", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { onConfirm(hourValue, minuteValue) }) {
                        Text(
                            text = "确定",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberCard(
    value: Int,
    unit: String,
    container: Color,
    content: Color,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(114.dp)
            .background(container, RoundedCornerShape(24.dp))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ArrowButton(content = content, up = true, onStep = onUp)
        Text(
            text = String.format(Locale.US, "%02d", value),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = content,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = unit,
            fontSize = 10.sp,
            color = content.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        ArrowButton(content = content, up = false, onStep = onDown)
    }
}

/** 箭头按钮：短按走一步；按住 320ms 后开始每 60ms 连发，抬手指立刻停止。 */
@Composable
private fun ArrowButton(
    content: Color,
    up: Boolean,
    onStep: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }

    // 按住 320ms 后每 60ms 连发一步；松手（pressed=false）时这个协程会被自动取消。
    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        delay(320)
        while (true) {
            onStep()
            delay(60)
        }
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onStep()
                        pressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (up) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (up) "加一" else "减一",
            tint = content.copy(alpha = 0.85f),
            modifier = Modifier.size(26.dp),
        )
    }
}