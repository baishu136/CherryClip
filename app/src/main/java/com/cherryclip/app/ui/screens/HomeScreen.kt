package com.cherryclip.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.MergeType
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPasteGo
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cherryclip.app.model.ClipFilter
import com.cherryclip.app.model.ClipItem
import com.cherryclip.app.ui.components.CherryBrandMark
import com.cherryclip.app.ui.components.CherryEmptyState
import com.cherryclip.app.ui.components.CherryFilterChipRow
import com.cherryclip.app.ui.components.CherrySearchPill
import com.cherryclip.app.ui.components.ClipCard
import com.cherryclip.app.ui.theme.CherryDimens
import com.cherryclip.app.ui.theme.CherryTheme

@Composable
fun HomeScreen(
    clips: List<ClipItem>,
    onSyncClipboard: () -> Unit,
    onCreateNew: () -> Unit,
    onCopyClip: (ClipItem) -> Unit,
    onCopyText: (String) -> Unit,
    onQuickAction: (ClipItem) -> Unit,
    onTogglePin: (String) -> Unit,
    onOpenDetail: (ClipItem) -> Unit,
    onBatchPin: (Set<String>, Boolean) -> Unit,
    onBatchDelete: (Set<String>) -> Unit,
    onBatchAddTag: (Set<String>, String) -> Unit,
    onOpenMergeSheet: (List<ClipItem>) -> Unit
) {
    val palette = CherryTheme.palette
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ClipFilter.ALL) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showBatchTagDialog by remember { mutableStateOf(false) }

    val isSelectionMode = selectedIds.isNotEmpty()

    val filterCounts = remember(clips) {
        ClipFilter.entries.associateWith { filter ->
            when (filter) {
                ClipFilter.ALL -> clips.size
                ClipFilter.PINNED -> clips.count { it.isPinned }
                else -> clips.count { it.type == filter.type }
            }
        }
    }

    val allTags = remember(clips) {
        clips.flatMap { it.tags }.distinct().sorted()
    }

    val filteredClips = remember(clips, searchQuery, selectedFilter, selectedTag) {
        clips.filter { item ->
            val matchesFilter = when (selectedFilter) {
                ClipFilter.ALL -> true
                ClipFilter.PINNED -> item.isPinned
                else -> item.type == selectedFilter.type
            }
            val matchesTag = selectedTag == null || selectedTag in item.tags
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                item.content.contains(q, ignoreCase = true) ||
                item.title.contains(q, ignoreCase = true) ||
                item.metadata.contains(q, ignoreCase = true) ||
                item.tags.any { it.contains(q, ignoreCase = true) }
            matchesFilter && matchesTag && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Header or Multi-Select Contextual Header
        if (isSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(CherryDimens.RadiusXl))
                    .background(palette.card)
                    .border(1.dp, palette.primary.copy(alpha = 0.4f), RoundedCornerShape(CherryDimens.RadiusXl))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedIds = emptySet() }) {
                    Icon(Icons.Rounded.Close, contentDescription = "取消多选", tint = palette.foreground)
                }
                Text(
                    text = "已选 ${selectedIds.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.foreground
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    selectedIds = if (selectedIds.size == filteredClips.size) emptySet()
                    else filteredClips.map { it.id }.toSet()
                }) {
                    Icon(Icons.Rounded.DoneAll, contentDescription = "全选", tint = palette.mutedForeground)
                }
                IconButton(onClick = {
                    val chosen = clips.filter { it.id in selectedIds }
                    onOpenMergeSheet(chosen)
                    selectedIds = emptySet()
                }) {
                    Icon(Icons.AutoMirrored.Rounded.MergeType, contentDescription = "合并复制", tint = palette.primary)
                }
                IconButton(onClick = { showBatchTagDialog = true }) {
                    Icon(Icons.AutoMirrored.Rounded.Label, contentDescription = "加标签", tint = palette.mutedForeground)
                }
                IconButton(onClick = {
                    onBatchPin(selectedIds, true)
                    selectedIds = emptySet()
                }) {
                    Icon(Icons.Rounded.PushPin, contentDescription = "置顶", tint = palette.mutedForeground)
                }
                IconButton(onClick = {
                    onBatchDelete(selectedIds)
                    selectedIds = emptySet()
                }) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除", tint = palette.destructive)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CherryBrandMark(size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CherryClip",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.foreground
                    )
                    Text(
                        text = "${clips.size} 条记录 · 点击即复制",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.foregroundTertiary
                    )
                }

                // 1-Tap Sync Clipboard pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                        .background(palette.primarySoft)
                        .border(1.dp, palette.primary.copy(alpha = 0.25f), RoundedCornerShape(CherryDimens.RadiusFull))
                        .clickable(onClick = onSyncClipboard)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = "同步剪贴板",
                        tint = palette.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "同步",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.width(8.dp))

                // New manual clip icon button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(palette.foreground)
                        .clickable(onClick = onCreateNew),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "新建",
                        tint = if (palette.isDark) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // Search pill
        CherrySearchPill(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(10.dp))

        // Smart type filter bar
        CherryFilterChipRow(
            selected = selectedFilter,
            counts = filterCounts,
            onSelect = { selectedFilter = it }
        )

        // Optional tag filter strip when user has tags
        AnimatedVisibility(visible = allTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allTags.forEach { tag ->
                    val active = selectedTag == tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                            .background(if (active) palette.brandSoft else palette.secondary)
                            .border(
                                1.dp,
                                if (active) palette.brand.copy(alpha = 0.4f) else palette.borderSubtle,
                                RoundedCornerShape(CherryDimens.RadiusFull)
                            )
                            .clickable { selectedTag = if (active) null else tag }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) palette.brand else palette.mutedForeground
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (filteredClips.isEmpty()) {
            CherryEmptyState(
                icon = Icons.Rounded.ContentPasteGo,
                title = if (searchQuery.isBlank() && selectedFilter == ClipFilter.ALL) "暂无剪贴记录" else "没有找到匹配项",
                subtitle = "复制任意内容后点击右上角「同步」，或通过系统分享 / 选词菜单保存到这里",
                actionLabel = "立即同步系统剪贴板",
                onAction = onSyncClipboard
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredClips, key = { it.id }) { item ->
                    val isSelected = item.id in selectedIds
                    ClipCard(
                        item = item,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onCardClick = {
                            if (isSelectionMode) {
                                selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                            } else {
                                onCopyClip(item)
                            }
                        },
                        onCardLongClick = {
                            selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                        },
                        onCopyOtp = onCopyText,
                        onQuickAction = { onQuickAction(item) },
                        onTogglePin = { onTogglePin(item.id) },
                        onOpenDetail = { onOpenDetail(item) }
                    )
                }
            }
        }
    }

    if (showBatchTagDialog) {
        var tagInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBatchTagDialog = false },
            containerColor = palette.card,
            title = { Text("批量添加标签", color = palette.foreground) },
            text = {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("标签名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onBatchAddTag(selectedIds, tagInput)
                        selectedIds = emptySet()
                        showBatchTagDialog = false
                    }
                ) {
                    Text("确定", color = palette.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchTagDialog = false }) {
                    Text("取消", color = palette.mutedForeground)
                }
            }
        )
    }
}
