package com.qingqian.chonglema.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 记住「今天」，并在跨过 0 点时自动更新。
 *
 * 之前每个页面都写 remember { LocalDate.now() }：App 一直开着过了 0 点，
 * 「今日次数 / 上一发 / 连续打卡」就会一直用昨天的日期算。统一从这里取。
 */
@Composable
fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val waitMs = Duration.between(now, nextMidnight).toMillis() + 1_000L
            delay(waitMs.coerceAtLeast(1_000L))
            today = LocalDate.now()
        }
    }
    return today
}
