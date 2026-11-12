package com.upisoundbox.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.design.R
import com.upisoundbox.core.design.component.ChipStatus
import com.upisoundbox.core.design.component.PillButton
import com.upisoundbox.core.design.component.StatusChip
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.tts.AnnouncementEngine
import com.upisoundbox.core.tts.AnnouncementQueue
import com.upisoundbox.core.tts.AnnouncementRequest
import kotlinx.coroutines.launch

@Composable
fun SetupWizardScreen(
    preferences: SoundboxPreferences,
    announcementQueue: AnnouncementQueue,
    announcementEngine: AnnouncementEngine,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Dynamic Permission & State tracking
    var hasNotificationAccess by remember { mutableStateOf(false) }
    var hasPostNotificationPermission by remember { mutableStateOf(false) }
    var hasBatteryExemption by remember { mutableStateOf(false) }
    var hasVoiceInstalled by remember { mutableStateOf(true) }
    var isTestSuccessful by remember { mutableStateOf(false) }

    var showRestrictedSettingsHelp by remember { mutableStateOf(false) }
    var showSmsDisclosureDialog by remember { mutableStateOf(false) }
    var showOemGuideDialog by remember { mutableStateOf(false) }

    val isSmsEnabled by preferences.isSmsSourceEnabled.collectAsState(initial = false)
    val languageTag by preferences.languageTag.collectAsState(initial = "en-IN")

    // Launcher for POST_NOTIFICATIONS
    val postNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPostNotificationPermission = granted
    }

    // Launcher for SMS permissions
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.RECEIVE_SMS] == true
        scope.launch {
            preferences.setSmsSourceEnabled(granted)
        }
    }

    fun refreshStates() {
        // 1. Notification access
        hasNotificationAccess = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

        // 2. Post notification permission
        hasPostNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        // 3. Battery exemption
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        hasBatteryExemption = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        // 4. Voice data
        scope.launch {
            hasVoiceInstalled = announcementEngine.isVoiceInstalled(languageTag)
        }
    }

    // Lifecycle observer to re-check states when returning from system Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        refreshStates()
    }

    val allEssentialReady = hasNotificationAccess && hasPostNotificationPermission && hasBatteryExemption

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Setup Soundbox",
            style = MaterialTheme.typography.displaySmall,
            color = colors.ink
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Follow these simple steps so your phone announces UPI payments without interruption.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Step 1: Notification Listener Access
        WizardItemCard(
            title = "1. Notification Access",
            description = "Allows detecting incoming payment alerts from allow-listed UPI apps.",
            isCompleted = hasNotificationAccess,
            buttonText = if (hasNotificationAccess) "Granted" else "Enable",
            onAction = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            },
            onHelp = { showRestrictedSettingsHelp = true },
            helpLabel = "Restricted settings help?"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Post Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            WizardItemCard(
                title = "2. Notification Permission",
                description = "Required to keep the Soundbox active in the background.",
                isCompleted = hasPostNotificationPermission,
                buttonText = if (hasPostNotificationPermission) "Granted" else "Allow",
                onAction = {
                    postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Step 3: Battery Optimization Exemption
        WizardItemCard(
            title = "3. Battery Optimization",
            description = "Exempt Soundbox so Android doesn't kill it during screen-off sleep.",
            isCompleted = hasBatteryExemption,
            buttonText = if (hasBatteryExemption) "Exempted" else "Fix",
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 4: OEM Background Guide
        WizardItemCard(
            title = "4. Autostart & OEM Settings",
            description = "Configure autostart on Xiaomi, Samsung, Vivo, Oppo, Realme devices.",
            isCompleted = false,
            buttonText = "Open Guide",
            onAction = { showOemGuideDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 5: TTS Voice Check
        WizardItemCard(
            title = "5. Speech Voice Data",
            description = "Verifies voice synthesis for your selected language ($languageTag).",
            isCompleted = hasVoiceInstalled,
            buttonText = if (hasVoiceInstalled) "Ready" else "Install Voice",
            onAction = {
                try {
                    context.startActivity(announcementEngine.getVoiceInstallIntent())
                } catch (_: Exception) {}
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 6: Optional SMS Detection
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "6. Bank SMS Detection (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.ink
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Backup detection for incoming bank credits when notifications are delayed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted
                        )
                    }
                    Switch(
                        checked = isSmsEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showSmsDisclosureDialog = true
                            } else {
                                scope.launch { preferences.setSmsSourceEnabled(false) }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.accentOrange)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Step 7: Test Payment Announcement
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.orangeTint.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔊 Test Announcement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.ink
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Test your volume and speech engine right now. Does not affect today's ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        announcementQueue.enqueue(AnnouncementRequest.Test())
                        isTestSuccessful = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentOrange),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(text = "Play Test Payment (₹100)")
                }
                if (isTestSuccessful) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ Announcement sent to queue",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.success
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Complete Button
        PillButton(
            text = "Complete Setup & Open Dashboard",
            onClick = {
                scope.launch {
                    preferences.setOnboardingCompleted(true)
                    onComplete()
                }
            },
            enabled = allEssentialReady,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Dialog: Android 13+ Restricted Settings Guide
    if (showRestrictedSettingsHelp) {
        AlertDialog(
            onDismissRequest = { showRestrictedSettingsHelp = false },
            title = {
                Text(
                    text = "Allowing Restricted Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink
                )
            },
            text = {
                Column {
                    Text(
                        text = "On Android 13 and newer, sideloaded apps have sensitive toggles greyed out by default.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "1. Open your phone's Settings > Apps > UPI Soundbox.\n" +
                                "2. Tap the three dots (⋮) in the top-right corner.\n" +
                                "3. Tap 'Allow restricted settings'.\n" +
                                "4. Authenticate with fingerprint or PIN.\n" +
                                "5. Return here and toggle Notification Access ON.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestrictedSettingsHelp = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open App Info", color = colors.accentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestrictedSettingsHelp = false }) {
                    Text("Dismiss", color = colors.muted)
                }
            }
        )
    }

    // Dialog: Plain-language SMS Privacy Disclosure
    if (showSmsDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showSmsDisclosureDialog = false },
            title = {
                Text(text = "SMS Credit Detection Privacy Disclosure", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column {
                    Text(
                        text = "• What we read: Incoming SMS from verified bank sender IDs (e.g. HDFCBK, SBIINB) matching incoming credit keywords only.\n\n" +
                                "• What we store: Only the payment amount, payer name/VPA and timestamp in your encrypted local database.\n\n" +
                                "• What never leaves your phone: EVERYTHING. This app has zero INTERNET permission. No SMS text is ever uploaded or shared.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSmsDisclosureDialog = false
                    smsPermissionLauncher.launch(
                        arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                    )
                }) {
                    Text("Agree & Enable", color = colors.accentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmsDisclosureDialog = false }) {
                    Text("Cancel", color = colors.muted)
                }
            }
        )
    }

    // Dialog: OEM Autostart Guide
    if (showOemGuideDialog) {
        AlertDialog(
            onDismissRequest = { showOemGuideDialog = false },
            title = {
                Text(text = "OEM Background Guides", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "• Xiaomi/Redmi/Poco (MIUI/HyperOS): Settings > Apps > Permissions > Autostart > Enable UPI Soundbox. Battery saver: No restrictions.\n\n" +
                                "• Samsung (OneUI): Settings > Battery and device care > Battery > Background usage limits > Never sleeping apps > Add UPI Soundbox.\n\n" +
                                "• Oppo/Realme (ColorOS): Settings > Battery > App Battery Management > Allow Auto-launch and background activity.\n\n" +
                                "• Vivo/iQOO (FuntouchOS): Settings > Battery > High background power consumption > Enable UPI Soundbox.\n\n" +
                                "• OnePlus (OxygenOS): Settings > Battery > More settings > Optimize battery use > Select 'Don't optimize'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showOemGuideDialog = false }) {
                    Text("Got it", color = colors.accentOrange)
                }
            }
        )
    }
}

@Composable
private fun WizardItemCard(
    title: String,
    description: String,
    isCompleted: Boolean,
    buttonText: String,
    onAction: () -> Unit,
    onHelp: (() -> Unit)? = null,
    helpLabel: String? = null
) {
    val colors = LocalSoundboxColors.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(
                    label = if (isCompleted) "Active" else "Pending",
                    status = if (isCompleted) ChipStatus.OK else ChipStatus.Warning
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onHelp != null && helpLabel != null) {
                    Text(
                        text = helpLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.accentOrange,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onHelp() }
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) colors.bubbleGrey else colors.ink,
                        contentColor = if (isCompleted) colors.ink else colors.paper
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
