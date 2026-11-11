package com.upisoundbox.feature.settings

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.design.component.AmountDisplay
import com.upisoundbox.core.design.component.AmountSize
import com.upisoundbox.core.design.component.AppChip
import com.upisoundbox.core.design.component.ChipStatus
import com.upisoundbox.core.design.component.StatusChip
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.parser.ParseResult
import com.upisoundbox.core.parser.ParserRouter
import com.upisoundbox.core.parser.RawNotification
import com.upisoundbox.core.parser.UpiAppConfig
import kotlinx.serialization.json.Json

@Composable
fun UpiAppsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current
    val packageManager = context.packageManager

    var appList by remember { mutableStateOf<List<UpiAppConfig>>(emptyList()) }
    val appEnabledMap = remember { mutableStateMapOf<String, Boolean>() }
    val appInstalledMap = remember { mutableStateMapOf<String, Boolean>() }

    // Parser Tester State
    var testerInputText by remember { mutableStateOf("Received ₹500 from Rahul Sharma via Google Pay. UPI Ref: 412345678901") }
    var testResult by remember { mutableStateOf<ParseResult?>(null) }
    val parserRouter = remember { ParserRouter() }

    LaunchedEffect(Unit) {
        try {
            val jsonString = context.assets.open("upi_apps.json").bufferedReader().use { it.readText() }
            val configs = Json.decodeFromString<List<UpiAppConfig>>(jsonString)
            appList = configs
            configs.forEach { app ->
                appEnabledMap[app.packageId] = app.enabled
                appInstalledMap[app.packageId] = try {
                    packageManager.getPackageInfo(app.packageId, 0)
                    true
                } catch (_: Exception) {
                    false
                }
            }
        } catch (_: Exception) {}
    }

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.ink)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "UPI Apps & Parser Tester",
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
            Spacer(modifier = Modifier.height(8.dp))

            // Section 1: Live Parser Tester
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.orangeTint.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🔬 Live Parser Tester",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Paste any notification or SMS sample to test detection and amount extraction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = testerInputText,
                        onValueChange = { testerInputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.accentOrange,
                            unfocusedBorderColor = colors.bubbleGrey
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val dummyNotification = RawNotification(
                                key = "tester_1",
                                packageName = "com.google.android.apps.nbu.paisa.user",
                                postTime = System.currentTimeMillis(),
                                text = testerInputText
                            )
                            testResult = parserRouter.parse(dummyNotification)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentOrange),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Parse Text")
                    }

                    if (testResult != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        when (val r = testResult!!) {
                            is ParseResult.Success -> {
                                Text(
                                    text = "✓ Parsed Successfully!",
                                    color = colors.success,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = "Amount: ${AmountParser.formatPaise(r.event.amountPaise)}", color = colors.ink)
                                Text(text = "Payer: ${r.event.payer ?: "N/A"}", color = colors.ink)
                                Text(text = "UPI Ref: ${r.event.upiRef ?: "N/A"}", color = colors.ink)
                            }
                            is ParseResult.Rejected -> {
                                Text(text = "✗ Rejected: ${r.reason}", color = colors.error, fontWeight = FontWeight.Bold)
                            }
                            is ParseResult.Ignored -> {
                                Text(text = "Ignored: ${r.reason}", color = colors.muted)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Allow-listed UPI Apps
            Text(
                text = "SUPPORTED UPI APPS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.muted
            )
            Spacer(modifier = Modifier.height(12.dp))

            appList.forEach { app ->
                val isInstalled = appInstalledMap[app.packageId] == true
                val isEnabled = appEnabledMap[app.packageId] ?: true

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppChip(appName = app.displayName)
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusChip(
                                    label = if (isInstalled) "Installed" else "Not detected",
                                    status = if (isInstalled) ChipStatus.OK else ChipStatus.Warning
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = app.packageId,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { appEnabledMap[app.packageId] = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.accentOrange)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
