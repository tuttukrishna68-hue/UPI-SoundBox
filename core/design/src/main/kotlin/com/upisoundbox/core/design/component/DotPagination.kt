package com.upisoundbox.core.design.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.upisoundbox.core.design.theme.SoundboxTheme

@Composable
fun DotPagination(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    val colors = SoundboxTheme.colors

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until pageCount) {
            val isSelected = i == currentPage

            val dotWidth by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                label = "dotWidth"
            )
            val dotColor by animateColorAsState(
                targetValue = if (isSelected) colors.accentOrange else colors.bubbleGrey,
                label = "dotColor"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(dotWidth)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
