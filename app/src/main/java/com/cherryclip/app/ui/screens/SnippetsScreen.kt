package com.cherryclip.app.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cherryclip.app.model.ClipSnippet
import com.cherryclip.app.ui.components.CherryEmptyState
import com.cherryclip.app.ui.components.CherrySearchPill
import com.cherryclip.app.ui.components.CherryTagBadge
import com.cherryclip.app.ui.theme.CherryDimens
import com.cherryclip.app.ui.theme.CherryTheme

@Composable
fun SnippetsScreen(
    snippets: List<ClipSnippet>,
    onCopySnippet: (ClipSnippet) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onNewSnippet: () -> Unit,
    onEditSnippet: (ClipSnippet) -> Unit,
    onDeleteSnippet: (String) -> Unit
) {
    val palette = CherryTheme.palette
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }

    val categories = remember(snippets) {
        listOf("全部", "收藏") + snippets.map { it.category }.distinct().sorted()
    }

    val filtered = remember(snippets, search, selectedCategory) {
        snippets.filter { s ->
            val matchCat = when (selectedCategory) {
                "全部" -> true
                "收藏" -> s.isFavorite
                else -> s.category == selectedCategory
            }
            val q = search.trim()
            val matchQuery = q.isEmpty() ||
                s.title.contains(q, ignoreCase = true) ||
                s.content.contains(q, ignoreCase = true) ||
                s.category.contains(q, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("常用片段", style = MaterialTheme.typography.titleMedium, color = palette.foreground)
                Text("支持 {{date}} · {{time}} · {{clipboard}} 动态变量", style = MaterialTheme.typography.bodySmall, color = palette.foregroundTertiary)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                    .background(palette.primary)
                    .clickable(onClick = onNewSnippet)
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.primaryForeground, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("新建片段", style = MaterialTheme.typography.labelMedium, color = palette.primaryForeground, fontWeight = FontWeight.SemiBold)
            }
        }

        CherrySearchPill(
            query = search,
            onQueryChange = { search = it },
            placeholder = "搜索常用片段、地址、模板...",
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val active = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                        .background(if (active) palette.foreground else palette.card)
                        .border(1.dp, if (active) palette.foreground else palette.border, RoundedCornerShape(CherryDimens.RadiusFull))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) {
                            if (palette.isDark) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
                        } else palette.mutedForeground,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            CherryEmptyState(
                icon = Icons.Rounded.BookmarkBorder,
                title = "暂无常用片段",
                subtitle = "把收货地址、邮箱、常用回复或开发指令保存成片段，随时一键复制",
                actionLabel = "创建第一个片段",
                onAction = onNewSnippet
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { snippet ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CherryDimens.RadiusXl))
                            .background(palette.card)
                            .border(1.dp, palette.border, RoundedCornerShape(CherryDimens.RadiusXl))
                            .clickable { onCopySnippet(snippet) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CherryTagBadge(
                                text = snippet.category,
                                bgColor = palette.tagBlue,
                                fgColor = palette.tagBlueFg
                            )
                            if (snippet.content.contains("{{")) {
                                Spacer(Modifier.width(6.dp))
                                CherryTagBadge(
                                    text = "动态模板",
                                    bgColor = palette.tagGreen,
                                    fgColor = palette.tagGreenFg
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = { onToggleFavorite(snippet.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (snippet.isFavorite) Icons.Rounded.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "收藏",
                                    tint = if (snippet.isFavorite) palette.tagAmberFg else palette.foregroundTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onEditSnippet(snippet) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "编辑",
                                    tint = palette.mutedForeground,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteSnippet(snippet.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "删除",
                                    tint = palette.foregroundTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = snippet.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.foreground
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = snippet.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.mutedForeground,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (snippet.usageCount > 0) "已使用 ${snippet.usageCount} 次" else "点击卡片一键复制",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.foregroundTertiary
                            )
                            Spacer(Modifier.weight(1f))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                                    .background(palette.primarySoft)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = palette.primary, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("复制", style = MaterialTheme.typography.labelSmall, color = palette.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
