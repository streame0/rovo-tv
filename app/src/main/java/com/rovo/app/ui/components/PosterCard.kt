package com.rovo.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rovo.app.data.tmdb.TmdbMetaPreview
import com.rovo.app.ui.theme.LocalRoundCorners

/**
 * Shared poster card used by CastDetailScreen and StudioDetailScreen.
 * Displays a 120×180dp poster with focus scale animation and border.
 */
@Composable
fun PosterCard(
    item: TmdbMetaPreview,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val roundCorners = LocalRoundCorners.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val cardShape = if (roundCorners) RoundedCornerShape(if (isFocused) 16.dp else 12.dp) else RectangleShape
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "posterScale")

    Box(
        modifier = modifier
            .width(120.dp)
            .height(180.dp)
            .scale(scale)
            .clip(cardShape)
            .background(Color.White.copy(0.06f))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) accentColor else Color.Transparent,
                shape = cardShape
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
    ) {
        if (item.poster != null) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(cardShape)
            )
        }
    }
}
