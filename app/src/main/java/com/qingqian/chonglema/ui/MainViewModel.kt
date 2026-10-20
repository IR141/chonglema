package com.qingqian.chonglema.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qingqian.chonglema.data.BackupManager
import com.qingqian.chonglema.data.ChallengeStore
import com.qingqian.chonglema.data.CheckInStore
import com.qingqian.chonglema.data.GoalConfig
import com.qingqian.chonglema.data.ImportResult
import com.qingqian.chonglema.data.SettingsStore
import com.qingqian.chonglema.data.ThemeMode
import com.qingqian.chonglema.data.ThemeStore
import com.qingqian.chonglema.data.TypeStore
import com.qingqian.chonglema.model.CheckInRecord
import com.qingqian.chonglema.stats.ChallengeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

/**
 * 全局状态与动作都收在这里，Activity 只负责主题和闪屏。
 * 这样配置变化 / 进程回收后，数据状态不再依赖 Compose 的 remember。
 *
 * 启动优化：主题要参与首帧配色，同步读取；其余设置（自律目标、打卡类型、挑战、
 * 启动标题）与首帧无关，放到首次组合之后在 IO 线程读，读不到就用默认值。
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app.applicationContext

    val store = CheckInStore(app)
    private val themeStore = ThemeStore(app)

    private val _goal = MutableStateFlow(GoalConfig())
    private val _types = MutableStateFlow(TypeStore.DEFAULT_TYPES)
    private val _challenge = MutableStateFlow<ChallengeState?>(null)
    private val _launchTitle = MutableStateFlow(SettingsStore.TITLE_SENTENCES.first())

    val records = store.records
    val isLoaded = store.loaded
    val themeMode = themeStore.mode
    val goal: StateFlow<GoalConfig> = _goal.asStateFlow()
    val types: StateFlow<List<String>> = _types.asStateFlow()
    val challenge: StateFlow<ChallengeState?> = _challenge.asStateFlow()

    /** 冷启动抽一句标题，进程存活期间不变（读到之前先用第一句）。 */
    val launchTitle: StateFlow<String> = _launchTitle.asStateFlow()

    private var settingsStore: SettingsStore? = null
    private var typeStore: TypeStore? = null
    private var challengeStore: ChallengeStore? = null

    init {
        viewModelScope.launch {
            val setting = withContext(Dispatchers.IO) { SettingsStore(appContext) }
            val type = withContext(Dispatchers.IO) { TypeStore(appContext) }
            val challenge = withContext(Dispatchers.IO) { ChallengeStore(appContext) }
            val title = withContext(Dispatchers.IO) { setting.nextLaunchTitle() }
            settingsStore = setting
            typeStore = type
            challengeStore = challenge
            _goal.value = setting.goal.value
            _types.value = type.types.value
            _challenge.value = challenge.state.value
            _launchTitle.value = title
        }
    }

    /** 首帧之后才被点到的动作，极端情况下（启动瞬间点按）兜底同步建一次。 */
    private fun settings(): SettingsStore =
        settingsStore ?: SettingsStore(appContext).also { settingsStore = it }

    private fun types(): TypeStore = typeStore ?: TypeStore(appContext).also { typeStore = it }

    private fun challenges(): ChallengeStore =
        challengeStore ?: ChallengeStore(appContext).also { challengeStore = it }

    fun setThemeMode(mode: ThemeMode) = themeStore.setMode(mode)

    fun setGoal(config: GoalConfig) {
        val target = settings()
        target.setGoal(config)
        _goal.value = target.goal.value
    }

    fun addType(name: String): String? = types().let { store ->
        store.add(name).also { _types.value = store.types.value }
    }

    fun removeType(name: String): String? = types().let { store ->
        store.remove(name).also { _types.value = store.types.value }
    }

    fun moveType(from: Int, to: Int) = types().let { store ->
        store.move(from, to)
        _types.value = store.types.value
    }

    fun resetTypes() = types().let { store ->
        store.reset()
        _types.value = store.types.value
    }

    fun addRecord(date: LocalDate, time: LocalTime, type: String, note: String) =
        store.addRecord(date, time, type, note)

    fun updateRecord(id: Long, date: LocalDate, time: LocalTime, type: String, note: String) =
        store.updateRecord(id, date, time, type, note)

    fun deleteRecord(id: Long) = store.deleteRecord(id)
    fun clearDate(date: LocalDate) = store.clearDate(date)
    fun addResist(date: LocalDate, time: LocalTime) = store.addResist(date, time)
    fun clearAll() = store.clearAll()

    /** 导入：先按现有记录去重，再写库。 */
    fun importRecords(imported: List<CheckInRecord>): ImportResult {
        val (fresh, skipped) = BackupManager.dedupe(store.records.value, imported)
        store.importRecords(fresh)
        return ImportResult(fresh.size, skipped)
    }

    fun startChallenge(days: Int) = challenges().let { store ->
        store.start(days)
        _challenge.value = store.state.value
    }

    fun cancelChallenge() = challenges().let { store ->
        store.cancel()
        _challenge.value = store.state.value
    }
}
