package com.qingqian.chonglema.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
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
import com.qingqian.chonglema.stats.ChallengeState
import com.qingqian.chonglema.stats.computeStats

private const val SCREEN_HOME = "home"
private const val SCREEN_REPORT = "report"
private const val SCREEN_SETTINGS = "settings"
private const val SCREEN_GOALS = "goals"
private const val SCREEN_CHALLENGE = "challenge"
private const val SCREEN_ANNUAL = "annual"
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
    challenge: ChallengeState?,
    onStartChallenge: (Int) -> Unit,
    onCancelChallenge: () -> Unit,
    onImportRecords: (List<CheckInRecord>) -> ImportResult,
    launchTitle: String,
) {
    val records by store.records.collectAsState()
    var screen by rememberSaveable { mutableStateOf(SCREEN_HOME) }
    // 两级返回：目标页 / 详情页分别记住是从哪里进来的
    var goalsParent by rememberSaveable { mutableStateOf(SCREEN_HOME) }
    var detailParent by rememberSaveable { mutableStateOf(SCREEN_GOALS) }
    val today = rememberToday()
    // 统计只算一次，首页 / 报表 / 挑战页共用；records 或日期变化时才重算
    val stats = remember(records, today) { computeStats(records, today) }

    BackHandler(enabled = screen != SCREEN_HOME) {
        screen = when (screen) {
            SCREEN_GOALS -> goalsParent
            SCREEN_CHALLENGE, SCREEN_ANNUAL -> detailParent
            else -> SCREEN_HOME
        }
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

            SCREEN_GOALS -> GoalsScreen(
                records = records,
                today = today,
                goal = goal,
                onGoalChange = onGoalChange,
                challenge = challenge,
                onOpenChallenge = {
                    detailParent = SCREEN_GOALS
                    screen = SCREEN_CHALLENGE
                },
                onOpenAnnual = {
                    detailParent = SCREEN_GOALS
                    screen = SCREEN_ANNUAL
                },
                onBack = { screen = goalsParent },
            )

            SCREEN_CHALLENGE -> ChallengeScreen(
                records = records,
                today = today,
                stats = stats,
                challenge = challenge,
                onStartChallenge = onStartChallenge,
                onCancelChallenge = onCancelChallenge,
                onBack = { screen = detailParent },
            )

            SCREEN_ANNUAL -> AnnualReportScreen(
                records = records,
                types = types,
                onBack = { screen = detailParent },
            )

            else -> HomeScreen(
                records = records,
                today = today,
                stats = stats,
                goal = goal,
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
                onOpenGoals = {
                    goalsParent = SCREEN_HOME
                    screen = SCREEN_GOALS
                },
                onOpenChallenge = {
                    detailParent = SCREEN_HOME
                    screen = SCREEN_CHALLENGE
                },
                onOpenAnnual = {
                    detailParent = SCREEN_HOME
                    screen = SCREEN_ANNUAL
                },
            )
        }
    }
}
