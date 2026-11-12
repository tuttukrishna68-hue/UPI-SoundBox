package com.upisoundbox.feature.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.data.db.AppBreakdown
import com.upisoundbox.core.data.db.DayAggregate
import com.upisoundbox.core.data.db.HourlyAggregate
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.design.component.AmountDisplay
import com.upisoundbox.core.design.component.AmountSize
import com.upisoundbox.core.design.component.AppChip
import com.upisoundbox.core.design.component.CardContainer
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.tts.AnnouncementEngine
import java.util.Calendar

@Composable
fun ReportsScreen(
    repository: PaymentRepository,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSoundboxColors.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Daily (7 days), 1: Weekly (30 days), 2: Monthly (90 days)

    var dayAggregates by remember { mutableStateOf<List<DayAggregate>>(emptyList()) }
    var appBreakdown by remember { mutableStateOf<List<AppBreakdown>>(emptyList()) }
    var hourlyBreakdown by remember { mutableStateOf<List<HourlyAggregate>>(emptyList()) }

    LaunchedEffect(selectedTab) {
        val daysBack = when (selectedTab) {
            0 -> 7
            1 -> 30
            else -> 90
        }
        val sinceTimestamp = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
        dayAggregates = repository.getDailyAggregates(sinceTimestamp)
        appBreakdown = repository.getAppBreakdown(sinceTimestamp)
        hourlyBreakdown = repository.getHourlyBreakdown(sinceTimestamp)
    }

    val totalPaise = remember(dayAggregates) { dayAggregates.sumOf { it.totalPaise } }
    val totalCount = remember(dayAggregates) { dayAggregates.sumOf { it.count } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.ink
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Collection Reports",
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink
            )
        }

        // Time Range Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.paper,
            contentColor = colors.ink,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = colors.accentOrange
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Last 7 Days", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Last 30 Days", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Last 90 Days", fontWeight = FontWeight.SemiBold) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Total Card
            CardContainer(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "TOTAL COLLECTED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AmountDisplay(amountPaise = totalPaise, size = AmountSize.Hero)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "$totalCount total payments received",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Daily Trend Bar Chart (Canvas-based)
            Text(
                text = "Daily Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.ink
            )
            Spacer(modifier = Modifier.height(12.dp))

            DailyTrendChart(
                aggregates = dayAggregates,
                accentColor = colors.accentOrange,
                bubbleColor = colors.bubbleGrey
            )

            Spacer(modifier = Modifier.height(28.dp))

            // App-wise Breakdown List & Chart
            Text(
                text = "Split by UPI App",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.ink
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (appBreakdown.isEmpty()) {
                Text(
                    text = "No payments in this period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            } else {
                for (item in appBreakdown) {
                    val appName = AnnouncementEngine.mapPackageToDisplayName(item.appPackage)
                    val percent = if (totalPaise > 0) (item.totalPaise.toDouble() / totalPaise * 100).toInt() else 0

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppChip(appName = appName)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${item.count} txns",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.muted
                                    )
                                }
                                Text(
                                    text = AmountParser.formatPaise(item.totalPaise),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ink
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(colors.bubbleGrey, RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (percent / 100f).coerceIn(0.05f, 1f))
                                        .height(6.dp)
                                        .background(colors.accentOrange, RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Peak Hours Activity (Hourly Histogram)
            Text(
                text = "Peak Activity Hours",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.ink
            )
            Spacer(modifier = Modifier.height(12.dp))

            HourlyActivityChart(
                hourlyData = hourlyBreakdown,
                accentColor = colors.accentOrange,
                bubbleColor = colors.bubbleGrey
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DailyTrendChart(
    aggregates: List<DayAggregate>,
    accentColor: Color,
    bubbleColor: Color
) {
    if (aggregates.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(bubbleColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No collection data available yet", color = Color.Gray, fontSize = 12.sp)
        }
        return
    }

    val maxAmount = aggregates.maxOfOrNull { it.totalPaise } ?: 1L
    val safeMax = if (maxAmount > 0) maxAmount else 1L

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.Transparent)
            .padding(vertical = 8.dp)
    ) {
        val barCount = aggregates.size
        val barSpacing = 8.dp.toPx()
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = ((size.width - totalSpacing) / barCount).coerceIn(12f, 48f)

        aggregates.forEachIndexed { index, day ->
            val barHeight = ((day.totalPaise.toFloat() / safeMax) * size.height).coerceAtLeast(8f)
            val left = index * (barWidth + barSpacing)
            val top = size.height - barHeight

            drawRoundRect(
                color = accentColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}

@Composable
private fun HourlyActivityChart(
    hourlyData: List<HourlyAggregate>,
    accentColor: Color,
    bubbleColor: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(bubbleColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        val maxTxns = hourlyData.maxOfOrNull { it.count } ?: 1
        val safeMax = if (maxTxns > 0) maxTxns else 1

        // Render 24 hours
        val slotWidth = size.width / 24f

        for (hour in 0 until 24) {
            val count = hourlyData.find { it.hourOfDay == hour }?.count ?: 0
            val barHeight = if (count > 0) (count.toFloat() / safeMax) * size.height else 4f
            val left = hour * slotWidth + 2f
            val top = size.height - barHeight

            drawRoundRect(
                color = if (count > 0) accentColor else Color.LightGray.copy(alpha = 0.4f),
                topLeft = Offset(left, top),
                size = Size((slotWidth - 4f).coerceAtLeast(2f), barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}
