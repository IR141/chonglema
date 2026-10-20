package com.qingqian.chonglema

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.qingqian.chonglema.data.ThemeMode
import com.qingqian.chonglema.ui.App
import com.qingqian.chonglema.ui.MainViewModel
import com.qingqian.chonglema.ui.theme.ChonglemaTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 闪屏一直挂到数据库首次读完，避免先闪一下空列表
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !vm.isLoaded.value }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by vm.themeMode.collectAsState()
            val goal by vm.goal.collectAsState()
            val types by vm.types.collectAsState()
            val challenge by vm.challenge.collectAsState()
            val launchTitle by vm.launchTitle.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            ChonglemaTheme(darkTheme = dark) {
                App(
                    store = vm.store,
                    themeMode = themeMode,
                    systemDark = systemDark,
                    onThemeModeChange = vm::setThemeMode,
                    goal = goal,
                    onGoalChange = vm::setGoal,
                    types = types,
                    onAddType = vm::addType,
                    onRemoveType = vm::removeType,
                    onMoveType = vm::moveType,
                    onResetTypes = vm::resetTypes,
                    challenge = challenge,
                    onStartChallenge = vm::startChallenge,
                    onCancelChallenge = vm::cancelChallenge,
                    onImportRecords = vm::importRecords,
                    launchTitle = launchTitle,
                )
            }
        }
    }
}
