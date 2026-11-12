package com.upisoundbox.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.design.component.PillButton
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import com.upisoundbox.core.tts.AnnouncementEngine
import com.upisoundbox.core.tts.AnnouncementQueue
import com.upisoundbox.core.tts.AnnouncementRequest
import kotlinx.coroutines.launch

data class LanguageOption(val tag: String, val name: String, val defaultTemplate: String)

val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("en-IN", "English (India)", SoundboxPreferences.DEFAULT_TEMPLATE_EN),
    LanguageOption("hi-IN", "Hindi (हिन्दी)", SoundboxPreferences.DEFAULT_TEMPLATE_HI),
    LanguageOption("ml-IN", "Malayalam (മലയാളം)", SoundboxPreferences.DEFAULT_TEMPLATE_ML),
    LanguageOption("ta-IN", "Tamil (தமிழ்)", SoundboxPreferences.DEFAULT_TEMPLATE_TA),
    LanguageOption("te-IN", "Telugu (తెలుగు)", SoundboxPreferences.DEFAULT_TEMPLATE_TE),
    LanguageOption("kn-IN", "Kannada (ಕನ್ನಡ)", SoundboxPreferences.DEFAULT_TEMPLATE_KN),
    LanguageOption("mr-IN", "Marathi (मराठी)", "{app} वर {payer} कडून {amount} प्राप्त झाले"),
    LanguageOption("bn-IN", "Bengali (বাংলা)", "{app} এ {payer} এর থেকে {amount} পাওয়া গেছে"),
    LanguageOption("gu-IN", "Gujarati (ગુજરાતી)", "{app} પર {payer} તરફથી {amount} મળ્યા"),
    LanguageOption("pa-IN", "Punjabi (ਪੰਜਾਬੀ)", "{app} 'ਤੇ {payer} ਵੱਲੋਂ {amount} ਪ੍ਰਾਪਤ ਹੋਏ")
)

@Composable
fun VoiceSettingsScreen(
    preferences: SoundboxPreferences,
    announcementQueue: AnnouncementQueue,
    announcementEngine: AnnouncementEngine,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current
    val scope = rememberCoroutineScope()

    val currentLangTag by preferences.languageTag.collectAsState(initial = "en-IN")
    val currentTemplate by preferences.phraseTemplate.collectAsState(initial = SoundboxPreferences.DEFAULT_TEMPLATE_EN)
    val repeatCount by preferences.repeatCount.collectAsState(initial = 1)
    val isVolumeBoost by preferences.isVolumeBoostEnabled.collectAsState(initial = true)
    val isAlarmStream by preferences.isAlarmStreamEnabled.collectAsState(initial = false)
    val isChimeEnabled by preferences.isChimeEnabled.collectAsState(initial = true)
    val waitDuringCalls by preferences.waitDuringCalls.collectAsState(initial = true)
    val isQuietHours by preferences.isQuietHoursEnabled.collectAsState(initial = false)
    val minPaise by preferences.minAmountPaise.collectAsState(initial = 0L)
    val burstMode by preferences.isBurstModeEnabled.collectAsState(initial = true)

    var editableTemplate by remember(currentTemplate) { mutableStateOf(currentTemplate) }
    var minAmountText by remember(minPaise) { mutableStateOf((minPaise / 100).toString()) }

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
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.ink)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Voice & Soundbox Settings",
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

            // 1. Language Selector
            Text(
                text = "ANNOUNCEMENT LANGUAGE (10 INDIAN LANGUAGES)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.muted
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(SUPPORTED_LANGUAGES) { lang ->
                    FilterChip(
                        selected = currentLangTag.equals(lang.tag, ignoreCase = true),
                        onClick = {
                            scope.launch {
                                preferences.setLanguageTag(lang.tag)
                                preferences.setPhraseTemplate(lang.defaultTemplate)
                                editableTemplate = lang.defaultTemplate
                            }
                        },
                        label = { Text(lang.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentOrange,
                            selectedLabelColor = colors.paper
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Editable Phrase Template with Live Preview
            Text(
                text = "PHRASE TEMPLATE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.muted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Placeholders: {amount}, {payer}, {app}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = editableTemplate,
                onValueChange = {
                    editableTemplate = it
                    scope.launch { preferences.setPhraseTemplate(it) }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.accentOrange,
                    unfocusedBorderColor = colors.bubbleGrey
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Live Preview Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.bubbleGrey.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "LIVE PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val previewText = announcementEngine.renderPhrase(
                        template = editableTemplate,
                        amountPaise = 25000L,
                        payer = "Rahul",
                        appPackage = "com.google.android.apps.nbu.paisa.user",
                        languageTag = currentLangTag
                    )
                    Text(
                        text = "\"$previewText\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Audio Controls
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Volume Boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Volume Boost", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                            Text(text = "Temporarily boosts volume during speech and restores it.", style = MaterialTheme.typography.bodySmall, color = colors.muted)
                        }
                        Switch(
                            checked = isVolumeBoost,
                            onCheckedChange = { scope.launch { preferences.setVolumeBoost(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.accentOrange)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Alarm Stream Mode (Hear in silent mode)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Alarm Stream Mode", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                            Text(text = "⚠️ Allows announcements even when phone is on silent mode.", style = MaterialTheme.typography.bodySmall, color = colors.muted)
                        }
                        Switch(
                            checked = isAlarmStream,
                            onCheckedChange = { scope.launch { preferences.setAlarmStream(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.accentOrange)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chime Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Play Chime Sound", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                            Text(text = "Plays a pleasant tone before announcing payment.", style = MaterialTheme.typography.bodySmall, color = colors.muted)
                        }
                        Switch(
                            checked = isChimeEnabled,
                            onCheckedChange = { scope.launch { preferences.setChimeEnabled(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.accentOrange)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Repeat Count (1 to 3 times)
                    Text(
                        text = "Repeat Announcement: $repeatCount time(s)",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink
                    )
                    Slider(
                        value = repeatCount.toFloat(),
                        onValueChange = { scope.launch { preferences.setRepeatCount(it.toInt()) } },
                        valueRange = 1f..3f,
                        steps = 1,
                        colors = SliderDefaults.colors(thumbColor = colors.accentOrange, activeTrackColor = colors.accentOrange)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Test Announcement Button
            PillButton(
                text = "🔊 Play Test Announcement",
                onClick = {
                    scope.launch {
                        val success = announcementQueue.enqueueAndAwait(
                            AnnouncementRequest.Test(
                                sampleAmountPaise = 50000L,
                                samplePayer = "Rahul Sharma",
                                languageTag = currentLangTag
                            )
                        )
                        val message = if (success) {
                            "📢 Test announcement played ($currentLangTag)"
                        } else {
                            "Audio unavailable. Check the phone's Text-to-Speech engine."
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
