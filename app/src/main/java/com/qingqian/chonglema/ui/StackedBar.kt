package com.qingqian.chonglema.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 一根柱：下半部分「冲了」，上半部分「忍住」。
 * 用 Canvas 一次画完，接缝处紧贴，不会出现两段颜色之间的白缝。
 */
@Composable
fun StackedBar(
    done: Int,
    resist: Int,
    peak: Int,
    height: Dp,
    doneColor: Color,
    resistColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val total = done + resist
        val safePeak = peak.coerceAtLeast(1)
        if (total <= 0) {
            drawRoundRect(
                color = Color(0x22888888),
                topLeft = Offset(0f, size.height - 2f),
                size = Size(size.width, 2f),
                cornerRadius = CornerRadius(1f, 1f),
            )
            return@Canvas
        }
        val totalHeight = size.height * (total.toFloat() / safePeak)
        val doneHeight = size.height * (done.toFloat() / safePeak)
        val radius = 3.dp.toPx()
        if (done > 0 && resist > 0) {
            val top = size.height - totalHeight
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(0f, top, size.width, size.height),
                        topLeft = CornerRadius(radius, radius),
                        topRight = CornerRadius(radius, radius),
                        bottomRight = CornerRadius.Zero,
                        bottomLeft = CornerRadius.Zero,
                    )
                )
            }
            drawPath(path, resistColor)
            drawRect(
                color = doneColor,
                topLeft = Offset(0f, size.height - doneHeight),
                size = Size(size.width, doneHeight),
            )
        } else {
            val color = if (done > 0) doneColor else resistColor
            val top = size.height - totalHeight
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(0f, top, size.width, size.height),
                        topLeft = CornerRadius(radius, radius),
                        topRight = CornerRadius(radius, radius),
                        bottomRight = CornerRadius.Zero,
                        bottomLeft = CornerRadius.Zero,
                    )
                )
            }
            drawPath(path, color)
        }
    }
}
