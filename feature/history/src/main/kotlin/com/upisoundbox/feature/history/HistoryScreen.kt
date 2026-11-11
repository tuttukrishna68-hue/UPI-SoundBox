package com.upisoundbox.feature.history

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upisoundbox.core.data.db.PaymentEntity
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.design.component.AmountDisplay
import com.upisoundbox.core.design.component.AmountSize
import com.upisoundbox.core.design.component.AppChip
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.tts.AnnouncementEngine
import com.upisoundbox.core.tts.AnnouncementQueue
import com.upisoundbox.core.tts.AnnouncementRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    repository: PaymentRepository,
    announcementQueue: AnnouncementQueue,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedAppFilter by remember { mutableStateOf<String?>(null) }

    val allPayments by repository.getAllPayments().collectAsState(initial = emptyList())

    // Filter payments in memory for instant responsive UI
    val filteredPayments = remember(allPayments, searchQuery, selectedAppFilter) {
        allPayments.filter { payment ->
            val matchesQuery = searchQuery.isBlank() ||
                    (payment.payer?.contains(searchQuery, ignoreCase = true) == true) ||
                    (payment.upiRef?.contains(searchQuery, ignoreCase = true) == true)

            val matchesApp = selectedAppFilter == null ||
                    AnnouncementEngine.mapPackageToDisplayName(payment.appPackage) == selectedAppFilter

            matchesQuery && matchesApp
        }
    }

    // Group by day string
    val groupedPayments = remember(filteredPayments) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        filteredPayments.groupBy { payment ->
            val date = Date(payment.receivedAt)
            if (DateUtils.isToday(payment.receivedAt)) {
                "Today"
            } else {
                dateFormat.format(date)
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

    val availableApps = listOf("Google Pay", "PhonePe", "Paytm", "BHIM", "Amazon Pay", "CRED", "Bank SMS")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
    ) {
        // App Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.ink
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Payment History",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink
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
                    tint = colors.accentOrange
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by payer or UPI Ref...", color = colors.muted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = colors.muted)
            },
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                unfocusedBorderColor = colors.bubbleGrey,
                focusedBorderColor = colors.accentOrange
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // App Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedAppFilter == null,
                    onClick = { selectedAppFilter = null },
                    label = { Text("All Apps") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.ink,
                        selectedLabelColor = colors.paper
                    )
                )
            }
            items(availableApps) { appName ->
                FilterChip(
                    selected = selectedAppFilter == appName,
                    onClick = {
                        selectedAppFilter = if (selectedAppFilter == appName) null else appName
                    },
                    label = { Text(appName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.accentOrange,
                        selectedLabelColor = colors.paper
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Payments List grouped by date
        if (groupedPayments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No payments found matching your filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                groupedPayments.forEach { (dateHeader, paymentsInDay) ->
                    item {
                        val dayTotal = paymentsInDay.sumOf { it.amountPaise }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.ink
                            )
                            Text(
                                text = "${paymentsInDay.size} txns • ${AmountParser.formatPaise(dayTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.muted
                            )
                        }
                    }

                    items(paymentsInDay) { payment ->
                        PaymentHistoryCard(
                            payment = payment,
                            onReplay = {
                                announcementQueue.enqueue(
                                    AnnouncementRequest.Payment(payment.toEvent())
                                )
                                Toast.makeText(context, "Replaying announcement...", Toast.LENGTH_SHORT).show()
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = payment.payer ?: "Customer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AppChip(appName = appName)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                    if (!payment.upiRef.isNullOrBlank()) {
                        Text(
                            text = " • Ref: ${payment.upiRef}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted
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
                        tint = colors.accentOrange,
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
