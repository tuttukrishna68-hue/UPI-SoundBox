package com.upisoundbox.core.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.upisoundbox.core.design.theme.SoundboxTheme

enum class StatusChipState {
    OK,
    Warning,
    Error
}

typealias ChipStatus = StatusChipState

@Composable
fun StatusChip(
    label: String,
    status: StatusChipState,
    modifier: Modifier = Modifier
) {
    val colors = SoundboxTheme.colors
    val (dotColor, bgColor) = when (status) {
        StatusChipState.OK -> colors.success to colors.success.copy(alpha = 0.12f)
        StatusChipState.Warning -> colors.warning to colors.warning.copy(alpha = 0.12f)
        StatusChipState.Error -> colors.error to colors.error.copy(alpha = 0.12f)
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = SoundboxTheme.typography.labelMedium,
            color = colors.textPrimary
        )
    }
}
