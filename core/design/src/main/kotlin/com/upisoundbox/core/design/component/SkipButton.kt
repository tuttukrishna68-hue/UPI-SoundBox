package com.upisoundbox.core.design.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upisoundbox.core.design.theme.PlusJakartaSans
import com.upisoundbox.core.design.theme.SoundboxColors
import com.upisoundbox.core.design.theme.SoundboxTheme

@Composable
fun SkipButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String = "Skip"
) {
    Text(
        text = text,
        style = SoundboxTheme.typography.titleMedium.copy(
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            color = if (enabled) SoundboxColors.AccentOrange else SoundboxColors.Muted
        ),
        modifier = modifier
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
