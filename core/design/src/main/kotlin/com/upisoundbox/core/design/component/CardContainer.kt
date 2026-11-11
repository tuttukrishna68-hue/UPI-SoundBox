package com.upisoundbox.core.design.component

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.upisoundbox.core.design.theme.SoundboxShapes
import com.upisoundbox.core.design.theme.SoundboxTheme

@Composable
fun CardContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = SoundboxTheme.colors

    Card(
        modifier = modifier,
        shape = SoundboxShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (colors.isDark) 0.dp else 2.dp
        )
    ) {
        content()
    }
}
