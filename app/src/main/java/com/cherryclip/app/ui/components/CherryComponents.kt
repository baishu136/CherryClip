package com.cherryclip.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cherryclip.app.model.ClipFilter
import com.cherryclip.app.model.ClipType
import com.cherryclip.app.ui.theme.CherryDimens
import com.cherryclip.app.ui.theme.CherryTheme

@Composable
fun CherryBrandMark(
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val palette = CherryTheme.palette

    // 配色取自应用图标（紫蓝剪贴板堆叠），固定色值，不随主题主色变化
    val cardBack = Color(0xFFB8BCFE)
    val cardMiddle = Color(0xFF8A8FFC)
    val clipTab = Color(0xFF8E95FC)
    val cardFront = Color(0xFFDFE2FE)
    val containerBg = if (palette.isDark) Color(0xFF1C1D33) else Color(0xFFF2F3FE)
    val containerOutline = if (palette.isDark) Color(0x33A9AEFC) else Color(0x2E959BFB)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.30f))
            .background(containerBg)
            .border(1.dp, containerOutline, RoundedCornerShape(size * 0.30f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.78f)) {
            val w = this.size.width
            val h = this.size.height

            fun card(x: Float, y: Float, cw: Float, ch: Float, radius: Float, color: Color) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * x, h * y),
                    size = Size(w * cw, h * ch),
                    cornerRadius = CornerRadius(w * radius, w * radius)
                )
            }

            // 三张错位堆叠的卡片：最后一张最靠左上
            card(0.01f, 0.31f, 0.56f, 0.60f, 0.11f, cardBack)
            card(0.13f, 0.20f, 0.56f, 0.60f, 0.11f, cardMiddle)
            // 夹子（先画，随后被前卡片盖住下半部分）
            card(0.405f, 0.07f, 0.26f, 0.26f, 0.09f, clipTab)
            card(0.25f, 0.28f, 0.56f, 0.60f, 0.11f, cardFront)
            // 夹子上的圆孔
            drawCircle(
                color = containerBg,
                radius = w * 0.06f,
                center = Offset(w * 0.535f, h * 0.19f)
            )
        }
    }
}

@Composable
fun CherrySearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "搜索剪贴历史、标签、链接...",
    modifier: Modifier = Modifier
) {
    val palette = CherryTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(CherryDimens.RadiusFull))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(CherryDimens.RadiusFull))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = palette.foregroundTertiary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.foregroundTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = palette.foreground,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(palette.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "清除",
                    tint = palette.foregroundTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CherryFilterRail(
    selected: ClipFilter,
    counts: Map<ClipFilter, Int>,
    onSelect: (ClipFilter) -> Unit,
    onNewClip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = CherryTheme.palette
    var expanded by remember { mutableStateOf(false) }
    val scrimInteraction = remember { MutableInteractionSource() }
    val railShape = RoundedCornerShape(CherryDimens.RadiusLg)

    Box(modifier = modifier) {
        // 展开时背后的遮罩，点击空白处收起
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (palette.isDark) 0.55f else 0.26f))
                    .clickable(interactionSource = scrimInteraction, indication = null) {
                        expanded = false
                    }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(198.dp)
                    .shadow(16.dp, RoundedCornerShape(CherryDimens.RadiusXl))
                    .clip(RoundedCornerShape(CherryDimens.RadiusXl))
                    .background(palette.card)
                    .border(
                        1.dp,
                        palette.borderStrong.copy(alpha = 0.5f),
                        RoundedCornerShape(CherryDimens.RadiusXl)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 6.dp, top = 2.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "筛选",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.foregroundTertiary
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${counts[ClipFilter.ALL] ?: 0} 条",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.foregroundTertiary
                    )
                }

                ClipFilter.entries.forEach { filter ->
                    FilterTabRow(
                        filter = filter,
                        count = counts[filter] ?: 0,
                        selected = filter == selected,
                        onClick = {
                            onSelect(filter)
                            expanded = false
                        }
                    )
                }

                CherryHairlineDivider(modifier = Modifier.padding(vertical = 3.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CherryDimens.RadiusMd))
                        .clickable {
                            expanded = false
                            onNewClip()
                        }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "新建记录",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 收起态：右侧居中的悬浮入口
        if (!expanded) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .shadow(10.dp, railShape)
                    .clip(railShape)
                    .background(palette.card.copy(alpha = 0.96f))
                    .border(1.dp, palette.borderStrong.copy(alpha = 0.45f), railShape)
                    .clickable { expanded = true }
                    .padding(horizontal = 9.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val activeColor = if (selected == ClipFilter.ALL) palette.mutedForeground else palette.primary
                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = "展开筛选项",
                    tint = activeColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = selected.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = activeColor,
                    fontWeight = if (selected == ClipFilter.ALL) FontWeight.Normal else FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun FilterTabRow(
    filter: ClipFilter,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = CherryTheme.palette
    val bg = if (selected) palette.foreground else Color.Transparent
    val fg = if (selected) {
        if (palette.isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    } else {
        palette.foreground
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CherryDimens.RadiusMd))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = filter.label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) fg.copy(alpha = 0.72f) else palette.foregroundTertiary
        )
    }
}

@Composable
fun CherryTagBadge(
    text: String,
    bgColor: Color,
    fgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CherryDimens.RadiusFull))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fgColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ClipTypeBadge(type: ClipType) {
    val palette = CherryTheme.palette
    val (bg, fg) = when (type) {
        ClipType.URL -> palette.tagBlue to palette.tagBlueFg
        ClipType.CODE -> palette.tagViolet to palette.tagVioletFg
        ClipType.OTP -> palette.tagGreen to palette.tagGreenFg
        ClipType.COLOR -> palette.tagRed to palette.tagRedFg
        ClipType.EMAIL -> palette.tagBlue to palette.tagBlueFg
        ClipType.PHONE -> palette.tagGreen to palette.tagGreenFg
        ClipType.IMAGE -> palette.tagAmber to palette.tagAmberFg
        ClipType.TEXT -> palette.secondary to palette.mutedForeground
    }
    CherryTagBadge(text = type.label, bgColor = bg, fgColor = fg)
}

@Composable
fun CherrySectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = CherryTheme.palette
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CherryDimens.RadiusXl))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(CherryDimens.RadiusXl))
    ) {
        content()
    }
}

@Composable
fun CherryHairlineDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = CherryTheme.palette.borderSubtle
    )
}

@Composable
fun CherryEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = CherryTheme.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(palette.secondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.mutedForeground,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = palette.foreground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.mutedForeground,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                    .background(palette.primary)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primaryForeground,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CherryToastCapsule(
    message: String?,
    modifier: Modifier = Modifier
) {
    val palette = CherryTheme.palette
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                .background(if (palette.isDark) Color(0xFF262626) else Color(0xFF171717))
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(CherryDimens.RadiusFull))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF28C96D),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
