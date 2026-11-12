package com.upisoundbox.feature.settings

import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.design.util.AppLogoProvider
import com.upisoundbox.core.parser.AmountParser
import com.upisoundbox.core.parser.ParseResult
import com.upisoundbox.core.tts.AnnouncementEngine
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
    val testApps = listOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.google.android.apps.nbu.paisa.merchant" to "GPay Business",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.org.npci.upiapp" to "BHIM",
        "in.amazon.mShop.android.shopping" to "Amazon Pay"
    )
    var selectedTesterPkg by remember { mutableStateOf(testApps.first().first) }
    var testerInputText by remember { mutableStateOf("Rahul Sharma paid you ₹500.00\nUPI Ref: 412345678901") }
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
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "UPI Apps & Parser Tester",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                fontSize = 18.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Section 1: Allow-listed UPI Apps (Primary merchant focus)
            Text(
                text = "SUPPORTED UPI APPS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            appList.forEach { app ->
                val isInstalled = appInstalledMap[app.packageId] == true
                val isEnabled = appEnabledMap[app.packageId] ?: true
                val logoRes = AppLogoProvider.getLogoForPackage(app.packageId)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    border = BorderStroke(1.dp, colors.cardBorder),
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
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = logoRes),
                                contentDescription = app.displayName,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Single-line compact status badge with color dot
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (isInstalled) colors.pillBackground else colors.bubbleGrey
                                            )
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(
                                                        if (isInstalled) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                        CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isInstalled) "Installed" else "Not detected",
                                                color = if (isInstalled) colors.pillText else colors.textMuted,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = app.packageId,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { appEnabledMap[app.packageId] = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = colors.cardBorder
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Real-time Parser Tester
            Text(
                text = "NOTIFICATION PARSER TESTER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Test Notification Text",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select an app and paste or click a sample notification to test offline extraction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // App Selector FilterChips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(testApps) { (pkg, label) ->
                            val isSelected = (selectedTesterPkg == pkg)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTesterPkg = pkg
                                    testResult = null
                                    testerInputText = when (pkg) {
                                        "com.google.android.apps.nbu.paisa.user" -> "Rahul Sharma paid you ₹500.00\nUPI Ref: 412345678901"
                                        "com.google.android.apps.nbu.paisa.merchant" -> "Payment received: ₹2,500.00 from Ramesh via Google Pay for Business"
                                        "com.phonepe.app" -> "Payment received of ₹120 from Priya Verma"
                                        "net.one97.paytm" -> "Received ₹500 from Sunita Sharma via Paytm UPI"
                                        "in.org.npci.upiapp" -> "Credited with ₹300.00 from anand@oksbi UPI Ref: 309876543210"
                                        else -> "Received ₹450 on Amazon Pay from Amit Kumar"
                                    }
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF10B981),
                                    selectedLabelColor = Color.White,
                                    containerColor = colors.pillBackground,
                                    labelColor = colors.textMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) Color(0xFF10B981) else colors.cardBorder,
                                    selectedBorderColor = Color(0xFF10B981),
                                    enabled = true,
                                    selected = isSelected
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = testerInputText,
                        onValueChange = { testerInputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.cardBackground,
                            unfocusedContainerColor = colors.cardBackground,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = colors.cardBorder,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val lines = testerInputText.split("\n", limit = 2)
                            val title = if (lines.size > 1) lines[0].trim() else null
                            val text = if (lines.size > 1) lines[1].trim() else testerInputText.trim()
                            val dummyNotification = RawNotification(
                                key = "tester_1",
                                packageName = selectedTesterPkg,
                                postTime = System.currentTimeMillis(),
                                title = title,
                                text = text,
                                bigText = testerInputText
                            )
                            testResult = parserRouter.parse(dummyNotification)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Test",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Parse Notification Text", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (testResult != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        when (val r = testResult!!) {
                            is ParseResult.Success -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.pillBackground)
                                        .padding(14.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = colors.pillText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Parsed Successfully",
                                                color = colors.pillText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Amount: ${AmountParser.formatPaise(r.event.amountPaise)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = colors.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Payer: ${r.event.payer ?: "Customer"}",
                                            fontSize = 13.sp,
                                            color = colors.textMuted
                                        )
                                        Text(
                                            text = "UPI Ref: ${r.event.upiRef ?: "N/A"}",
                                            fontSize = 13.sp,
                                            color = colors.textMuted
                                        )
                                        Text(
                                            text = "App: ${AnnouncementEngine.mapPackageToDisplayName(r.event.appPackage)}",
                                            fontSize = 13.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }
                            is ParseResult.Rejected -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.cardBackground)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "✗ Rejected: ${r.reason}",
                                        color = colors.error,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            is ParseResult.Ignored -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.cardBackground)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Ignored: ${r.reason}",
                                        color = colors.textMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
