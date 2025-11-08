package com.iyox.vouch.navigation

import androidx.compose.animation.core.tween
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

    val navigationItems = listOf(
        NavigationItem(Screen.Home, Icons.Outlined.Home, Icons.Filled.Home),
        NavigationItem(Screen.Settings, Icons.Outlined.Settings, Icons.Filled.Settings)
    )

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
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
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
            enterTransition = { fadeIn(animationSpec = tween(durationMillis = 0)) },
            exitTransition = { fadeOut(animationSpec = tween(durationMillis = 0)) },
            popEnterTransition = { fadeIn(animationSpec = tween(durationMillis = 0)) },
            popExitTransition = { fadeOut(animationSpec = tween(durationMillis = 0)) }
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

