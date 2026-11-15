package com.cherryclip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cherryclip.app.model.AppSettings
import com.cherryclip.app.model.ThemeMode
import com.cherryclip.app.ui.components.CherryHairlineDivider
import com.cherryclip.app.ui.components.CherrySectionCard
import com.cherryclip.app.ui.theme.CherryDimens
import com.cherryclip.app.ui.theme.CherryTheme

@Composable
fun SettingsScreen(
    settings: AppSettings,
    clipCount: Int,
    snippetCount: Int,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onToggleForegroundService: (Boolean) -> Unit,
    onSetPinLock: (String?) -> Unit,
    onLockNow: () -> Unit,
    onClearUnpinned: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    val palette = CherryTheme.palette
    var showPinDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.titleMedium, color = palette.foreground)

        // Theme section
        SectionLabel("外观主题")
        CherrySectionCard {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("色彩模式", style = MaterialTheme.typography.titleSmall, color = palette.foreground)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                        .background(palette.secondary)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val active = settings.themeMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                                .background(if (active) palette.card else Color.Transparent)
                                .clickable { onUpdateSettings { it.copy(themeMode = mode) } }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) palette.foreground else palette.mutedForeground,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                CherryHairlineDivider()
                SettingsSwitchRow(
                    icon = Icons.Rounded.Vibration,
                    title = "复制触感反馈",
                    subtitle = "点击复制或同步时提供轻柔震动反馈",
                    checked = settings.hapticFeedback,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(hapticFeedback = v) } }
                )
            }
        }

        // Capture section
        SectionLabel("剪贴板采集")
        CherrySectionCard {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                SettingsSwitchRow(
                    icon = Icons.Rounded.Sync,
                    title = "切回应用自动同步",
                    subtitle = "打开或回到 CherryClip 时自动读取最新剪贴板",
                    checked = settings.autoCaptureOnFocus,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(autoCaptureOnFocus = v) } }
                )
                CherryHairlineDivider()
                SettingsSwitchRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "常驻快捷通知栏",
                    subtitle = "在通知栏保留快捷入口，下拉随时同步/粘贴",
                    checked = settings.foregroundServiceEnabled,
                    onCheckedChange = onToggleForegroundService
                )
                CherryHairlineDivider()
                SettingsSwitchRow(
                    icon = Icons.Rounded.AutoDelete,
                    title = "自动去重并置顶时间",
                    subtitle = "重复复制相同内容时不产生多余卡片",
                    checked = settings.deduplicateClips,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(deduplicateClips = v) } }
                )
                CherryHairlineDivider()
                SettingsSwitchRow(
                    icon = Icons.Rounded.PrivacyTip,
                    title = "自动跳过敏感文本",
                    subtitle = "自动采集时跳过包含密钥、密码或银行卡号的内容",
                    checked = settings.ignoreSensitiveClips,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(ignoreSensitiveClips = v) } }
                )
            }
        }

        // Security & Privacy
        SectionLabel("隐私锁与存储")
        CherrySectionCard {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                SettingsActionRow(
                    icon = Icons.Rounded.Lock,
                    title = "PIN 应用锁",
                    subtitle = if (settings.pinLockHash != null) "已启用 · 点击修改或关闭" else "未设置 · 保护你的历史隐私",
                    onClick = { showPinDialog = true }
                )
                if (settings.pinLockHash != null) {
                    CherryHairlineDivider()
                    SettingsActionRow(
                        icon = Icons.Rounded.Lock,
                        title = "立即锁定",
                        subtitle = "切换到锁屏界面",
                        onClick = onLockNow
                    )
                }
                CherryHairlineDivider()
                // History limit selector
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    Text("保留历史上限", style = MaterialTheme.typography.titleSmall, color = palette.foreground)
                    Spacer(Modifier.height(4.dp))
                    Text("置顶项目不受上限限制（当前共 $clipCount 条记录）", style = MaterialTheme.typography.bodySmall, color = palette.mutedForeground)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(100 to "100条", 500 to "500条", 1000 to "1000条", 0 to "不限").forEach { (limit, label) ->
                            val active = settings.maxHistoryCount == limit
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                                    .background(if (active) palette.primary else palette.secondary)
                                    .clickable { onUpdateSettings { it.copy(maxHistoryCount = limit) } }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) palette.primaryForeground else palette.foreground
                                )
                            }
                        }
                    }
                }
                CherryHairlineDivider()
                SettingsActionRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "清空未置顶记录",
                    subtitle = "保留置顶内容，清理其余临时历史",
                    tint = palette.destructive,
                    onClick = { showClearConfirm = true }
                )
            }
        }

        // Backup & Restore
        SectionLabel("备份与迁移")
        CherrySectionCard {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                SettingsActionRow(
                    icon = Icons.Rounded.Upload,
                    title = "导出 JSON 备份",
                    subtitle = "导出全部 $clipCount 条剪贴记录与 $snippetCount 条常用片段",
                    onClick = onExportBackup
                )
                CherryHairlineDivider()
                SettingsActionRow(
                    icon = Icons.Rounded.Download,
                    title = "从 JSON 备份恢复",
                    subtitle = "合并导入之前导出的剪贴记录与常用片段",
                    onClick = onImportBackup
                )
            }
        }

        // Android 10+ Guide
        SectionLabel("系统级快捷采集技巧 (Android 10+)")
        CherrySectionCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("无需 Root，三种方式随处采集", style = MaterialTheme.typography.titleSmall, color = palette.foreground)
                }
                Text(
                    text = "1. 选词即存：在任意 App 长按选中文字，在弹出菜单点「存入 CherryClip」。\n" +
                        "2. 快捷瓷贴：下拉系统控制中心，添加「CherryClip」磁贴，随时呼出浮窗并同步。\n" +
                        "3. 系统分享：分享图片或文本到 CherryClip，直接永久归档。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.mutedForeground
                )
            }
        }
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = palette.card,
            title = { Text(if (settings.pinLockHash != null) "修改或关闭 PIN 锁" else "设置 PIN 锁", color = palette.foreground) },
            text = {
                Column {
                    Text("输入 4~8 位数字 PIN 码；留空并确定可关闭锁定。", style = MaterialTheme.typography.bodySmall, color = palette.mutedForeground)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 8 && it.all { ch -> ch.isDigit() }) pinInput = it },
                        label = { Text("PIN 码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput.isBlank()) {
                            onSetPinLock(null)
                            showPinDialog = false
                        } else if (pinInput.length >= 4) {
                            onSetPinLock(pinInput)
                            showPinDialog = false
                        }
                    }
                ) {
                    Text("确定", color = palette.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("取消", color = palette.mutedForeground)
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = palette.card,
            title = { Text("清空未置顶记录？", color = palette.foreground) },
            text = { Text("置顶的记录会完整保留，其余历史记录将被永久删除。", style = MaterialTheme.typography.bodyMedium, color = palette.mutedForeground) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearUnpinned()
                        showClearConfirm = false
                    }
                ) {
                    Text("确认清空", color = palette.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消", color = palette.mutedForeground)
                }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = CherryTheme.palette.mutedForeground,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = CherryTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = palette.mutedForeground, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.foreground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.mutedForeground)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.primary,
                uncheckedThumbColor = palette.mutedForeground,
                uncheckedTrackColor = palette.secondary
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = CherryTheme.palette.mutedForeground,
    onClick: () -> Unit
) {
    val palette = CherryTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (tint == palette.destructive) tint else palette.foreground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.mutedForeground)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.foregroundTertiary, modifier = Modifier.size(18.dp))
    }
}
