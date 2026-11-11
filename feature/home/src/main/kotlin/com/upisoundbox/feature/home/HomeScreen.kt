package com.upisoundbox.feature.home

import android.text.format.DateUtils
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.design.component.AmountDisplay
import com.upisoundbox.core.design.component.AmountSize
import com.upisoundbox.core.design.component.AppChip
import com.upisoundbox.core.design.component.CardContainer
import com.upisoundbox.core.design.component.ChatBubble
import com.upisoundbox.core.design.component.ChipStatus
import com.upisoundbox.core.design.component.RoundIconButton
import com.upisoundbox.core.design.component.SectionHeader
import com.upisoundbox.core.design.component.StatusChip
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.tts.AnnouncementEngine
import com.upisoundbox.core.tts.AnnouncementQueue
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: PaymentRepository,
    preferences: SoundboxPreferences,
    announcementQueue: AnnouncementQueue,
    onNavigateCounter: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateQr: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val colors = LocalSoundboxColors.current
    val scope = rememberCoroutineScope()

    val isListening by preferences.isSoundboxActive.collectAsState(initial = true)
    val todayTotalPaise by repository.getTodayTotal().collectAsState(initial = 0L)
    val todayCount by repository.getTodayCount().collectAsState(initial = 0)
    val lastPayment by repository.getLastPayment().collectAsState(initial = null)
    val recentPayments by repository.getRecentPayments(20).collectAsState(initial = emptyList())

    // Pulsing animation for listening ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "UPI Soundbox",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.ink
                )
                Text(
                    text = if (isListening) "Listening for payments..." else "Soundbox Paused",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isListening) colors.success else colors.muted
                )
            }

            // Quick navigation round icon buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "Replay Last",
                    onClick = {
                        scope.launch { announcementQueue.replayLast() }
                    }
                )
                StatusChip(
                    label = if (isListening) "ON" else "OFF",
                    status = if (isListening) ChipStatus.OK else ChipStatus.Warning,
                    modifier = Modifier.clickable {
                        scope.launch { preferences.setSoundboxActive(!isListening) }
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Hero Collection Card
            item {
                CardContainer(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TODAY'S COLLECTION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.muted
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        AmountDisplay(
                            amountPaise = todayTotalPaise ?: 0L,
                            size = AmountSize.Hero
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$todayCount",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ink
                                )
                                Text(
                                    text = "Payments",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                            val avgPaise = if (todayCount > 0) (todayTotalPaise ?: 0L) / todayCount else 0L
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = AmountParser.formatPaise(avgPaise),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ink
                                )
                                Text(
                                    text = "Average",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Master Listening Pulse Button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .scale(pulseScale)
                                .border(3.dp, colors.accentOrange.copy(alpha = 0.35f), CircleShape)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(80.dp)
                            .clickable {
                                scope.launch {
                                    preferences.setSoundboxActive(!isListening)
                                }
                            },
                        shape = CircleShape,
                        color = if (isListening) colors.accentOrange else colors.surface,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isListening) "LIVE" else "OFF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isListening) colors.paper else colors.muted
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Quick Actions Row (Counter Mode, Reports, My QR, History)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "Counter",
                        subtitle = "Full screen",
                        onClick = onNavigateCounter,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        title = "Reports",
                        subtitle = "Analytics",
                        onClick = onNavigateReports,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        title = "My QR",
                        subtitle = "Standee",
                        onClick = onNavigateQr,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        title = "History",
                        subtitle = "All ledger",
                        onClick = onNavigateHistory,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Last Payment Card (if available)
            if (lastPayment != null) {
                item {
                    val p = lastPayment!!
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.orangeTint.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "LAST RECEIVED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accentOrange
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = p.payer ?: "Customer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.ink
                                )
                                Text(
                                    text = AnnouncementEngine.mapPackageToDisplayName(p.appPackage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                AmountDisplay(amountPaise = p.amountPaise, size = AmountSize.Large)
                                val timeAgo = DateUtils.getRelativeTimeSpanString(p.receivedAt)
                                Text(text = "$timeAgo", style = MaterialTheme.typography.bodySmall, color = colors.muted)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Live Feed Header
            item {
                SectionHeader(
                    title = "Live Payment Feed",
                    actionText = "See all",
                    onAction = onNavigateHistory
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (recentPayments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No payments received yet today.\nIncoming payments will appear here in real time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(recentPayments) { payment ->
                    val appName = AnnouncementEngine.mapPackageToDisplayName(payment.appPackage)
                    val timeStr = DateUtils.formatDateTime(
                        androidx.compose.ui.platform.LocalContext.current,
                        payment.receivedAt,
                        DateUtils.FORMAT_SHOW_TIME
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChatBubble(
                            text = "${AmountParser.formatPaise(payment.amountPaise)} received from ${payment.payer ?: "Customer"}",
                            timestamp = timeStr,
                            appChip = {
                                AppChip(appName = appName)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundboxColors.current
    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.ink
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                fontSize = 11.sp
            )
        }
    }
}
