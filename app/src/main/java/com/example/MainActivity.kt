package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.config.SamiOwnerBuildConfig
import com.example.ui.SamiViewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.MediaLabScreen
import com.example.ui.screens.OwnerManagementScreen
import com.example.ui.screens.ReelsStudioScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SamiKingTheme

const val ROUTE_CHAT = "chat"
const val ROUTE_REELS_STUDIO = "reels_studio"
const val ROUTE_MEDIA_LAB = "media_lab"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_OWNER_PANEL = "owner_panel"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val viewModel: SamiViewModel = viewModel()
            val config by viewModel.configState.collectAsStateWithLifecycle()

            CompositionLocalProvider(
                LocalLayoutDirection provides if (config.isPersianUi) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                SamiKingTheme(themePreference = config.themePreference) {
                    SamiKingAppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}

@Composable
private fun SamiKingAppNavigation(viewModel: SamiViewModel) {
    val navController = rememberNavController()
    val config by viewModel.configState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = ROUTE_CHAT
    ) {
        composable(ROUTE_CHAT) {
            ChatScreen(
                viewModel = viewModel,
                onOpenReelsStudio = { navController.navigate(ROUTE_REELS_STUDIO) },
                onOpenMediaLab = { navController.navigate(ROUTE_MEDIA_LAB) },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                onOpenOwnerPanel = {
                    if (SamiOwnerBuildConfig.ENABLE_OWNER_MANAGEMENT && config.isOwnerUnlocked) {
                        navController.navigate(ROUTE_OWNER_PANEL)
                    }
                }
            )
        }
        composable(ROUTE_REELS_STUDIO) {
            ReelsStudioScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(ROUTE_MEDIA_LAB) {
            MediaLabScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onContinueInChat = {
                    navController.popBackStack(ROUTE_CHAT, inclusive = false)
                }
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenOwnerPanel = {
                    if (SamiOwnerBuildConfig.ENABLE_OWNER_MANAGEMENT && config.isOwnerUnlocked) {
                        navController.navigate(ROUTE_OWNER_PANEL)
                    }
                }
            )
        }
        composable(ROUTE_OWNER_PANEL) {
            if (SamiOwnerBuildConfig.ENABLE_OWNER_MANAGEMENT && config.isOwnerUnlocked) {
                OwnerManagementScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onLockedExit = {
                        navController.popBackStack(ROUTE_CHAT, inclusive = false)
                    }
                )
            }
        }
    }
}
