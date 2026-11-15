package com.cherryclip.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cherryclip.app.clipboard.ClipboardHelper
import com.cherryclip.app.clipboard.ClipboardMonitorService
import com.cherryclip.app.data.ClipRepository
import com.cherryclip.app.model.ClipItem
import com.cherryclip.app.model.ClipSnippet
import com.cherryclip.app.model.ClipType
import com.cherryclip.app.model.MainTab
import com.cherryclip.app.model.TextTransformers
import com.cherryclip.app.ui.components.CherryToastCapsule
import com.cherryclip.app.ui.components.ClipDetailAndToolboxSheet
import com.cherryclip.app.ui.components.MergeCopySheet
import com.cherryclip.app.ui.components.NewClipSheet
import com.cherryclip.app.ui.components.QrCodeSheet
import com.cherryclip.app.ui.components.SnippetEditorSheet
import com.cherryclip.app.ui.screens.AppLockScreen
import com.cherryclip.app.ui.screens.HomeScreen
import com.cherryclip.app.ui.screens.SettingsScreen
import com.cherryclip.app.ui.screens.SnippetsScreen
import com.cherryclip.app.ui.theme.CherryClipTheme
import com.cherryclip.app.ui.theme.CherryDimens
import com.cherryclip.app.ui.theme.CherryTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var repository: ClipRepository
    private var lastAutoCapturedText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = (application as CherryClipApp).repository

        if (repository.settings.value.foregroundServiceEnabled) {
            ClipboardMonitorService.start(this)
        }

        setContent {
            CherryClipRoot(
                repository = repository,
                onSyncClipboardNow = { syncClipboardNow(silentIfUnchanged = false) }
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && repository.settings.value.autoCaptureOnFocus) {
            syncClipboardNow(silentIfUnchanged = true)
        }
    }

    private fun syncClipboardNow(silentIfUnchanged: Boolean): Boolean {
        val result = ClipboardHelper.readCurrentClipboard(this)
        var captured = false
        when (result) {
            is ClipboardHelper.ClipboardReadResult.Text -> {
                if (!silentIfUnchanged || result.value != lastAutoCapturedText) {
                    lastAutoCapturedText = result.value
                    captured = true
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        repository.addTextClip(result.value, fromAutoCapture = silentIfUnchanged)
                    }
                }
            }
            is ClipboardHelper.ClipboardReadResult.ImageUri -> {
                captured = true
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    repository.addImageClip(result.uri)
                }
            }
            ClipboardHelper.ClipboardReadResult.Empty -> Unit
        }
        return captured
    }
}

@Composable
private fun CherryClipRoot(
    repository: ClipRepository,
    onSyncClipboardNow: () -> Boolean
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val clips by repository.clips.collectAsState()
    val snippets by repository.snippets.collectAsState()
    val settings by repository.settings.collectAsState()
    val isLocked by repository.isLocked.collectAsState()

    var currentTab by remember { mutableStateOf(MainTab.HISTORY) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    var detailClip by remember { mutableStateOf<ClipItem?>(null) }
    var showNewClipSheet by remember { mutableStateOf(false) }
    var editingSnippet by remember { mutableStateOf<ClipSnippet?>(null) }
    var showSnippetEditor by remember { mutableStateOf(false) }
    var mergeTargets by remember { mutableStateOf<List<ClipItem>?>(null) }
    var qrContent by remember { mutableStateOf<String?>(null) }

    fun showToast(msg: String) {
        toastMessage = msg
    }

    fun triggerHaptic() {
        if (settings.hapticFeedback) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2200)
            toastMessage = null
        }
    }

    // SAF Export & Import Launchers
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = repository.exportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                showToast("已导出备份文件")
            }.onFailure {
                showToast("导出失败: ${it.message}")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val readRes = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("无法读取文件")
                }
                readRes.onSuccess { json ->
                    repository.importBackupJson(json)
                        .onSuccess { (c, s) -> showToast("已恢复 $c 条记录与 $s 条片段") }
                        .onFailure { showToast("恢复失败：文件格式无效") }
                }.onFailure {
                    showToast("读取文件失败")
                }
            }
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            repository.updateSettings { it.copy(foregroundServiceEnabled = true) }
            ClipboardMonitorService.start(context)
            showToast("已开启常驻快捷通知栏")
        } else {
            showToast("需要通知权限以常驻快捷面板")
        }
    }

    CherryClipTheme(themeMode = settings.themeMode) {
        val palette = CherryTheme.palette

        if (isLocked) {
            AppLockScreen(
                onVerifyPin = { pin ->
                    val ok = repository.verifyPinAndUnlock(pin)
                    if (ok) triggerHaptic()
                    ok
                }
            )
            return@CherryClipTheme
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            when (currentTab) {
                MainTab.HISTORY -> HomeScreen(
                    clips = clips,
                    onSyncClipboard = {
                        triggerHaptic()
                        val captured = onSyncClipboardNow()
                        showToast(if (captured) "已同步当前剪贴板" else "当前剪贴板为空")
                    },
                    onCreateNew = { showNewClipSheet = true },
                    onCopyClip = { item ->
                        triggerHaptic()
                        if (item.imagePath != null) {
                            val ok = ClipboardHelper.copyImage(context, item.imagePath)
                            if (ok) {
                                scope.launch { repository.recordCopyUsage(item.id) }
                                showToast("已复制图片到剪贴板")
                            } else {
                                showToast("图片文件不存在")
                            }
                        } else {
                            ClipboardHelper.copyText(context, item.content, isSensitive = item.isSensitive)
                            scope.launch { repository.recordCopyUsage(item.id) }
                            showToast("已复制到剪贴板")
                        }
                    },
                    onCopyText = { text ->
                        triggerHaptic()
                        ClipboardHelper.copyText(context, text)
                        showToast("已复制: $text")
                    },
                    onQuickAction = { item ->
                        when (item.type) {
                            ClipType.URL -> if (!ClipboardHelper.openUrl(context, item.content)) showToast("无法打开链接")
                            ClipType.PHONE -> ClipboardHelper.dialPhone(context, item.content)
                            ClipType.EMAIL -> ClipboardHelper.sendEmail(context, item.content)
                            else -> {
                                ClipboardHelper.copyText(context, item.content)
                                showToast("已复制到剪贴板")
                            }
                        }
                    },
                    onTogglePin = { id ->
                        triggerHaptic()
                        scope.launch { repository.togglePin(id) }
                    },
                    onOpenDetail = { item -> detailClip = item },
                    onBatchPin = { ids, pinned ->
                        scope.launch {
                            repository.pinClips(ids, pinned)
                            showToast("已置顶 ${ids.size} 项")
                        }
                    },
                    onBatchDelete = { ids ->
                        scope.launch {
                            repository.deleteClips(ids)
                            showToast("已删除 ${ids.size} 项")
                        }
                    },
                    onBatchAddTag = { ids, tag ->
                        scope.launch {
                            repository.addTagToClips(ids, tag)
                            showToast("已添加标签 #$tag")
                        }
                    },
                    onOpenMergeSheet = { selected -> mergeTargets = selected }
                )

                MainTab.SNIPPETS -> SnippetsScreen(
                    snippets = snippets,
                    onCopySnippet = { snippet ->
                        triggerHaptic()
                        val sysClip = (ClipboardHelper.readCurrentClipboard(context) as? ClipboardHelper.ClipboardReadResult.Text)?.value.orEmpty()
                        val expanded = TextTransformers.expandSnippetTemplate(snippet.content, sysClip)
                        ClipboardHelper.copyText(context, expanded)
                        scope.launch { repository.incrementSnippetUsage(snippet.id) }
                        showToast("已复制「${snippet.title}」")
                    },
                    onToggleFavorite = { id ->
                        scope.launch { repository.toggleSnippetFavorite(id) }
                    },
                    onNewSnippet = {
                        editingSnippet = null
                        showSnippetEditor = true
                    },
                    onEditSnippet = { snippet ->
                        editingSnippet = snippet
                        showSnippetEditor = true
                    },
                    onDeleteSnippet = { id ->
                        scope.launch {
                            repository.deleteSnippet(id)
                            showToast("已删除片段")
                        }
                    }
                )

                MainTab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    clipCount = clips.size,
                    snippetCount = snippets.size,
                    onUpdateSettings = { transform -> repository.updateSettings(transform) },
                    onToggleForegroundService = { enable ->
                        if (enable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                repository.updateSettings { it.copy(foregroundServiceEnabled = true) }
                                ClipboardMonitorService.start(context)
                                showToast("已开启常驻快捷通知栏")
                            }
                        } else {
                            repository.updateSettings { it.copy(foregroundServiceEnabled = false) }
                            ClipboardMonitorService.stop(context)
                            showToast("已关闭常驻通知栏")
                        }
                    },
                    onSetPinLock = { pin ->
                        repository.setPinLock(pin)
                        showToast(if (pin == null) "已关闭 PIN 锁" else "PIN 锁已更新")
                    },
                    onLockNow = { repository.lockNowIfEnabled() },
                    onClearUnpinned = {
                        scope.launch {
                            repository.clearUnpinned()
                            showToast("已清空未置顶记录")
                        }
                    },
                    onExportBackup = {
                        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                        exportLauncher.launch("cherryclip_backup_$stamp.json")
                    },
                    onImportBackup = {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }
                )
            }

            // Floating Cherry Pill Bottom Navigation
            CherryFloatingNavBar(
                currentTab = currentTab,
                onSelectTab = { currentTab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            )

            // Crisp floating toast
            CherryToastCapsule(
                message = toastMessage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 82.dp)
            )
        }

        // Modal Sheets
        detailClip?.let { item ->
            ClipDetailAndToolboxSheet(
                item = item,
                onDismiss = { detailClip = null },
                onSave = { updated ->
                    scope.launch {
                        repository.updateClip(updated)
                        showToast("已保存更改")
                    }
                },
                onCopy = { text ->
                    triggerHaptic()
                    ClipboardHelper.copyText(context, text, isSensitive = item.isSensitive)
                    scope.launch { repository.recordCopyUsage(item.id) }
                    showToast("已复制到剪贴板")
                },
                onCopyImage = { path ->
                    triggerHaptic()
                    ClipboardHelper.copyImage(context, path)
                    showToast("已复制图片")
                },
                onShare = { target ->
                    if (target.imagePath != null) ClipboardHelper.shareImage(context, target.imagePath)
                    else ClipboardHelper.shareText(context, target.content)
                },
                onShowQr = { text -> qrContent = text },
                onSaveAsSnippet = { title, content ->
                    scope.launch {
                        repository.upsertSnippet(ClipSnippet(title = title, content = content, category = "收藏"))
                        showToast("已存为常用片段")
                    }
                },
                onDelete = { id ->
                    scope.launch {
                        repository.deleteClips(listOf(id))
                        showToast("已删除记录")
                    }
                },
                onShowMessage = { msg -> showToast(msg) }
            )
        }

        if (showNewClipSheet) {
            NewClipSheet(
                onDismiss = { showNewClipSheet = false },
                onReadSystemClipboard = {
                    (ClipboardHelper.readCurrentClipboard(context) as? ClipboardHelper.ClipboardReadResult.Text)?.value
                },
                onCreate = { content, title, tags ->
                    scope.launch {
                        repository.addTextClip(content, title = title, tags = tags)
                        showToast("已保存新记录")
                    }
                }
            )
        }

        if (showSnippetEditor) {
            SnippetEditorSheet(
                initial = editingSnippet,
                onDismiss = { showSnippetEditor = false },
                onSave = { snippet ->
                    scope.launch {
                        repository.upsertSnippet(snippet)
                        showToast("已保存常用片段")
                    }
                }
            )
        }

        mergeTargets?.let { items ->
            MergeCopySheet(
                selectedItems = items,
                onDismiss = { mergeTargets = null },
                onMergeCopy = { merged, saveAsNew ->
                    triggerHaptic()
                    ClipboardHelper.copyText(context, merged)
                    if (saveAsNew) {
                        scope.launch { repository.addTextClip(merged, title = "合并剪贴内容") }
                    }
                    showToast(if (saveAsNew) "已合并复制并保存" else "已合并复制到剪贴板")
                }
            )
        }

        qrContent?.let { text ->
            QrCodeSheet(
                content = text,
                onDismiss = { qrContent = null },
                onCopy = {
                    ClipboardHelper.copyText(context, it)
                    showToast("已复制文本")
                }
            )
        }
    }
}

@Composable
private fun CherryFloatingNavBar(
    currentTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = CherryTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CherryDimens.RadiusFull))
            .background(palette.card.copy(alpha = 0.96f))
            .border(1.dp, palette.borderStrong.copy(alpha = 0.55f), RoundedCornerShape(CherryDimens.RadiusFull))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(
            icon = Icons.Rounded.ContentPaste,
            label = MainTab.HISTORY.label,
            selected = currentTab == MainTab.HISTORY,
            onClick = { onSelectTab(MainTab.HISTORY) },
            modifier = Modifier.weight(1f)
        )
        NavItem(
            icon = Icons.Rounded.Bookmark,
            label = MainTab.SNIPPETS.label,
            selected = currentTab == MainTab.SNIPPETS,
            onClick = { onSelectTab(MainTab.SNIPPETS) },
            modifier = Modifier.weight(1f)
        )
        NavItem(
            icon = Icons.Rounded.Settings,
            label = MainTab.SETTINGS.label,
            selected = currentTab == MainTab.SETTINGS,
            onClick = { onSelectTab(MainTab.SETTINGS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = CherryTheme.palette
    val fg = if (selected) palette.primary else palette.mutedForeground
    val bg = if (selected) palette.primarySoft else androidx.compose.ui.graphics.Color.Transparent

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(CherryDimens.RadiusFull))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
