package com.upisoundbox.feature.home

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.data.db.PaymentEntity
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.design.util.AppLogoProvider
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
    val fontScalePref by preferences.fontScale.collectAsState(initial = "compact")
    val todayTotalPaise by repository.getTodayTotal().collectAsState(initial = 0L)
    val todayCount by repository.getTodayCount().collectAsState(initial = 0)
    val recentPayments by repository.getRecentPayments(20).collectAsState(initial = emptyList())

    // Font size multiplier based on user preference
    val scaleMultiplier = when (fontScalePref.lowercase()) {
        "large" -> 1.15f
        "standard" -> 1.05f
        else -> 1.0f // compact default (matching the reference image perfectly)
    }

    // Pulsing animation for the green listening indicator dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    // Background gradient matching the reference designs
    val backgroundBrush = if (colors.isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF090D0B),
                Color(0xFF0C130F),
                Color(0xFF090D0B)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFEBF7F0),
                Color(0xFFF7FAF8),
                Color(0xFFEBF7F0)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            // 1. Top Header: Logo + App Name + Notification Bell & Settings buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.upisoundbox.core.design.R.drawable.ic_app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "UPI Soundbox",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            fontSize = (18 * scaleMultiplier).sp
                        )
                    }

                    // Top Action Buttons (Bell with badge + Settings gear)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Notification Bell Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .border(BorderStroke(1.dp, colors.cardBorder), CircleShape)
                                .clickable { onNavigateHistory() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            // Badge dot
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 9.dp, end = 9.dp)
                                    .size(6.dp)
                                    .background(
                                        if (colors.isDark) Color(0xFF10B981) else Color(0xFFEF4444),
                                        CircleShape
                                    )
                            )
                        }

                        // Settings Gear Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .border(BorderStroke(1.dp, colors.cardBorder), CircleShape)
                                .clickable { onNavigateSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. Listening Status Banner Card with Animated ON/OFF switch
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    border = BorderStroke(1.dp, colors.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(if (isListening) dotPulse else 1f)
                                    .background(
                                        if (isListening) Color(0xFF10B981) else Color(0xFF9CA3AF),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isListening) "Listening for payments" else "Soundbox paused",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                fontSize = (13 * scaleMultiplier).sp
                            )
                        }

                        // Smooth Animated Toggle Pill Button (Fluid slide & color transition)
                        AnimatedListeningSwitch(
                            checked = isListening,
                            onToggle = {
                                scope.launch { preferences.setSoundboxActive(!isListening) }
                            }
                        )
                    }
                }
            }

            // 3. Hero Card: "Today's Collection" + Real Ledger Data
            item {
                val heroGradient = Brush.verticalGradient(
                    listOf(colors.heroGradientStart, colors.heroGradientEnd)
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, colors.heroBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(heroGradient, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Today's Collection",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted,
                            fontSize = (12 * scaleMultiplier).sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val displayAmount = AmountParser.formatPaise(todayTotalPaise ?: 0L)

                        Text(
                            text = displayAmount,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary,
                            fontSize = (30 * scaleMultiplier).sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Status/Ledger Pill (Real status instead of hardcoded numbers)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.pillBackground)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (todayTotalPaise != null && todayTotalPaise!! > 0L) "Today's Ledger Active" else "Ready for payments",
                                color = colors.pillText,
                                fontWeight = FontWeight.Bold,
                                fontSize = (11 * scaleMultiplier).sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Real Stats 2-column row: Payments count & Monthly Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Stat: Payments count
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.statIconBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = "Payments",
                                        tint = colors.statIconTint,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "$todayCount",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        fontSize = (15 * scaleMultiplier).sp
                                    )
                                    Text(
                                        text = "Payments",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted,
                                        fontSize = (11 * scaleMultiplier).sp
                                    )
                                }
                            }

                            // Right Stat: Monthly Total (or real ledger total)
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.statIconBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Monthly Total",
                                        tint = colors.statIconTint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = displayAmount,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        fontSize = (15 * scaleMultiplier).sp
                                    )
                                    Text(
                                        text = "Monthly Total",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted,
                                        fontSize = (11 * scaleMultiplier).sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Quick Actions 4-Grid: My QR, Reports, History, Counter (Replaces Customers)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.QrCode,
                        title = "My QR",
                        scale = scaleMultiplier,
                        onClick = onNavigateQr,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.BarChart,
                        title = "Reports",
                        scale = scaleMultiplier,
                        onClick = onNavigateReports,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.ReceiptLong,
                        title = "History",
                        scale = scaleMultiplier,
                        onClick = onNavigateHistory,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.Storefront,
                        title = "Counter",
                        scale = scaleMultiplier,
                        onClick = onNavigateCounter,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Section Header: "Live Payment Feed" + "See all >"
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Payment Feed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = (16 * scaleMultiplier).sp
                    )
                    Row(
                        modifier = Modifier.clickable { onNavigateHistory() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted,
                            fontSize = (12 * scaleMultiplier).sp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "See all",
                            tint = colors.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // 6. Live Payment Feed Rows (Clean, Real Data Only)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    border = BorderStroke(1.dp, colors.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (recentPayments.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            recentPayments.take(8).forEachIndexed { index, payment ->
                                PaymentFeedRow(
                                    payment = payment,
                                    scale = scaleMultiplier,
                                    onClick = onNavigateHistory
                                )
                                if (index < recentPayments.take(8).size - 1) {
                                    HorizontalDivider(
                                        color = colors.divider,
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Clean, professional empty state (NO fake/mock data)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(colors.statIconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = colors.statIconTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Waiting for live payments...",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (14 * scaleMultiplier).sp,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Incoming payments from PhonePe, Google Pay, and Paytm will be announced aloud and listed here instantly.",
                                fontSize = (11 * scaleMultiplier).sp,
                                color = colors.textMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated ON/OFF Pill Switch with smooth sliding thumb and color animation
 */
@Composable
private fun AnimatedListeningSwitch(
    checked: Boolean,
    onToggle: () -> Unit
) {
    val trackBg by animateColorAsState(
        targetValue = if (checked) Color(0xFF10B981) else Color(0xFF374151),
        animationSpec = tween(250),
        label = "switchBg"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = tween(250),
        label = "switchOffset"
    )

    Box(
        modifier = Modifier
            .width(52.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(trackBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .padding(2.dp)
    ) {
        // Text inside pill
        Text(
            text = if (checked) "ON" else "OFF",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            modifier = Modifier
                .align(if (checked) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 5.dp)
        )

        // Sliding knob
        Box(
            modifier = Modifier
                .size(24.dp)
                .offset(x = thumbOffset)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    title: String,
    scale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundboxColors.current
    Surface(
        modifier = modifier
            .height(68.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                fontSize = (11 * scale).sp
            )
        }
    }
}

@Composable
private fun PaymentFeedRow(
    payment: PaymentEntity,
    scale: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current

    val appName = AnnouncementEngine.mapPackageToDisplayName(payment.appPackage)
    val logoRes = AppLogoProvider.getLogoForPackage(payment.appPackage)
    val timeStr = DateUtils.formatDateTime(context, payment.receivedAt, DateUtils.FORMAT_SHOW_TIME)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = appName,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(11.dp))
            Column {
                Text(
                    text = AmountParser.formatPaise(payment.amountPaise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = (15 * scale).sp
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "$appName • $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    fontSize = (11 * scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Pill badge with checkmark: "✔ Received"
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.pillBackground)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Received",
                    tint = colors.pillText,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Received",
                    color = colors.pillText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (10 * scale).sp
                )
            }
        }
    }
}
