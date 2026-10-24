package com.qingqian.chonglema.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.qingqian.chonglema.data.CheckInStore
import com.qingqian.chonglema.data.GoalConfig
import com.qingqian.chonglema.data.ImportResult
import com.qingqian.chonglema.data.ThemeMode
import com.qingqian.chonglema.model.CheckInRecord
import com.qingqian.chonglema.stats.ChallengeAttempt
import com.qingqian.chonglema.stats.challengeKey
import com.qingqian.chonglema.stats.computeChallengeProgress
import com.qingqian.chonglema.stats.computeStats

private const val SCREEN_HOME = "home"
private const val SCREEN_REPORT = "report"
private const val SCREEN_SETTINGS = "settings"
private const val SCREEN_AUTONOMY = "autonomy"
private const val SCREEN_BADGES = "badges"
private const val SCREEN_SEARCH = "search"

@Composable
fun App(
    store: CheckInStore,
    themeMode: ThemeMode,
    systemDark: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    goal: GoalConfig,
    onGoalChange: (GoalConfig) -> Unit,
    types: List<String>,
    onAddType: (String) -> String?,
    onRemoveType: (String) -> String?,
    onMoveType: (Int, Int) -> Unit,
    onResetTypes: () -> Unit,
    attempts: List<ChallengeAttempt>,
    celebratedKeys: Set<String>,
    homeChallengeHidden: Boolean,
    dataReady: Boolean,
    onStartChallenge: (Int) -> Unit,
    onCancelChallenge: () -> Unit,
    onDeleteChallenge: (ChallengeAttempt) -> Unit,
    onMarkChallengeCelebrated: (ChallengeAttempt) -> Unit,
    onSetHomeChallengeHidden: (Boolean) -> Unit,
    onImportRecords: (List<CheckInRecord>) -> ImportResult,
    launchTitle: String,
) {
    val records by store.records.collectAsState()
    var screen by rememberSaveable { mutableStateOf(SCREEN_HOME) }
    val today = rememberToday()
    // 统计只算一次，首页 / 报表 / 勋章页共用；records 或日期变化时才重算
    val stats = remember(records, today) { computeStats(records, today) }
    // 挑战状态永远由记录实时算：删掉「冲了」记录，挑战会自动复活。
    // 已放弃的挑战只用于「挑战记录」，不参与当前挑战 / 勋章计算。
    val allChallengeProgresses = remember(records, attempts, today) {
        attempts.map { computeChallengeProgress(records, it, today) }
    }
    val challengeProgresses = remember(allChallengeProgresses) {
        allChallengeProgresses.filter { !it.attempt.cancelled }
    }
    val currentChallenge = challengeProgresses.lastOrNull()
    // 「挑战记录」只放已经结束的挑战：失败 / 成功 / 主动放弃。
    // 进行中的挑战（包括历史遗留的旧进行中尝试）只在顶部卡片展示，不在记录里重复。
    val challengeHistory = remember(allChallengeProgresses) {
        allChallengeProgresses
            .filter { it.attempt.cancelled || it.failed || it.completed }
            .asReversed()
            .take(10)
    }
    // 冷启动时只要没有「进行中」的挑战（没有 / 已失败 / 已成功），首页大卡右上角的挑战信息就不显示
    var launchChallengeChecked by remember { mutableStateOf(false) }
    LaunchedEffect(dataReady) {
        if (dataReady && !launchChallengeChecked) {
            launchChallengeChecked = true
            val active = currentChallenge != null && !currentChallenge.failed && !currentChallenge.completed
            onSetHomeChallengeHidden(!active)
        }
    }

    BackHandler(enabled = screen != SCREEN_HOME) {
        screen = SCREEN_HOME
    }

    Crossfade(targetState = screen) { current ->
        when (current) {
            SCREEN_SEARCH -> SearchScreen(
                records = records,
                today = today,
                types = types,
                onAddRecord = store::addRecord,
                onUpdateRecord = store::updateRecord,
                onDeleteRecord = store::deleteRecord,
                onClearDate = store::clearDate,
                onResist = store::addResist,
                onBack = { screen = SCREEN_HOME },
            )

            SCREEN_REPORT -> ReportScreen(
                records = records,
                types = types,
                today = today,
                onBack = { screen = SCREEN_HOME },
            )

            SCREEN_SETTINGS -> SettingsScreen(
                records = records,
                themeMode = themeMode,
                systemDark = systemDark,
                onThemeModeChange = onThemeModeChange,
                types = types,
                onAddType = onAddType,
                onRemoveType = onRemoveType,
                onMoveType = onMoveType,
                onResetTypes = onResetTypes,
                onImportRecords = onImportRecords,
                onClearAll = { store.clearAll() },
                onBack = { screen = SCREEN_HOME },
            )

            SCREEN_AUTONOMY -> ChallengeScreen(
                records = records,
                today = today,
                goal = goal,
                onGoalChange = onGoalChange,
                progresses = challengeProgresses,
                history = challengeHistory,
                onDeleteHistory = onDeleteChallenge,
                isCelebrated = { attempt -> challengeKey(attempt) in celebratedKeys },
                onStartChallenge = onStartChallenge,
                onCancelChallenge = onCancelChallenge,
                onCelebrate = onMarkChallengeCelebrated,
                onBack = { screen = SCREEN_HOME },
            )

            SCREEN_BADGES -> BadgesScreen(
                stats = stats,
                progresses = challengeProgresses,
                onBack = { screen = SCREEN_HOME },
            )

            else -> HomeScreen(
                records = records,
                today = today,
                stats = stats,
                goal = goal,
                challenge = if (homeChallengeHidden) null else currentChallenge,
                challengeHidden = homeChallengeHidden,
                types = types,
                onAddType = onAddType,
                onRemoveType = onRemoveType,
                onMoveType = onMoveType,
                onResetTypes = onResetTypes,
                launchTitle = launchTitle,
                onAddRecord = store::addRecord,
                onUpdateRecord = store::updateRecord,
                onDeleteRecord = store::deleteRecord,
                onClearDate = store::clearDate,
                onResist = store::addResist,
                onOpenReport = { screen = SCREEN_REPORT },
                onOpenSearch = { screen = SCREEN_SEARCH },
                onOpenSettings = { screen = SCREEN_SETTINGS },
                onOpenAutonomy = { screen = SCREEN_AUTONOMY },
                onOpenBadges = { screen = SCREEN_BADGES },
            )
        }
    }
}
