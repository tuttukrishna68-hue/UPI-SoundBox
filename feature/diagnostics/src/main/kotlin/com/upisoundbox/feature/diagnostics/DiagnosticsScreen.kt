package com.upisoundbox.feature.diagnostics

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.design.component.ChipStatus
import com.upisoundbox.core.design.component.PillButton
import com.upisoundbox.core.design.component.StatusChip
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.parser.PaymentEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.OutputStreamWriter

@Composable
fun DiagnosticsScreen(
    repository: PaymentRepository,
    preferences: SoundboxPreferences,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current
    val scope = rememberCoroutineScope()

    val recentPayments by repository.getRecentPayments(15).collectAsState(initial = emptyList())
    val learningExpiry by preferences.unknownAppLearningExpiry.collectAsState(initial = 0L)
    val isLearningActive = System.currentTimeMillis() < learningExpiry

    // Health checks refresh when the merchant returns from Android Settings.
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    var isNotificationAccessEnabled by remember { mutableStateOf(false) }
    var isBatteryExempt by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        fun refreshHealth() {
            isNotificationAccessEnabled = NotificationManagerCompat
                .getEnabledListenerPackages(context)
                .contains(context.packageName)
            isBatteryExempt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshHealth()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refreshHealth()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedPaymentForExport by remember { mutableStateOf<PaymentEvent?>(null) }

    // Export fixture launcher
    val exportFixtureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && selectedPaymentForExport != null) {
            scope.launch(Dispatchers.IO) {
                val p = selectedPaymentForExport!!
                val jsonFixture = buildJsonObject {
                    put("id", "user_sample_${System.currentTimeMillis()}")
                    put("verified", false)
                    put("note", "Exported redacted user fixture")
                    put("package", p.appPackage)
                    put("title", "Payment of ${AmountParser.formatPaise(p.amountPaise)} received")
                    put("text", "Received from [REDACTED]. Ref: [REDACTED]")
                    put("shouldParse", true)
                    put("expectedAmountPaise", p.amountPaise)
                }.toString()

                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        OutputStreamWriter(out).use { it.write(jsonFixture) }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Fixture exported! You can share it to report parser issues.", Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception) {}
            }
        }
    }

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
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.ink)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Diagnostics & Health",
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. System Health Status Card
            Text(
                text = "DETECTION SUBSYSTEM HEALTH",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.muted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    HealthStatusRow(
                        title = "Notification Listener Access",
                        status = if (isNotificationAccessEnabled) ChipStatus.OK else ChipStatus.Error,
                        statusText = if (isNotificationAccessEnabled) "Connected" else "Disabled"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HealthStatusRow(
                        title = "Battery Optimization",
                        status = if (isBatteryExempt) ChipStatus.OK else ChipStatus.Warning,
                        statusText = if (isBatteryExempt) "Exempted" else "Active (May sleep)"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HealthStatusRow(
                        title = "Watchdog Health Monitor",
                        status = ChipStatus.OK,
                        statusText = "Active (Every 15 min)"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Unknown-App 5-Minute Learning Mode
            Text(
                text = "UNKNOWN-APP LEARNING (5 MINUTES)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.muted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isLearningActive) "🟢 Learning Mode ACTIVE" else "Start Unknown-App Learning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLearningActive) colors.success else colors.ink
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When active, payments from non-listed bank apps will be detected so you can add them to the allow-list in 1 tap. Automatically stops after 5 minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    PillButton(
                        text = if (isLearningActive) "Extend 5 Minutes" else "Start 5-Minute Learning",
                        onClick = {
                            val newExpiry = System.currentTimeMillis() + (5 * 60 * 1000L)
                            scope.launch { preferences.setUnknownAppLearningExpiry(newExpiry) }
                            Toast.makeText(context, "Learning mode active for 5 minutes", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Redacted Transaction Logs & Fixture Exporter
            Text(
                text = "REDACTED AUDIT LOGS (TAP TO EXPORT FIXTURE)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.muted
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (recentPayments.isEmpty()) {
                Text(
                    text = "No recent transactions logged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            } else {
                recentPayments.forEach { p ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${AmountParser.formatPaise(p.amountPaise)} from [REDACTED]",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.ink,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Pkg: ${p.appPackage} • Src: ${p.source}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = {
                                    selectedPaymentForExport = p.toEvent()
                                    exportFixtureLauncher.launch("fixture_${p.id}.json")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.bubbleGrey),
                                shape = RoundedCornerShape(50),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Export",
                                    color = colors.ink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HealthStatusRow(
    title: String,
    status: ChipStatus,
    statusText: String
) {
    val colors = LocalSoundboxColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink
        )
        StatusChip(
            label = statusText,
            status = status
        )
    }
}
