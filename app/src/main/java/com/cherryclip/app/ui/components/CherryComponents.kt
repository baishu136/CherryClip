package com.cherryclip.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.30f))
            .background(palette.brandSoft)
            .border(1.dp, palette.brand.copy(alpha = 0.25f), RoundedCornerShape(size * 0.30f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.72f)) {
            val w = this.size.width
            val h = this.size.height

            // Stems
            val stemPathLeft = Path().apply {
                moveTo(w * 0.50f, h * 0.16f)
                cubicTo(w * 0.44f, h * 0.34f, w * 0.34f, h * 0.48f, w * 0.30f, h * 0.60f)
            }
            val stemPathRight = Path().apply {
                moveTo(w * 0.50f, h * 0.16f)
                cubicTo(w * 0.56f, h * 0.34f, w * 0.66f, h * 0.48f, w * 0.70f, h * 0.60f)
            }
            drawPath(stemPathLeft, color = palette.primary, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
            drawPath(stemPathRight, color = palette.primary, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))

            // Leaf
            val leaf = Path().apply {
                moveTo(w * 0.50f, h * 0.16f)
                cubicTo(w * 0.64f, h * 0.08f, w * 0.78f, h * 0.14f, w * 0.75f, h * 0.25f)
                cubicTo(w * 0.65f, h * 0.28f, w * 0.55f, h * 0.23f, w * 0.50f, h * 0.16f)
                close()
            }
            drawPath(leaf, color = palette.primary)

            // Cherries
            val r = w * 0.20f
            drawCircle(color = palette.brand, radius = r, center = Offset(w * 0.30f, h * 0.71f))
            drawCircle(color = palette.brand, radius = r, center = Offset(w * 0.70f, h * 0.71f))
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
fun CherryFilterChipRow(
    selected: ClipFilter,
    counts: Map<ClipFilter, Int>,
    onSelect: (ClipFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = CherryTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ClipFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            val count = counts[filter] ?: 0
            val bg = if (isSelected) palette.foreground else palette.card
            val fg = if (isSelected) {
                if (palette.isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
            } else {
                palette.mutedForeground
            }
            val borderColor = if (isSelected) palette.foreground else palette.border

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(CherryDimens.RadiusFull))
                    .background(bg)
                    .border(1.dp, borderColor, RoundedCornerShape(CherryDimens.RadiusFull))
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = filter.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = fg,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
                if (count > 0 && filter != ClipFilter.ALL) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = fg.copy(alpha = 0.72f)
                    )
                }
            }
        }
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
