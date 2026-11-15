package com.cherryclip.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cherryclip.app.model.ClipItem
import com.cherryclip.app.model.ClipSnippet
import com.cherryclip.app.model.ClipType
import com.cherryclip.app.model.QrCodeGenerator
import com.cherryclip.app.model.TextTransformers
import com.cherryclip.app.ui.theme.CherryDimens
import com.cherryclip.app.ui.theme.CherryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClipDetailAndToolboxSheet(
    item: ClipItem,
    onDismiss: () -> Unit,
    onSave: (ClipItem) -> Unit,
    onCopy: (String) -> Unit,
    onCopyImage: (String) -> Unit,
    onShare: (ClipItem) -> Unit,
    onShowQr: (String) -> Unit,
    onSaveAsSnippet: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val palette = CherryTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var titleText by remember(item.id) { mutableStateOf(item.title) }
    var contentText by remember(item.id) { mutableStateOf(item.content) }
    var tagsText by remember(item.id) { mutableStateOf(item.tags.joinToString(", ")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.card,
        contentColor = palette.foreground,
        shape = RoundedCornerShape(topStart = CherryDimens.RadiusSheet, topEnd = CherryDimens.RadiusSheet)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClipTypeBadge(item.type)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.foregroundTertiary
                )
                Spacer(Modifier.weight(1f))
                if (item.imagePath == null) {
                    IconButton(onClick = { onShowQr(contentText) }) {
                        Icon(Icons.Rounded.QrCode2, contentDescription = "生成二维码", tint = palette.mutedForeground)
                    }
                    IconButton(onClick = {
                        onSaveAsSnippet(titleText.ifBlank { contentText.take(16) }, contentText)
                    }) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = "存为常用片段", tint = palette.mutedForeground)
                    }
                }
                IconButton(onClick = { onShare(item.copy(content = contentText)) }) {
                    Icon(Icons.Rounded.Share, contentDescription = "分享", tint = palette.mutedForeground)
                }
                IconButton(onClick = {
                    onDelete(item.id)
                    onDismiss()
                }) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除", tint = palette.destructive)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Title & Tags inputs
            CherryOutlinedField(
                value = titleText,
                onValueChange = { titleText = it },
                label = "标题（可选）",
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            CherryOutlinedField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = "标签（逗号分隔，例如 工作, 灵感）",
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            if (item.imagePath != null) {
                val bitmap by produceState<Bitmap?>(null, item.imagePath) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            val file = File(item.imagePath)
                            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                        }.getOrNull()
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = titleText,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(CherryDimens.RadiusLg))
                            .background(palette.codeBlock)
                    )
                }
            } else {
                CherryOutlinedField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = "剪贴内容（${contentText.length} 字符 · ${contentText.lines().size} 行）",
                    minLines = 4,
                    maxLines = 10,
                    monospace = item.type == ClipType.CODE || item.type == ClipType.COLOR
                )

                Spacer(Modifier.height(14.dp))

                // Smart Text Toolbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoFixHigh,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "智能文本工具箱",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.foreground,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToolChip("清理空行/空格") {
                        contentText = TextTransformers.trimAndCleanBlankLines(contentText)
                    }
                    ToolChip("去除链接追踪参数") {
                        TextTransformers.cleanTrackingUrl(contentText)
                            .onSuccess { contentText = it; onShowMessage("已清理追踪参数") }
                            .onFailure { onShowMessage("当前内容不是有效链接") }
                    }
                    ToolChip("JSON 美化") {
                        TextTransformers.prettyJson(contentText)
                            .onSuccess { contentText = it }
                            .onFailure { onShowMessage("不是有效的 JSON") }
                    }
                    ToolChip("JSON 压缩") {
                        TextTransformers.minifyJson(contentText)
                            .onSuccess { contentText = it }
                            .onFailure { onShowMessage("不是有效的 JSON") }
                    }
                    ToolChip("Base64 编码") {
                        contentText = TextTransformers.encodeBase64(contentText)
                    }
                    ToolChip("Base64 解码") {
                        TextTransformers.decodeBase64(contentText)
                            .onSuccess { contentText = it }
                            .onFailure { onShowMessage("Base64 解码失败") }
                    }
                    ToolChip("URL 编码") {
                        contentText = TextTransformers.encodeUrl(contentText)
                    }
                    ToolChip("URL 解码") {
                        TextTransformers.decodeUrl(contentText)
                            .onSuccess { contentText = it }
                            .onFailure { onShowMessage("URL 解码失败") }
                    }
                    ToolChip("全大写") {
                        contentText = TextTransformers.toUpperCase(contentText)
                    }
                    ToolChip("全小写") {
                        contentText = TextTransformers.toLowerCase(contentText)
                    }
                    ToolChip("提取链接") {
                        val links = TextTransformers.extractLinks(contentText)
                        if (links.isNotEmpty()) contentText = links else onShowMessage("未检测到链接")
                    }
                    ToolChip("提取邮箱") {
                        val emails = TextTransformers.extractEmails(contentText)
                        if (emails.isNotEmpty()) contentText = emails else onShowMessage("未检测到邮箱")
                    }
                    ToolChip("提取数字") {
                        val nums = TextTransformers.extractNumbers(contentText)
                        if (nums.isNotEmpty()) contentText = nums else onShowMessage("未检测到数字")
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Primary bottom buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                        .background(palette.secondary)
                        .clickable {
                            val parsedTags = tagsText.split(",", "，")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .distinct()
                            onSave(
                                item.copy(
                                    title = titleText.trim(),
                                    content = contentText,
                                    tags = parsedTags
                                )
                            )
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = palette.foreground, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("保存修改", style = MaterialTheme.typography.labelMedium, color = palette.foreground, fontWeight = FontWeight.SemiBold)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                        .background(palette.primary)
                        .clickable {
                            if (item.imagePath != null) {
                                onCopyImage(item.imagePath)
                            } else {
                                onCopy(contentText)
                            }
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = palette.primaryForeground, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("复制内容", style = MaterialTheme.typography.labelMedium, color = palette.primaryForeground, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewClipSheet(
    onDismiss: () -> Unit,
    onReadSystemClipboard: () -> String?,
    onCreate: (content: String, title: String, tags: List<String>) -> Unit
) {
    val palette = CherryTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var content by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.card,
        contentColor = palette.foreground,
        shape = RoundedCornerShape(topStart = CherryDimens.RadiusSheet, topEnd = CherryDimens.RadiusSheet)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("新建剪贴记录", style = MaterialTheme.typography.titleMedium, color = palette.foreground)
                Spacer(Modifier.weight(1f))
                ToolChip("填入系统剪贴板") {
                    val sys = onReadSystemClipboard()
                    if (!sys.isNullOrBlank()) content = sys
                }
            }
            Spacer(Modifier.height(12.dp))
            CherryOutlinedField(
                value = title,
                onValueChange = { title = it },
                label = "标题（可选）",
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            CherryOutlinedField(
                value = content,
                onValueChange = { content = it },
                label = "输入或粘贴内容...",
                minLines = 4,
                maxLines = 8
            )
            Spacer(Modifier.height(10.dp))
            CherryOutlinedField(
                value = tags,
                onValueChange = { tags = it },
                label = "标签（逗号分隔，可选）",
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                    .background(if (content.isNotBlank()) palette.primary else palette.secondary)
                    .clickable(enabled = content.isNotBlank()) {
                        val parsedTags = tags.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                        onCreate(content, title.trim(), parsedTags)
                        onDismiss()
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "保存到 CherryClip",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (content.isNotBlank()) palette.primaryForeground else palette.foregroundTertiary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetEditorSheet(
    initial: ClipSnippet?,
    onDismiss: () -> Unit,
    onSave: (ClipSnippet) -> Unit
) {
    val palette = CherryTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "常用") }
    var content by remember(initial) { mutableStateOf(initial?.content ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.card,
        contentColor = palette.foreground,
        shape = RoundedCornerShape(topStart = CherryDimens.RadiusSheet, topEnd = CherryDimens.RadiusSheet)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = if (initial == null) "新建常用片段" else "编辑常用片段",
                style = MaterialTheme.typography.titleMedium,
                color = palette.foreground
            )
            Spacer(Modifier.height(12.dp))
            CherryOutlinedField(
                value = title,
                onValueChange = { title = it },
                label = "片段名称（例如 收货地址 / 邮箱签名）",
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            CherryOutlinedField(
                value = category,
                onValueChange = { category = it },
                label = "分类",
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            Text("插入动态变量（点击添加）：", style = MaterialTheme.typography.labelSmall, color = palette.mutedForeground)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("{{date}}", "{{time}}", "{{datetime}}", "{{weekday}}", "{{clipboard}}", "{{uuid}}").forEach { token ->
                    ToolChip(token) { content += token }
                }
            }
            Spacer(Modifier.height(10.dp))
            CherryOutlinedField(
                value = content,
                onValueChange = { content = it },
                label = "片段正文",
                minLines = 4,
                maxLines = 8
            )
            Spacer(Modifier.height(16.dp))
            val canSave = title.isNotBlank() && content.isNotBlank()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                    .background(if (canSave) palette.primary else palette.secondary)
                    .clickable(enabled = canSave) {
                        val next = (initial ?: ClipSnippet(title = "", content = "")).copy(
                            title = title.trim(),
                            category = category.trim().ifBlank { "常用" },
                            content = content
                        )
                        onSave(next)
                        onDismiss()
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "保存片段",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (canSave) palette.primaryForeground else palette.foregroundTertiary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeCopySheet(
    selectedItems: List<ClipItem>,
    onDismiss: () -> Unit,
    onMergeCopy: (mergedText: String, saveAsNew: Boolean) -> Unit
) {
    val palette = CherryTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val presets = listOf(
        "换行" to "\n",
        "空行" to "\n\n",
        "空格" to " ",
        "逗号" to ", ",
        "无分隔" to ""
    )
    var selectedSeparator by remember { mutableStateOf("\n") }
    val mergedPreview = remember(selectedItems, selectedSeparator) {
        selectedItems.filter { it.imagePath == null }.joinToString(selectedSeparator) { it.content }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.card,
        contentColor = palette.foreground,
        shape = RoundedCornerShape(topStart = CherryDimens.RadiusSheet, topEnd = CherryDimens.RadiusSheet)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "合并复制 (${selectedItems.size} 项)",
                style = MaterialTheme.typography.titleMedium,
                color = palette.foreground
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (label, sep) ->
                    val active = selectedSeparator == sep
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                            .background(if (active) palette.primary else palette.secondary)
                            .clickable { selectedSeparator = sep }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) palette.primaryForeground else palette.foreground
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 200.dp)
                    .clip(RoundedCornerShape(CherryDimens.RadiusMd))
                    .background(palette.codeBlock)
                    .border(1.dp, palette.border, RoundedCornerShape(CherryDimens.RadiusMd))
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = mergedPreview.ifBlank { "所选项目暂无可合并的文本内容" },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.foreground
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                        .background(palette.secondary)
                        .clickable(enabled = mergedPreview.isNotBlank()) {
                            onMergeCopy(mergedPreview, true)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("合并并存为新条", style = MaterialTheme.typography.labelMedium, color = palette.foreground, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                        .background(palette.primary)
                        .clickable(enabled = mergedPreview.isNotBlank()) {
                            onMergeCopy(mergedPreview, false)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("仅合并复制", style = MaterialTheme.typography.labelMedium, color = palette.primaryForeground, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeSheet(
    content: String,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    val palette = CherryTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val qrBitmap by produceState<Bitmap?>(null, content) {
        value = withContext(Dispatchers.Default) {
            runCatching { QrCodeGenerator.generateBitmap(content.take(1500), 600) }.getOrNull()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.card,
        contentColor = palette.foreground,
        shape = RoundedCornerShape(topStart = CherryDimens.RadiusSheet, topEnd = CherryDimens.RadiusSheet)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("扫码传输 / 分享二维码", style = MaterialTheme.typography.titleMedium, color = palette.foreground)
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(224.dp)
                    .clip(RoundedCornerShape(CherryDimens.RadiusXl))
                    .background(Color.White)
                    .border(1.dp, palette.border, RoundedCornerShape(CherryDimens.RadiusXl))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "二维码",
                        modifier = Modifier.size(200.dp)
                    )
                } else {
                    Text("生成中...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = palette.mutedForeground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                    .background(palette.primary)
                    .clickable {
                        onCopy(content)
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("复制文本", style = MaterialTheme.typography.labelMedium, color = palette.primaryForeground, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ToolChip(
    label: String,
    onClick: () -> Unit
) {
    val palette = CherryTheme.palette
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(CherryDimens.RadiusFull))
            .background(palette.secondary)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(CherryDimens.RadiusFull))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.foreground,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CherryOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6,
    monospace: Boolean = false
) {
    val palette = CherryTheme.palette
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            color = palette.foreground
        ),
        shape = RoundedCornerShape(CherryDimens.RadiusLg),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.primary,
            unfocusedBorderColor = palette.border,
            focusedLabelColor = palette.primary,
            unfocusedLabelColor = palette.mutedForeground,
            cursorColor = palette.primary,
            focusedContainerColor = palette.backgroundSubtle,
            unfocusedContainerColor = palette.backgroundSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
