package com.upisoundbox.feature.history

import android.content.Context
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.data.db.PaymentEntity
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.design.component.AmountDisplay
import com.upisoundbox.core.design.component.AmountSize
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.design.util.AppLogoProvider
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.tts.AnnouncementEngine
import com.upisoundbox.core.tts.AnnouncementQueue
import com.upisoundbox.core.tts.AnnouncementRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    repository: PaymentRepository,
    announcementQueue: AnnouncementQueue,
    preferences: SoundboxPreferences,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current
    val scope = rememberCoroutineScope()

    val today = remember { LocalDate.now() }
    val zoneId = remember { ZoneId.systemDefault() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }

    val allPayments by repository.getAllPayments().collectAsState(initial = emptyList())

    // Opening the ledger is the read action for the notification bell. Persist the
    // latest received timestamp so the badge disappears after the merchant sees it.
    LaunchedEffect(allPayments.firstOrNull()?.receivedAt) {
        allPayments.firstOrNull()?.receivedAt?.let { preferences.setLastSeenPaymentAt(it) }
    }

    // Set of distinct dates that contain at least one payment
    val datesWithPayments = remember(allPayments) {
        allPayments.map {
            Instant.ofEpochMilli(it.receivedAt).atZone(zoneId).toLocalDate()
        }.toSet()
    }

    // Dynamic scrollable date list: At least 30 days of past history up to the end of the current week,
    // or further back if older payments exist in the ledger
    val dates = remember(today, allPayments) {
        val earliestPaymentDate = allPayments.minOfOrNull {
            Instant.ofEpochMilli(it.receivedAt).atZone(zoneId).toLocalDate()
        }
        val defaultStart = today.minusDays(28)
        val start = if (earliestPaymentDate != null && earliestPaymentDate.isBefore(defaultStart)) {
            earliestPaymentDate
        } else {
            defaultStart
        }
        val daysUntilEndOfWeek = (7 - today.dayOfWeek.value).coerceAtLeast(0)
        val end = today.plusDays(daysUntilEndOfWeek.toLong())
        val list = mutableListOf<LocalDate>()
        var curr = start
        while (!curr.isAfter(end)) {
            list.add(curr)
            curr = curr.plusDays(1)
        }
        list
    }

    // Filter payments in memory for instant, offline responsiveness
    val filteredPayments = remember(allPayments, searchQuery, selectedDate) {
        allPayments.filter { payment ->
            val matchesQuery = searchQuery.isBlank() ||
                    (payment.payer?.contains(searchQuery, ignoreCase = true) == true) ||
                    (payment.upiRef?.contains(searchQuery, ignoreCase = true) == true)

            val matchesDate = if (selectedDate == null) {
                true
            } else {
                val paymentDate = Instant.ofEpochMilli(payment.receivedAt).atZone(zoneId).toLocalDate()
                paymentDate == selectedDate
            }

            matchesQuery && matchesDate
        }
    }

    // Group by day string for the "All Days" view
    val groupedPayments = remember(filteredPayments) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        filteredPayments.groupBy { payment ->
            if (DateUtils.isToday(payment.receivedAt)) {
                "Today"
            } else {
                dateFormat.format(Date(payment.receivedAt))
            }
        }
    }

    // CSV Export Launcher
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                writePaymentsToCsv(context, uri, filteredPayments)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. App Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Payment History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            IconButton(
                onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    exportCsvLauncher.launch("UPI_Soundbox_Ledger_$timestamp.csv")
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Export CSV",
                    tint = Color(0xFF10B981)
                )
            }
        }

        // 2. Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by customer name or UPI Ref...", color = colors.textMuted, fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.cardBackground,
                unfocusedContainerColor = colors.cardBackground,
                unfocusedBorderColor = colors.cardBorder,
                focusedBorderColor = Color(0xFF10B981),
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Horizontal Calendar Scrolling Date Strip (matching reference image)
        CalendarDateStrip(
            dates = dates,
            selectedDate = selectedDate,
            today = today,
            datesWithPayments = datesWithPayments,
            onSelectDate = { date -> selectedDate = date }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Date Summary Row: Displays active date, txn count, and day's total collection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dateLabel = if (selectedDate != null) {
                if (selectedDate == today) {
                    "Today, ${selectedDate?.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
                } else {
                    selectedDate?.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")) ?: ""
                }
            } else {
                "All Transactions"
            }

            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                val totalPaise = filteredPayments.sumOf { it.amountPaise }
                Text(
                    text = "${filteredPayments.size} txns • ${AmountParser.formatPaise(totalPaise)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981)
                )

                if (selectedDate != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Show All",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedDate = null }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 5. Payment Records List
        if (filteredPayments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) Color(0xFF131D17) else Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (selectedDate != null) {
                            if (selectedDate == today) "No payments received today yet"
                            else "No payments on ${selectedDate?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))}"
                        } else {
                            "No payments found"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Check the spelling or clear the search query"
                        else if (selectedDate != null) "Select another date above or tap 'View All'"
                        else "Incoming UPI payments will appear here in real time",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                    if (selectedDate != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "View All Transactions",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedDate = null }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedDate != null) {
                    items(filteredPayments, key = { it.id }) { payment ->
                        PaymentHistoryCard(
                            payment = payment,
                            onReplay = {
                                announcementQueue.enqueue(
                                    AnnouncementRequest.Payment(payment.toEvent())
                                )
                                Toast.makeText(context, "Replaying announcement...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                } else {
                    groupedPayments.forEach { (dateHeader, paymentsInDay) ->
                        item(key = "header_$dateHeader") {
                            val dayTotal = paymentsInDay.sumOf { it.amountPaise }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${paymentsInDay.size} txns • ${AmountParser.formatPaise(dayTotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        items(paymentsInDay, key = { it.id }) { payment ->
                            PaymentHistoryCard(
                                payment = payment,
                                onReplay = {
                                    announcementQueue.enqueue(
                                        AnnouncementRequest.Payment(payment.toEvent())
                                    )
                                    Toast.makeText(context, "Replaying announcement...", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Horizontal calendar scrolling strip matching the reference image.
 * Displays: M T W T F S S with day-of-month below, active squircle container, and coral dot indicator.
 */
@Composable
private fun CalendarDateStrip(
    dates: List<LocalDate>,
    selectedDate: LocalDate?,
    today: LocalDate,
    datesWithPayments: Set<LocalDate>,
    onSelectDate: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundboxColors.current
    val listState = rememberLazyListState()

    // Scroll to today / selected date initially
    LaunchedEffect(Unit) {
        val targetDate = selectedDate ?: today
        val targetIndex = dates.indexOf(targetDate)
        if (targetIndex >= 0) {
            // Index 0 is "ALL", so the day is at targetIndex + 1
            listState.scrollToItem((targetIndex + 1).coerceAtLeast(0))
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (colors.isDark) Color(0xFF0F1712) else Color(0xFFFFFFFF)
        ),
        border = BorderStroke(
            1.dp,
            if (colors.isDark) Color(0xFF1E2D24) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "ALL" Option Pill at index 0
            item {
                val isAllSelected = (selectedDate == null)
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isAllSelected) {
                                if (colors.isDark) Color(0xFF1E3A2B) else Color(0xFFDCFCE7)
                            } else {
                                Color.Transparent
                            }
                        )
                        .then(
                            if (isAllSelected) {
                                Modifier.border(
                                    BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                                    RoundedCornerShape(14.dp)
                                )
                            } else Modifier
                        )
                        .clickable { onSelectDate(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ALL",
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (isAllSelected) {
                                if (colors.isDark) Color.White else Color(0xFF065F46)
                            } else {
                                colors.textMuted
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "DAYS",
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (isAllSelected) {
                                if (colors.isDark) Color.White else Color(0xFF065F46)
                            } else {
                                colors.textMuted
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isAllSelected) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(Color(0xFFEF4444), CircleShape)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(5.dp))
                        }
                    }
                }
            }

            // Days items (M 21, T 22, W 23, T 24, F 25, S 26, S 27...)
            items(dates) { date ->
                val isSelected = (selectedDate == date)
                val isToday = (date == today)
                val isFuture = date.isAfter(today)
                val hasPayments = datesWithPayments.contains(date)
                val dayLetter = when (date.dayOfWeek) {
                    DayOfWeek.MONDAY -> "M"
                    DayOfWeek.TUESDAY -> "T"
                    DayOfWeek.WEDNESDAY -> "W"
                    DayOfWeek.THURSDAY -> "T"
                    DayOfWeek.FRIDAY -> "F"
                    DayOfWeek.SATURDAY -> "S"
                    DayOfWeek.SUNDAY -> "S"
                }

                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) {
                                if (colors.isDark) Color(0xFF1E3A2B) else Color(0xFFDCFCE7)
                            } else {
                                Color.Transparent
                            }
                        )
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                                    RoundedCornerShape(14.dp)
                                )
                            } else Modifier
                        )
                        .clickable(enabled = !isFuture) {
                            if (isSelected) {
                                onSelectDate(null) // Toggle back to all
                            } else {
                                onSelectDate(date)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Weekday single letter: M, T, W, T, F, S, S
                        Text(
                            text = dayLetter,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = when {
                                isSelected -> if (colors.isDark) Color.White else Color(0xFF065F46)
                                isFuture -> colors.textMuted.copy(alpha = 0.35f)
                                else -> colors.textMuted
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // Day of month number
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp,
                            color = when {
                                isSelected -> if (colors.isDark) Color.White else Color(0xFF065F46)
                                isToday -> Color(0xFF10B981)
                                isFuture -> colors.textMuted.copy(alpha = 0.35f)
                                else -> colors.textPrimary
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Dot indicator: coral/red for selected, green for payments, spacer for empty
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(Color(0xFFEF4444), CircleShape)
                            )
                        } else if (hasPayments) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(5.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryCard(
    payment: PaymentEntity,
    onReplay: () -> Unit
) {
    val colors = LocalSoundboxColors.current
    val appName = AnnouncementEngine.mapPackageToDisplayName(payment.appPackage)
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(payment.receivedAt))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val logoRes = AppLogoProvider.getLogoForPackage(payment.appPackage)
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = appName,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = payment.payer ?: "Customer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                    if (!payment.upiRef.isNullOrBlank()) {
                        Text(
                            text = " • Ref: ${payment.upiRef}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                AmountDisplay(amountPaise = payment.amountPaise, size = AmountSize.Large)

                IconButton(
                    onClick = onReplay,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Replay voice",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private suspend fun writePaymentsToCsv(context: Context, uri: Uri, payments: List<PaymentEntity>) {
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    // CSV Header
                    writer.write("ID,Date,Time,Amount_INR,Payer,UPI_Ref,App,Source\n")

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

                    for (p in payments) {
                        val d = Date(p.receivedAt)
                        val amountInr = p.amountPaise / 100.0
                        val payerClean = (p.payer ?: "").replace(",", " ")
                        val refClean = (p.upiRef ?: "").replace(",", " ")
                        val appClean = AnnouncementEngine.mapPackageToDisplayName(p.appPackage)

                        writer.write("${p.id},${dateFormat.format(d)},${timeFormat.format(d)},$amountInr,$payerClean,$refClean,$appClean,${p.source}\n")
                    }
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "CSV Exported successfully!", Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
