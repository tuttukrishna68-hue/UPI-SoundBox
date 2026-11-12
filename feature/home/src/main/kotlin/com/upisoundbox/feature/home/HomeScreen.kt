package com.upisoundbox.feature.home

import android.text.format.DateUtils
import android.widget.Toast
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isListening by preferences.isSoundboxActive.collectAsState(initial = true)
    val userName by preferences.userName.collectAsState(initial = SoundboxPreferences.DEFAULT_USER_NAME)
    val fontScalePref by preferences.fontScale.collectAsState(initial = "compact")
    val todayTotalPaise by repository.getTodayTotal().collectAsState(initial = 0L)
    val todayCount by repository.getTodayCount().collectAsState(initial = 0)
    val monthlyTotalPaise by repository.getMonthlyTotal().collectAsState(initial = 0L)
    val yesterdayTotalPaise by repository.getYesterdayTotal().collectAsState(initial = 0L)
    val recentPayments by repository.getRecentPayments(20).collectAsState(initial = emptyList())
    val lastSeenPaymentAt by preferences.lastSeenPaymentAt.collectAsState(initial = 0L)
    val hasUnreadPayments = recentPayments.firstOrNull()?.receivedAt?.let {
        it > lastSeenPaymentAt
    } ?: false

    val isNotificationAccessGranted = remember {
        androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        try {
            val intent = android.content.Intent("com.upisoundbox.action.RESUME").apply {
                setPackage(context.packageName)
            }
            context.startService(intent)
        } catch (_: Exception) {}
    }

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
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
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

                    // Top Action Buttons (Speaker toggle + Notification Bell + Settings gear)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speaker / Listening Toggle Button (Next to Notification Bell)
                        SpeakerToggle(
                            checked = isListening,
                            onCheckedChange = { newState ->
                                scope.launch { preferences.setSoundboxActive(newState) }
                                Toast.makeText(
                                    context,
                                    if (newState) "🔊 Soundbox listening active" else "🔇 Soundbox listening paused",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        // Notification Bell Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .border(BorderStroke(1.dp, colors.cardBorder), CircleShape)
                                .clickable {
                                    scope.launch {
                                        recentPayments.firstOrNull()?.receivedAt?.let {
                                            preferences.setLastSeenPaymentAt(it)
                                        }
                                    }
                                    onNavigateHistory()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            if (hasUnreadPayments) {
                                // The badge represents unread ledger items, not a permanent decoration.
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 9.dp, end = 9.dp)
                                        .size(6.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                            }
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

            // Notification Access Warning Banner (if disabled in Android settings)
            if (!isNotificationAccessGranted) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (colors.isDark) Color(0xFF26190F) else Color(0xFFFFF7ED)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFF97316)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    context.startActivity(
                                        android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                    )
                                } catch (_: Exception) {}
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF97316).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Notification Access",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (13 * scaleMultiplier).sp,
                                    color = if (colors.isDark) Color(0xFFFED7AA) else Color(0xFF9A3412)
                                )
                                Text(
                                    text = "Tap to grant permission so Google Pay payments are announced.",
                                    fontSize = (11 * scaleMultiplier).sp,
                                    color = colors.textMuted
                                )
                            }
                            Text(
                                text = "ALLOW →",
                                fontWeight = FontWeight.Bold,
                                fontSize = (11 * scaleMultiplier).sp,
                                color = Color(0xFFF97316)
                            )
                        }
                    }
                }
            }

            // 2. Plain merchant greeting — intentionally not a card or editable surface.
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 6.dp)
                ) {
                    Text(
                        text = "Hello, ${userName.ifBlank { SoundboxPreferences.DEFAULT_USER_NAME }}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = (28 * scaleMultiplier).sp,
                        letterSpacing = (-0.6).sp
                    )
                }
            }

            // 3. Hero Card: "Today's Collection" + Real Ledger Data (Ultra-High-End Fintech Architecture)
            item {
                val heroBackgroundBrush = if (colors.isDark) {
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF13422C).copy(alpha = 0.45f),
                            Color(0xFF0C2419),
                            Color(0xFF07140E)
                        ),
                        center = Offset(800f, -100f),
                        radius = 900f
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF0FDF4),
                            Color(0xFFE4F8EB),
                            Color(0xFFDCFCE7).copy(alpha = 0.6f)
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (colors.isDark) Color(0xFF1F4D35) else Color(0xFFB9F1CD)
                    ),
                    shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(heroBackgroundBrush, RoundedCornerShape(22.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp)
                    ) {
                        // Header Row: "Today's Collection" + Live Telemetry Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Collection",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (colors.isDark) Color(0xFF9EABA2) else Color(0xFF4B6354),
                                fontSize = (13 * scaleMultiplier).sp,
                                letterSpacing = 0.2.sp
                            )

                            // Telemetry Live badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x66111820))
                                    .border(
                                        BorderStroke(0.5.dp, Color(0xFFFF4D5D).copy(alpha = 0.55f)),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .scale(dotPulse)
                                            .background(Color(0xFFFF4D5D), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "LIVE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.8.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Large Currency Display: ₹12,480
                        val todayPaise = todayTotalPaise ?: 0L
                        val displayAmount = AmountParser.formatPaise(todayPaise)

                        Text(
                            text = displayAmount,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            fontSize = (34 * scaleMultiplier).sp,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Real Comparison / Trend Badge: ↑ +18% vs yesterday (matching reference design)
                        val yesterdayPaise = yesterdayTotalPaise ?: 0L
                        val growthText = if (yesterdayPaise > 0L) {
                            val diff = todayPaise - yesterdayPaise
                            val pct = (diff * 100 / yesterdayPaise).toInt()
                            if (pct >= 0) "↑ +$pct% vs yesterday" else "↓ ${kotlin.math.abs(pct)}% vs yesterday"
                        } else if (todayPaise > 0L) {
                            "First collection today"
                        } else {
                            "No comparison yet"
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (growthText.startsWith("↑") || growthText.startsWith("↓")) {
                                Icon(
                                    imageVector = if (growthText.startsWith("↓")) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (growthText.startsWith("↓")) colors.error else Color(0xFF35E6A1),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = growthText,
                                color = when {
                                    growthText.startsWith("↓") -> colors.error
                                    growthText.startsWith("No") -> colors.textMuted
                                    else -> Color(0xFF35E6A1)
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (11.5 * scaleMultiplier).sp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // A single, quiet summary rail keeps the two core metrics legible
                        // without stacking competing mini-cards inside the hero.
                        val monthlyPaise = monthlyTotalPaise ?: (todayTotalPaise ?: 0L)
                        val monthlyDisplay = AmountParser.formatPaise(monthlyPaise)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (colors.isDark) Color(0x261A4A32) else Color(0xB8FFFFFF)
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (colors.isDark) Color(0x332FE89E) else Color(0xFFD7F2E0)
                                    ),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DashboardMetric(
                                icon = Icons.Default.Layers,
                                label = "PAYMENTS",
                                value = todayCount.toString(),
                                supportingText = "received today",
                                accent = if (colors.isDark) Color(0xFF42E6A3) else Color(0xFF11824F),
                                scale = scaleMultiplier,
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .width(1.dp)
                                    .height(44.dp)
                                    .background(
                                        if (colors.isDark) Color(0x3348D69B) else Color(0xFFD1E9D9)
                                    )
                            )

                            DashboardMetric(
                                icon = Icons.Default.CalendarToday,
                                label = "MONTHLY TOTAL",
                                value = monthlyDisplay,
                                supportingText = "all collections",
                                accent = if (colors.isDark) Color(0xFF7FE8C0) else Color(0xFF11824F),
                                scale = scaleMultiplier,
                                modifier = Modifier.weight(1.18f)
                            )
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
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
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
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
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

@Composable
private fun SpeakerToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalSoundboxColors.current
    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            if (colors.isDark) Color(0xFF123D2B) else Color(0xFFD7F8E7)
        } else {
            colors.cardBackground
        },
        animationSpec = tween(220),
        label = "speakerTrack"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) colors.emeraldGreen else colors.textMuted.copy(alpha = 0.9f),
        animationSpec = tween(220),
        label = "speakerThumb"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 28.dp else 3.dp,
        animationSpec = tween(260),
        label = "speakerThumbOffset"
    )

    Box(
        modifier = Modifier
            .width(60.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(trackColor)
            .border(
                BorderStroke(
                    1.dp,
                    if (checked) colors.emeraldGreen.copy(alpha = 0.7f) else colors.cardBorder
                ),
                RoundedCornerShape(17.dp)
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(28.dp)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (checked) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = if (checked) "Speaker on" else "Speaker off",
                tint = if (checked) Color(0xFF062719) else colors.background,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


@Composable
private fun DashboardMetric(
    icon: ImageVector,
    label: String,
    value: String,
    supportingText: String,
    accent: Color,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundboxColors.current

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (colors.isDark) 0.14f else 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                color = colors.textMuted,
                fontSize = (9.5f * scale).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(9.dp))

        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = (21 * scale).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.35).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = supportingText,
            color = colors.textMuted,
            fontSize = (10.5f * scale).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (colors.isDark) Color(0xFF131F18) else Color(0xFFF1F5F9))
                    .border(
                        BorderStroke(0.5.dp, if (colors.isDark) Color(0xFF1C3626) else Color(0xFFE2E8F0)),
                        RoundedCornerShape(9.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (colors.isDark) Color(0xFF14231A) else Color(0xFFF1F5F9))
                    .border(
                        BorderStroke(0.5.dp, if (colors.isDark) Color(0xFF1F3D2C) else Color(0xFFE2E8F0)),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = appName,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = AmountParser.formatPaise(payment.amountPaise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = (15.5 * scale).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$appName • $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    fontSize = (11.5 * scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Pill badge with checkmark: "✔ Received"
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (colors.isDark) Color(0xFF102E1E) else Color(0xFFDCFCE7))
                .border(
                    BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Received",
                    tint = if (colors.isDark) Color(0xFF34D399) else Color(0xFF15803D),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Received",
                    color = if (colors.isDark) Color(0xFF34D399) else Color(0xFF15803D),
                    fontWeight = FontWeight.Bold,
                    fontSize = (10.5 * scale).sp
                )
            }
        }
    }
}
