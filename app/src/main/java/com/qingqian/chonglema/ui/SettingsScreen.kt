package com.qingqian.chonglema.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingqian.chonglema.data.BackupManager
import com.qingqian.chonglema.data.CrashLog
import com.qingqian.chonglema.data.ImportResult
import com.qingqian.chonglema.data.ThemeMode
import com.qingqian.chonglema.model.CheckInRecord
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 设置页：打卡设置、外观、数据备份/恢复、关于。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    records: List<CheckInRecord>,
    themeMode: ThemeMode,
    systemDark: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    types: List<String>,
    onAddType: (String) -> String?,
    onRemoveType: (String) -> String?,
    onMoveType: (Int, Int) -> Unit,
    onResetTypes: () -> Unit,
    onImportRecords: (List<CheckInRecord>) -> ImportResult,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toastState = rememberAppToastState()
    var confirmClear by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var pendingCrash by remember { mutableStateOf<String?>(null) }
    var showTypeEditor by remember { mutableStateOf(false) }
    var crashText by remember { mutableStateOf(CrashLog.read(context)) }

    fun toast(message: String) {
        toastState.show(scope, message)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExport
        if (uri == null || json == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                ?: error("openOutputStream returned null")
        }.isSuccess
        toast(if (ok) "已导出 " + records.size + " 条记录" else "导出失败，换个位置再试")
        pendingExport = null
    }

    val crashExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val text = pendingCrash
        if (uri == null || text == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                ?: error("openOutputStream returned null")
        }.isSuccess
        toast(if (ok) "已导出崩溃日志" else "导出失败，换个位置再试")
        pendingCrash = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (text.isNullOrBlank()) {
            toast("读取文件失败")
            return@rememberLauncherForActivityResult
        }
        val parsed = BackupManager.parse(text)
        if (parsed == null) {
            toast("这不是「冲了吗」的备份文件")
            return@rememberLauncherForActivityResult
        }
        val result = onImportRecords(parsed)
        toast("导入完成：新增 " + result.added + " 条，跳过 " + result.skipped + " 条")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { AppToastHost(toastState) },
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            AppearanceCard(mode = themeMode, systemDark = systemDark, onChange = onThemeModeChange)
            Spacer(Modifier.height(16.dp))
            TypesCard(types = types, onOpen = { showTypeEditor = true })
            Spacer(Modifier.height(16.dp))
            DataCard(
                recordCount = records.size,
                onExport = {
                    pendingExport = BackupManager.toJson(records)
                    val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
                    exportLauncher.launch("chonglema-backup-" + stamp + ".json")
                },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                onClear = { confirmClear = true },
            )
            Spacer(Modifier.height(16.dp))
            CrashLogCard(
                hasLog = crashText != null,
                onExport = {
                    val text = crashText ?: return@CrashLogCard
                    pendingCrash = text
                    val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
                    crashExportLauncher.launch("chonglema-crash-" + stamp + ".txt")
                },
                onClear = {
                    CrashLog.clear(context)
                    crashText = null
                    toast("已清除崩溃日志")
                },
            )
            Spacer(Modifier.height(16.dp))
            AboutCard()
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showTypeEditor) {
        TypeManagerDialog(
            types = types,
            onAdd = onAddType,
            onRemove = onRemoveType,
            onMove = onMoveType,
            onReset = onResetTypes,
            onDismiss = { showTypeEditor = false },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("确定清空全部数据？") },
            text = { Text("共 " + records.size + " 条记录将被永久删除，无法恢复。建议先导出备份。") },
            confirmButton = {
                TextButton(onClick = {
                    onClearAll()
                    confirmClear = false
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}

@Composable
private fun AppearanceCard(
    mode: ThemeMode,
    systemDark: Boolean,
    onChange: (ThemeMode) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "外观",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("跟随系统", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (systemDark) "手机当前是夜间模式" else "手机当前是白天模式",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = mode == ThemeMode.SYSTEM,
                    onCheckedChange = { follow ->
                        onChange(
                            if (follow) ThemeMode.SYSTEM
                            else if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT
                        )
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "夜间模式",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (mode == ThemeMode.SYSTEM) "打开后改为手动夜间，跟随系统自动关闭" else "手动选择白天或夜间",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = when (mode) {
                        ThemeMode.SYSTEM -> systemDark
                        ThemeMode.DARK -> true
                        ThemeMode.LIGHT -> false
                    },
                    onCheckedChange = { dark ->
                        onChange(if (dark) ThemeMode.DARK else ThemeMode.LIGHT)
                    },
                )
            }
        }
    }
}

@Composable
private fun TypesCard(types: List<String>, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "打卡类型",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = types.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "点进去可以添加、删除或长按排序",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DataCard(
    recordCount: Int,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "数据",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "记录只保存在这台手机上。导出的 JSON 备份可以放网盘，换手机后能导回来；导入会和现有记录合并，重复的自动跳过。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("导出备份（" + recordCount + " 条）")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("从备份导入")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = recordCount > 0,
            ) {
                Text("清空全部数据", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CrashLogCard(
    hasLog: Boolean,
    onExport: () -> Unit,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "崩溃日志",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (hasLog) {
                    "上次闪退的记录已保存在本机，可以导出后发给开发者排查。"
                } else {
                    "没有崩溃记录。如果 App 闪退，这里会留下最近一次的崩溃信息。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = hasLog,
            ) {
                Text("导出崩溃日志")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = hasLog,
            ) {
                Text(
                    text = "清除崩溃日志",
                    color = if (hasLog) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun AboutCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "冲了吗 v1.1",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "所有记录只存在本机 SQLite 数据库里，不联网、不申请任何权限。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

        }
    }
}
