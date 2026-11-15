package com.cherryclip.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
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
import com.cherryclip.app.clipboard.ClipboardHelper
import com.cherryclip.app.data.ClipRepository
import com.cherryclip.app.model.TextTransformers
import com.cherryclip.app.ui.components.CherryBrandMark
import com.cherryclip.app.ui.components.CherrySearchPill
import com.cherryclip.app.ui.components.ClipTypeBadge
import com.cherryclip.app.ui.theme.CherryClipTheme
import com.cherryclip.app.ui.theme.CherryDimens
import com.cherryclip.app.ui.theme.CherryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickPasteActivity : ComponentActivity() {

    private lateinit var repository: ClipRepository
    private var syncedOnOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = (application as CherryClipApp).repository

        setContent {
            val clips by repository.clips.collectAsState()
            val snippets by repository.snippets.collectAsState()
            val settings by repository.settings.collectAsState()
            val isLocked by repository.isLocked.collectAsState()

            CherryClipTheme(themeMode = settings.themeMode, updateSystemBars = false) {
                val palette = CherryTheme.palette
                var query by remember { mutableStateOf("") }
                var showSnippets by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.scrim)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { finish() },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp)
                            .clip(RoundedCornerShape(topStart = CherryDimens.RadiusSheet, topEnd = CherryDimens.RadiusSheet))
                            .background(palette.card)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {}
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        // Grabber
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(palette.borderStrong)
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CherryBrandMark(size = 28.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("快捷剪贴板", style = MaterialTheme.typography.titleSmall, color = palette.foreground)
                            Spacer(Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                                    .background(if (showSnippets) palette.primarySoft else palette.secondary)
                                    .clickable { showSnippets = !showSnippets }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Bookmark,
                                        contentDescription = null,
                                        tint = if (showSnippets) palette.primary else palette.mutedForeground,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (showSnippets) "切回历史" else "常用片段",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (showSnippets) palette.primary else palette.foreground
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    syncNow(showToast = true)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Sync, contentDescription = "同步", tint = palette.primary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    startActivity(Intent(this@QuickPasteActivity, MainActivity::class.java))
                                    finish()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "打开主界面", tint = palette.mutedForeground, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        if (isLocked) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "已开启 PIN 锁定，请点击右上角打开主界面解锁",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.mutedForeground
                                )
                            }
                        } else {
                            CherrySearchPill(
                                query = query,
                                onQueryChange = { query = it },
                                placeholder = if (showSnippets) "搜索常用片段..." else "搜索最近剪贴..."
                            )
                            Spacer(Modifier.height(10.dp))

                            if (showSnippets) {
                                val filteredSnippets = snippets.filter {
                                    query.isBlank() || it.title.contains(query, true) || it.content.contains(query, true)
                                }
                                LazyColumn(
                                    contentPadding = PaddingValues(bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredSnippets.take(20), key = { it.id }) { snippet ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(CherryDimens.RadiusLg))
                                                .background(palette.backgroundSubtle)
                                                .border(1.dp, palette.border, RoundedCornerShape(CherryDimens.RadiusLg))
                                                .clickable {
                                                    val sys = (ClipboardHelper.readCurrentClipboard(this@QuickPasteActivity) as? ClipboardHelper.ClipboardReadResult.Text)?.value.orEmpty()
                                                    val expanded = TextTransformers.expandSnippetTemplate(snippet.content, sys)
                                                    ClipboardHelper.copyText(this@QuickPasteActivity, expanded)
                                                    CoroutineScope(Dispatchers.IO).launch { repository.incrementSnippetUsage(snippet.id) }
                                                    Toast.makeText(this@QuickPasteActivity, "已复制「${snippet.title}」", Toast.LENGTH_SHORT).show()
                                                    finish()
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Text(snippet.title, style = MaterialTheme.typography.labelMedium, color = palette.foreground, fontWeight = FontWeight.SemiBold)
                                            Spacer(Modifier.height(2.dp))
                                            Text(snippet.content, style = MaterialTheme.typography.bodySmall, color = palette.mutedForeground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            } else {
                                val filteredClips = clips.filter {
                                    query.isBlank() || it.content.contains(query, true) || it.title.contains(query, true)
                                }
                                LazyColumn(
                                    contentPadding = PaddingValues(bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredClips.take(25), key = { it.id }) { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(CherryDimens.RadiusLg))
                                                .background(palette.backgroundSubtle)
                                                .border(1.dp, palette.border, RoundedCornerShape(CherryDimens.RadiusLg))
                                                .clickable {
                                                    if (item.imagePath != null) {
                                                        ClipboardHelper.copyImage(this@QuickPasteActivity, item.imagePath)
                                                    } else {
                                                        ClipboardHelper.copyText(this@QuickPasteActivity, item.content, isSensitive = item.isSensitive)
                                                    }
                                                    CoroutineScope(Dispatchers.IO).launch { repository.recordCopyUsage(item.id) }
                                                    Toast.makeText(this@QuickPasteActivity, "已复制", Toast.LENGTH_SHORT).show()
                                                    finish()
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ClipTypeBadge(item.type)
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = item.title.ifBlank { item.content },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.foreground,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !syncedOnOpen) {
            syncedOnOpen = true
            syncNow(showToast = false)
        }
    }

    private fun syncNow(showToast: Boolean) {
        when (val result = ClipboardHelper.readCurrentClipboard(this)) {
            is ClipboardHelper.ClipboardReadResult.Text -> {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.addTextClip(result.value, fromAutoCapture = true)
                }
                if (showToast) Toast.makeText(this, "已同步当前剪贴板", Toast.LENGTH_SHORT).show()
            }
            is ClipboardHelper.ClipboardReadResult.ImageUri -> {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.addImageClip(result.uri)
                }
                if (showToast) Toast.makeText(this, "已同步图片", Toast.LENGTH_SHORT).show()
            }
            ClipboardHelper.ClipboardReadResult.Empty -> {
                if (showToast) Toast.makeText(this, "当前剪贴板为空", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EXTRA_SYNC_ONLY = "sync_only"
    }
}
