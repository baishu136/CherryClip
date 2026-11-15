package com.cherryclip.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cherryclip.app.model.ClipItem
import com.cherryclip.app.model.ClipType
import com.cherryclip.app.model.ContentClassifier
import com.cherryclip.app.ui.theme.CherryDimens
import com.cherryclip.app.ui.theme.CherryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ClipCard(
    item: ClipItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    onCopyOtp: (String) -> Unit,
    onQuickAction: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = CherryTheme.palette
    val borderColor = when {
        isSelected -> palette.primary
        item.isPinned -> palette.primary.copy(alpha = 0.35f)
        else -> palette.border
    }
    val cardBg = if (isSelected) palette.primarySoft else palette.card

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CherryDimens.RadiusXl))
            .background(cardBg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(CherryDimens.RadiusXl)
            )
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Header meta row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) palette.primary else palette.foregroundTertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            ClipTypeBadge(item.type)

            if (item.metadata.isNotBlank() && item.type != ClipType.OTP && item.type != ClipType.COLOR) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = item.metadata,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.foregroundTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            if (item.isSensitive) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "敏感内容",
                    tint = palette.tagAmberFg,
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = formatRelativeTime(item.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = palette.foregroundTertiary
            )

            if (!isSelectionMode) {
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (item.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (item.isPinned) "取消置顶" else "置顶",
                        tint = if (item.isPinned) palette.brand else palette.foregroundTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onOpenDetail,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = "详情与工具",
                        tint = palette.mutedForeground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (item.title.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(8.dp))

        // Rich body preview by type
        when (item.type) {
            ClipType.COLOR -> {
                val colorInt = ContentClassifier.colorHexToInt(item.metadata)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CherryDimens.RadiusMd))
                        .background(palette.codeBlock)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colorInt?.let { Color(it) } ?: palette.secondary)
                            .border(1.dp, palette.borderStrong, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.metadata.ifBlank { item.content },
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                            color = palette.foreground
                        )
                        if (item.content != item.metadata) {
                            Text(
                                text = item.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.mutedForeground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            ClipType.OTP -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.metadata.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(CherryDimens.RadiusMd))
                                .background(palette.tagGreen)
                                .border(1.dp, palette.primary.copy(alpha = 0.25f), RoundedCornerShape(CherryDimens.RadiusMd))
                                .clickable { onCopyOtp(item.metadata) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                tint = palette.tagGreenFg,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "复制验证码 ${item.metadata}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = palette.tagGreenFg
                            )
                        }
                    }
                    Text(
                        text = item.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.mutedForeground,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            ClipType.CODE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CherryDimens.RadiusMd))
                        .background(palette.codeBlock)
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(CherryDimens.RadiusMd))
                        .padding(10.dp)
                ) {
                    Text(
                        text = item.content,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        ),
                        color = palette.foreground,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            ClipType.IMAGE -> {
                ClipImageThumbnail(
                    imagePath = item.imagePath,
                    caption = item.content
                )
            }

            ClipType.URL -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.tagBlueFg,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isSelectionMode) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                                .background(palette.tagBlue)
                                .clickable { onQuickAction() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = "打开链接",
                                    tint = palette.tagBlueFg,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "打开",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.tagBlueFg
                                )
                            }
                        }
                    }
                }
            }

            ClipType.PHONE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.content,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                        color = palette.foreground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                                .background(palette.tagGreen)
                                .clickable { onQuickAction() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Phone,
                                    contentDescription = "拨号",
                                    tint = palette.tagGreenFg,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "拨打",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.tagGreenFg
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.foreground,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom tags & copy hint row
        if (item.tags.isNotEmpty() || item.copyCount > 1) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.tags.forEach { tag ->
                        CherryTagBadge(
                            text = "#$tag",
                            bgColor = palette.secondary,
                            fgColor = palette.mutedForeground
                        )
                    }
                }
                if (item.copyCount > 1) {
                    Text(
                        text = "复制 ${item.copyCount} 次",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.foregroundTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipImageThumbnail(imagePath: String?, caption: String) {
    val palette = CherryTheme.palette
    val imageBitmap = produceState<ImageBitmap?>(initialValue = null, key1 = imagePath) {
        value = if (imagePath != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(imagePath)
                    if (!file.exists()) return@runCatching null
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
                }.getOrNull()
            }
        } else null
    }

    if (imageBitmap.value != null) {
        Image(
            bitmap = imageBitmap.value!!,
            contentDescription = caption,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(CherryDimens.RadiusMd))
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(CherryDimens.RadiusMd))
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(CherryDimens.RadiusMd))
                .background(palette.codeBlock),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = caption.ifBlank { "图片已保存" },
                style = MaterialTheme.typography.bodySmall,
                color = palette.mutedForeground
            )
        }
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "刚刚"
        diff < 3_600_000L -> "${diff / 60_000L}分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000L}小时前"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000L}天前"
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
    }
}
