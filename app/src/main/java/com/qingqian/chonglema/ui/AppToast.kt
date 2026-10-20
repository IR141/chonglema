package com.qingqian.chonglema.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 轻提示状态：只保留「最新一条」，新消息会直接顶掉旧的并重新计时，
 * 不会像 Snackbar 那样排队导致连续操作时慢半拍。
 */
@Stable
class AppToastState {
    var message by mutableStateOf<String?>(null)
        private set
    private var job: Job? = null

    fun show(scope: CoroutineScope, text: String, durationMs: Long = 1900L) {
        job?.cancel()
        message = text
        job = scope.launch {
            delay(durationMs)
            message = null
        }
    }
}

@Composable
fun rememberAppToastState(): AppToastState = remember { AppToastState() }

/** 圆角胶囊提示：主色浅底 + 圆形勾选徽章，带轻微上滑/淡入动画。 */
@Composable
fun AppToastHost(state: AppToastState, modifier: Modifier = Modifier) {
    var lastMessage by remember { mutableStateOf("") }
    state.message?.let { lastMessage = it }
    AnimatedVisibility(
        visible = state.message != null,
        enter = fadeIn(tween(140)) + slideInVertically(tween(220)) { it / 3 },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 4 },
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 8.dp,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 14.dp),
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 20.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
