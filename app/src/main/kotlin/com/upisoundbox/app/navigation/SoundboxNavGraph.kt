package com.upisoundbox.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.data.repository.PaymentRepository
import com.upisoundbox.core.tts.AnnouncementEngine
import com.upisoundbox.core.tts.AnnouncementQueue
import com.upisoundbox.feature.diagnostics.DiagnosticsScreen
import com.upisoundbox.feature.help.HelpScreen
import com.upisoundbox.feature.history.HistoryScreen
import com.upisoundbox.feature.home.CounterModeScreen
import com.upisoundbox.feature.home.HomeScreen
import com.upisoundbox.feature.onboarding.OnboardingScreen
import com.upisoundbox.feature.onboarding.SetupWizardScreen
import com.upisoundbox.feature.onboarding.SplashScreen
import com.upisoundbox.feature.qr.MyQrScreen
import com.upisoundbox.feature.reports.ReportsScreen
import com.upisoundbox.feature.settings.SettingsScreen
import com.upisoundbox.feature.settings.UpiAppsScreen
import com.upisoundbox.feature.settings.VoiceSettingsScreen

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object SetupWizard : Screen("setup_wizard")
    data object Home : Screen("home")
    data object CounterMode : Screen("counter_mode")
    data object History : Screen("history")
    data object Reports : Screen("reports")
    data object VoiceSettings : Screen("voice_settings")
    data object UpiApps : Screen("upi_apps")
    data object Settings : Screen("settings")
    data object MyQR : Screen("my_qr")
    data object Help : Screen("help")
    data object Diagnostics : Screen("diagnostics")
}

@Composable
fun SoundboxNavGraph(
    navController: NavHostController = rememberNavController(),
    repository: PaymentRepository,
    preferences: SoundboxPreferences,
    announcementQueue: AnnouncementQueue,
    announcementEngine: AnnouncementEngine,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                preferences = preferences,
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.SetupWizard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.SetupWizard.route) {
            SetupWizardScreen(
                preferences = preferences,
                announcementQueue = announcementQueue,
                announcementEngine = announcementEngine,
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SetupWizard.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                repository = repository,
                preferences = preferences,
                announcementQueue = announcementQueue,
                onNavigateCounter = { navController.navigate(Screen.CounterMode.route) },
                onNavigateHistory = { navController.navigate(Screen.History.route) },
                onNavigateReports = { navController.navigate(Screen.Reports.route) },
                onNavigateQr = { navController.navigate(Screen.MyQR.route) },
                onNavigateSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.CounterMode.route) {
            CounterModeScreen(
                repository = repository,
                onClose = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                repository = repository,
                announcementQueue = announcementQueue,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Reports.route) {
            ReportsScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.VoiceSettings.route) {
            VoiceSettingsScreen(
                preferences = preferences,
                announcementQueue = announcementQueue,
                announcementEngine = announcementEngine,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.UpiApps.route) {
            UpiAppsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                preferences = preferences,
                onNavigateBack = { navController.popBackStack() },
                onNavigateVoiceSettings = { navController.navigate(Screen.VoiceSettings.route) },
                onNavigateUpiApps = { navController.navigate(Screen.UpiApps.route) },
                onNavigateHelp = { navController.navigate(Screen.Help.route) },
                onNavigateDiagnostics = { navController.navigate(Screen.Diagnostics.route) }
            )
        }
        composable(Screen.MyQR.route) {
            MyQrScreen(
                preferences = preferences,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Help.route) {
            HelpScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(
                repository = repository,
                preferences = preferences,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
