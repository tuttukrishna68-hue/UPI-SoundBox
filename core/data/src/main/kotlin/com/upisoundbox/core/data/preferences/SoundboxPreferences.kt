package com.upisoundbox.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.soundboxDataStore: DataStore<Preferences> by preferencesDataStore(name = "soundbox_preferences")

class SoundboxPreferences(private val context: Context) {

    private val dataStore = context.soundboxDataStore

    // Soundbox State
    val isSoundboxActive: Flow<Boolean> = dataStore.data.map { it[KEY_SOUNDBOX_ACTIVE] ?: true }

    // TTS Voice & Audio Settings
    val languageTag: Flow<String> = dataStore.data.map { it[KEY_LANGUAGE_TAG] ?: "en-IN" }
    val phraseTemplate: Flow<String> = dataStore.data.map { preferences ->
        normalizeDefaultTemplate(preferences[KEY_PHRASE_TEMPLATE])
    }
    val repeatCount: Flow<Int> = dataStore.data.map { it[KEY_REPEAT_COUNT] ?: 1 }
    val isVolumeBoostEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_VOLUME_BOOST] ?: true }
    val isAlarmStreamEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_ALARM_STREAM] ?: false }
    val isChimeEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_CHIME_ENABLED] ?: true }
    val waitDuringCalls: Flow<Boolean> = dataStore.data.map { it[KEY_WAIT_CALLS] ?: true }

    // Quiet Hours
    val isQuietHoursEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_QUIET_HOURS_ENABLED] ?: false }
    val quietHoursStartHour: Flow<Int> = dataStore.data.map { it[KEY_QUIET_START_HOUR] ?: 22 }
    val quietHoursStartMinute: Flow<Int> = dataStore.data.map { it[KEY_QUIET_START_MIN] ?: 0 }
    val quietHoursEndHour: Flow<Int> = dataStore.data.map { it[KEY_QUIET_END_HOUR] ?: 7 }
    val quietHoursEndMinute: Flow<Int> = dataStore.data.map { it[KEY_QUIET_END_MIN] ?: 0 }

    // Minimum amount & Burst handling
    val minAmountPaise: Flow<Long> = dataStore.data.map { it[KEY_MIN_AMOUNT_PAISE] ?: 0L }
    val isBurstModeEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_BURST_MODE_ENABLED] ?: true }
    val burstThresholdCount: Flow<Int> = dataStore.data.map { it[KEY_BURST_THRESHOLD] ?: 3 }
    val burstWindowSeconds: Flow<Int> = dataStore.data.map { it[KEY_BURST_WINDOW] ?: 10 }

    // Sources
    val isSmsSourceEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_SMS_ENABLED] ?: false }
    val allowedSmsSenders: Flow<String> = dataStore.data.map { it[KEY_SMS_SENDERS] ?: DEFAULT_SMS_SENDERS }
    val unknownAppLearningExpiry: Flow<Long> = dataStore.data.map { it[KEY_UNKNOWN_APP_EXPIRY] ?: 0L }
    val disabledUpiPackages: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[KEY_DISABLED_UPI_PACKAGES]
            .orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()
    }

    // Security & Business
    val isAppLockEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_APP_LOCK] ?: false }
    val isSecureScreenEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_SECURE_SCREEN] ?: false }
    val myUpiId: Flow<String> = dataStore.data.map { it[KEY_MY_UPI_ID] ?: "" }
    val myShopName: Flow<String> = dataStore.data.map { it[KEY_MY_SHOP_NAME] ?: "" }
    val userName: Flow<String> = dataStore.data.map { it[KEY_USER_NAME] ?: DEFAULT_USER_NAME }
    val fontScale: Flow<String> = dataStore.data.map { it[KEY_FONT_SCALE] ?: "compact" }
    val lastSeenPaymentAt: Flow<Long> = dataStore.data.map { it[KEY_LAST_SEEN_PAYMENT_AT] ?: 0L }

    // Onboarding
    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun setFontScale(scale: String) {
        dataStore.edit { it[KEY_FONT_SCALE] = scale }
    }

    suspend fun setSoundboxActive(active: Boolean) {
        dataStore.edit { it[KEY_SOUNDBOX_ACTIVE] = active }
    }

    suspend fun setLanguageTag(tag: String) {
        dataStore.edit { it[KEY_LANGUAGE_TAG] = tag }
    }

    suspend fun setPhraseTemplate(template: String) {
        dataStore.edit { it[KEY_PHRASE_TEMPLATE] = template }
    }

    suspend fun setRepeatCount(count: Int) {
        dataStore.edit { it[KEY_REPEAT_COUNT] = count.coerceIn(1, 3) }
    }

    suspend fun setVolumeBoost(enabled: Boolean) {
        dataStore.edit { it[KEY_VOLUME_BOOST] = enabled }
    }

    suspend fun setAlarmStream(enabled: Boolean) {
        dataStore.edit { it[KEY_ALARM_STREAM] = enabled }
    }

    suspend fun setChimeEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_CHIME_ENABLED] = enabled }
    }

    suspend fun setQuietHours(enabled: Boolean, startH: Int, startM: Int, endH: Int, endM: Int) {
        dataStore.edit {
            it[KEY_QUIET_HOURS_ENABLED] = enabled
            it[KEY_QUIET_START_HOUR] = startH
            it[KEY_QUIET_START_MIN] = startM
            it[KEY_QUIET_END_HOUR] = endH
            it[KEY_QUIET_END_MIN] = endM
        }
    }

    suspend fun setMinAmountPaise(paise: Long) {
        dataStore.edit { it[KEY_MIN_AMOUNT_PAISE] = paise }
    }

    suspend fun setSmsSourceEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SMS_ENABLED] = enabled }
    }

    suspend fun setUnknownAppLearningExpiry(expiryMs: Long) {
        dataStore.edit { it[KEY_UNKNOWN_APP_EXPIRY] = expiryMs }
    }

    suspend fun setUpiAppEnabled(packageId: String, enabled: Boolean) {
        val normalizedPackageId = packageId.trim()
        if (normalizedPackageId.isEmpty()) return

        dataStore.edit { preferences ->
            val disabled = preferences[KEY_DISABLED_UPI_PACKAGES]
                .orEmpty()
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toMutableSet()

            if (enabled) {
                disabled.remove(normalizedPackageId)
            } else {
                disabled.add(normalizedPackageId)
            }

            if (disabled.isEmpty()) {
                preferences.remove(KEY_DISABLED_UPI_PACKAGES)
            } else {
                preferences[KEY_DISABLED_UPI_PACKAGES] = disabled.sorted().joinToString(",")
            }
        }
    }

    suspend fun setMyQrDetails(upiId: String, shopName: String) {
        dataStore.edit {
            it[KEY_MY_UPI_ID] = upiId
            it[KEY_MY_SHOP_NAME] = shopName
        }
    }

    suspend fun setSecureScreen(enabled: Boolean) {
        dataStore.edit { it[KEY_SECURE_SCREEN] = enabled }
    }

    suspend fun setAppLock(enabled: Boolean) {
        dataStore.edit { it[KEY_APP_LOCK] = enabled }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[KEY_USER_NAME] = name.trim().ifEmpty { DEFAULT_USER_NAME } }
    }

    suspend fun setLastSeenPaymentAt(timestamp: Long) {
        dataStore.edit { it[KEY_LAST_SEEN_PAYMENT_AT] = timestamp.coerceAtLeast(0L) }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = completed }
    }

    companion object {
        const val DEFAULT_USER_NAME = "Merchant"
        const val DEFAULT_TEMPLATE_EN = "{amount} received on {app}"
        const val DEFAULT_TEMPLATE_HI = "{app} पर {amount} प्राप्त हुए"
        const val DEFAULT_TEMPLATE_ML = "{app} വഴി {amount} ലഭിച്ചു"
        const val DEFAULT_TEMPLATE_TA = "{app} மூலம் {amount} பெறப்பட்டது"
        const val DEFAULT_TEMPLATE_TE = "{app} ద్వారా {amount} అందింది"
        const val DEFAULT_TEMPLATE_KN = "{app} ಮೂಲಕ {amount} ಸ್ವೀಕರಿಸಲಾಗಿದೆ"

        val DEFAULT_SMS_SENDERS = "HDFCBK,SBIINB,ICICIB,KOTAKB,AXISBK,PNBSMS,BOBTXN,UNIONB,CANBNK,PAYTMB"

        private val KEY_SOUNDBOX_ACTIVE = booleanPreferencesKey("soundbox_active")
        private val KEY_LANGUAGE_TAG = stringPreferencesKey("language_tag")
        private val KEY_PHRASE_TEMPLATE = stringPreferencesKey("phrase_template")
        private val KEY_REPEAT_COUNT = intPreferencesKey("repeat_count")
        private val KEY_VOLUME_BOOST = booleanPreferencesKey("volume_boost")
        private val KEY_ALARM_STREAM = booleanPreferencesKey("alarm_stream")
        private val KEY_CHIME_ENABLED = booleanPreferencesKey("chime_enabled")
        private val KEY_WAIT_CALLS = booleanPreferencesKey("wait_during_calls")

        private val KEY_QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        private val KEY_QUIET_START_HOUR = intPreferencesKey("quiet_start_hour")
        private val KEY_QUIET_START_MIN = intPreferencesKey("quiet_start_min")
        private val KEY_QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")
        private val KEY_QUIET_END_MIN = intPreferencesKey("quiet_end_min")

        private val KEY_MIN_AMOUNT_PAISE = longPreferencesKey("min_amount_paise")
        private val KEY_BURST_MODE_ENABLED = booleanPreferencesKey("burst_mode_enabled")
        private val KEY_BURST_THRESHOLD = intPreferencesKey("burst_threshold")
        private val KEY_BURST_WINDOW = intPreferencesKey("burst_window")

        private val KEY_SMS_ENABLED = booleanPreferencesKey("sms_source_enabled")
        private val KEY_SMS_SENDERS = stringPreferencesKey("allowed_sms_senders")
        private val KEY_UNKNOWN_APP_EXPIRY = longPreferencesKey("unknown_app_expiry")
        private val KEY_DISABLED_UPI_PACKAGES = stringPreferencesKey("disabled_upi_packages")

        private val KEY_APP_LOCK = booleanPreferencesKey("app_lock")
        private val KEY_SECURE_SCREEN = booleanPreferencesKey("secure_screen")
        private val KEY_MY_UPI_ID = stringPreferencesKey("my_upi_id")
        private val KEY_MY_SHOP_NAME = stringPreferencesKey("my_shop_name")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_FONT_SCALE = stringPreferencesKey("font_scale")
        private val KEY_LAST_SEEN_PAYMENT_AT = longPreferencesKey("last_seen_payment_at")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

        private fun normalizeDefaultTemplate(template: String?): String {
            return when (template) {
                null,
                "Received {amount} on {app} from {payer}" -> DEFAULT_TEMPLATE_EN
                "{app} पर {payer} से {amount} प्राप्त हुए" -> DEFAULT_TEMPLATE_HI
                "{app} വഴി {payer} ൽ നിന്ന് {amount} ലഭിച്ചു" -> DEFAULT_TEMPLATE_ML
                "{app} மூலம் {payer} இடமிருந்து {amount} பெறப்பட்டது" -> DEFAULT_TEMPLATE_TA
                "{app} ద్వారా {payer} నుండి {amount} అందింది" -> DEFAULT_TEMPLATE_TE
                "{app} ಮೂಲಕ {payer} ರಿಂದ {amount} ಸ್ವೀಕರಿಸಲಾಗಿದೆ" -> DEFAULT_TEMPLATE_KN
                "{app} वर {payer} कडून {amount} प्राप्त झाले" -> "{app} वर {amount} प्राप्त झाले"
                "{app} এ {payer} এর থেকে {amount} পাওয়া গেছে" -> "{app} এ {amount} পাওয়া গেছে"
                "{app} પર {payer} તરફથી {amount} મળ્યા" -> "{app} પર {amount} મળ્યા"
                "{app} 'ਤੇ {payer} ਵੱਲੋਂ {amount} ਪ੍ਰਾਪਤ ਹੋਏ" -> "{app} 'ਤੇ {amount} ਪ੍ਰਾਪਤ ਹੋਏ"
                else -> template
            }
        }
    }
}

