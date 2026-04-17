package com.iyox.vouch.navigation


import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iyox.vouch.view.HomeScreen
import com.iyox.vouch.view.PersonScreen
import com.iyox.vouch.view.PersonsScreen
import com.iyox.vouch.view.SettingsScreen
import com.iyox.vouch.view.VouchScreen
import com.iyox.vouch.viewmodel.HomeViewModel
import com.iyox.vouch.viewmodel.PersonsViewModel
import com.iyox.vouch.viewmodel.SettingsViewModel

data class NavigationItem(
    val screen: Screen, val icon: ImageVector, val iconSelected: ImageVector
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val haptic = LocalHapticFeedback.current

    val navigationItems = listOf(
        NavigationItem(Screen.Vouches, Icons.Outlined.Payments, Icons.Filled.Payments),
        NavigationItem(Screen.Persons, Icons.Outlined.Group, Icons.Filled.Group),
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
                            val isSelected =
                                currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                            val icon = if (isSelected) item.iconSelected else item.icon
                            Icon(imageVector = icon, contentDescription = item.screen.title)
                        },
                        label = { Text(item.screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
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
        }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Vouches.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }) {
            composable(Screen.Vouches.route) { backStackEntry ->
                val homeViewModel: HomeViewModel = viewModel(viewModelStoreOwner = backStackEntry)
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAddVouch = {
                        navController.navigate(Screen.AddVouch.route)
                    },
                    onNavigateToEditVouch = { vouchId ->
                        navController.navigate(Screen.EditVouch.createRoute(vouchId))
                    }
                )
            }
            composable(Screen.Persons.route) { backStackEntry ->
                val personsViewModel: PersonsViewModel =
                    viewModel(viewModelStoreOwner = backStackEntry)
                PersonsScreen(viewModel = personsViewModel, onNavigateToViewPerson = { personName ->
                    navController.navigate(Screen.ViewPerson.createRoute(personName))
                })
            }
            composable(Screen.Settings.route) { backStackEntry ->
                val settingsViewModel: SettingsViewModel =
                    viewModel(viewModelStoreOwner = backStackEntry)
                SettingsScreen(viewModel = settingsViewModel)
            }
            composable(Screen.AddVouch.route) {
                VouchScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    })
            }
            composable(
                route = Screen.ViewPerson.route,
                arguments = listOf(navArgument("personName") { type = NavType.StringType })
            ) { backStackEntry ->
                val personName =
                    backStackEntry.arguments?.getString("personName") ?: return@composable
                PersonScreen(
                    personName = personName,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToEditVouch = { vouchId ->
                        navController.navigate(Screen.EditVouch.createRoute(vouchId))
                    }
                )
            }
            composable(
                route = Screen.EditVouch.route,
                arguments = listOf(navArgument("vouchId") { type = NavType.IntType })
            ) { backStackEntry ->
                val vouchId = backStackEntry.arguments?.getInt("vouchId") ?: return@composable
                VouchScreen(
                    vouchId = vouchId.toLong(),
                    edit = true,
                    onNavigateBack = {
                        navController.popBackStack()
                    })
            }
        }
    }
}
