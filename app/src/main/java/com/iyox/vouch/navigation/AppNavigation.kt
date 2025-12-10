package com.iyox.vouch.navigation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iyox.vouch.view.AddVouchScreen
import com.iyox.vouch.view.HomeScreen
import com.iyox.vouch.view.SettingsScreen

data class NavigationItem(
    val screen: Screen,
    val icon: ImageVector,
    val iconSelected: ImageVector
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val navigationItems = listOf(
        NavigationItem(Screen.Home, Icons.Outlined.Home, Icons.Filled.Home),
        NavigationItem(Screen.Settings, Icons.Outlined.Settings, Icons.Filled.Settings)
    )

    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    Scaffold(
        bottomBar = {
            ShortNavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navigationItems.forEach { item ->
                    ShortNavigationBarItem(
                        icon = {
                            val isSelected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                            val icon = if (isSelected) item.iconSelected else item.icon
                            Icon(imageVector = icon, contentDescription = item.screen.title)
                        },
                        label = { Text(item.screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(50)
                            }

                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                    inclusive = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        })
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAddVouch = {
                        navController.navigate(Screen.AddVouch.route)
                    }
                )
            }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.AddVouch.route) {
                AddVouchScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

