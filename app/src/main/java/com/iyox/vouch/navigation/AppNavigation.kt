package com.iyox.vouch.navigation


import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iyox.vouch.feature.onboarding.OnboardingScreen
import com.iyox.vouch.feature.person.PersonScreen
import com.iyox.vouch.feature.persons.PersonsScreen
import com.iyox.vouch.feature.persons.PersonsViewModel
import com.iyox.vouch.feature.settings.SettingsScreen
import com.iyox.vouch.feature.settings.SettingsViewModel
import com.iyox.vouch.feature.settled.SettledSpaceScreen
import com.iyox.vouch.feature.vouch.GroupVouchScreen
import com.iyox.vouch.feature.vouch.SingleVouchScreen
import com.iyox.vouch.feature.vouches.VouchesScreen
import com.iyox.vouch.feature.vouches.VouchesViewModel

data class NavigationItem(
    val screen: Screen, val icon: ImageVector, val iconSelected: ImageVector
)

@Composable
fun AppNavigation(
    snackbarHost: @Composable (() -> Unit) = {},
    settingsViewModel: SettingsViewModel = viewModel(),
    vouchesViewModel: VouchesViewModel = viewModel(),
    personsViewModel: PersonsViewModel = viewModel()
) {
    val navController = rememberNavController()
    val haptic = LocalHapticFeedback.current
    val uiState by settingsViewModel.uiState.collectAsState()

    val navigationItems = listOf(
        NavigationItem(Screen.Vouches, Icons.Outlined.Payments, Icons.Filled.Payments),
        NavigationItem(Screen.Persons, Icons.Outlined.Group, Icons.Filled.Group),
        NavigationItem(Screen.Settings, Icons.Outlined.Settings, Icons.Filled.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isNotOnboarding = currentRoute != Screen.Onboarding.route

    val startDestination = remember {
        if (settingsViewModel.isOnboardingCompleted()) Screen.Vouches.route else Screen.Onboarding.route
    }

    Scaffold(
        snackbarHost = snackbarHost,
        bottomBar = {
            if (isNotOnboarding) {
                ShortNavigationBar {
                    val activeTopLevelRoute = when (currentRoute) {
                        Screen.Persons.route,
                        Screen.ViewPerson.route -> Screen.Persons.route

                        Screen.Settings.route -> Screen.Settings.route

                        Screen.Vouches.route,
                        Screen.AddVouch.route,
                        Screen.AddGroupVouch.route,
                        Screen.EditVouch.route,
                        Screen.EditGroupVouch.route,
                        Screen.SettledSpace.route -> Screen.Vouches.route

                        else -> Screen.Vouches.route
                    }

                    navigationItems.forEach { item ->
                        val isSelected = item.screen.route == activeTopLevelRoute
                        val icon = if (isSelected) item.iconSelected else item.icon
                        ShortNavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = stringResource(item.screen.titleRes)
                                )
                            },
                            label = { Text(stringResource(item.screen.titleRes)) },
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                if (isSelected) {
                                    if (navBackStackEntry?.destination?.route != item.screen.route) {
                                        navController.popBackStack(
                                            route = item.screen.route,
                                            inclusive = false
                                        )
                                    }
                                } else {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            })
                    }
                }
            }
        }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        settingsViewModel.setOnboardingCompleted(true)
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.Vouches.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    },
                    currentCurrencyCode = uiState.currencyCode,
                    onCurrencySelected = { currencyCode ->
                        settingsViewModel.setCurrency(currencyCode)
                    }
                )
            }
            composable(Screen.Vouches.route) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                    VouchesScreen(
                        viewModel = vouchesViewModel,
                        onNavigateToAddVouch = {
                            navController.navigate(Screen.AddVouch.createRoute())
                        },
                        onNavigateToAddGroupVouch = {
                            navController.navigate(Screen.AddGroupVouch.route)
                        },
                        onNavigateToEditVouch = { vouchId, isGroup ->
                            if (isGroup) {
                                navController.navigate(Screen.EditGroupVouch.createRoute(vouchId))
                            } else {
                                navController.navigate(Screen.EditVouch.createRoute(vouchId))
                            }
                        },
                        onNavigateToPerson = { personName ->
                            navController.navigate(Screen.ViewPerson.createRoute(personName))
                        },
                        onNavigateToSettledSpace = {
                            navController.navigate(Screen.SettledSpace.createRoute())
                        }
                    )
                }
            }
            composable(Screen.Persons.route) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                    PersonsScreen(viewModel = personsViewModel, onNavigateToViewPerson = { personName ->
                        navController.navigate(Screen.ViewPerson.createRoute(personName))
                    })
                }
            }
            composable(Screen.Settings.route) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateToOnboarding = {
                            navController.navigate(Screen.Onboarding.route)
                        }
                    )
                }
            }
            composable(
                route = Screen.AddVouch.route,
                arguments = listOf(
                    navArgument(Screen.AddVouch.PERSON_NAME_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val prefilledPersonName =
                    backStackEntry.arguments?.getString(Screen.AddVouch.PERSON_NAME_ARG)
                Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                    SingleVouchScreen(
                        prefilledPersonName = prefilledPersonName,
                        onNavigateBack = {
                            navController.popBackStack()
                        })
                }
            }
            composable(Screen.AddGroupVouch.route) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                    GroupVouchScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(
                route = Screen.ViewPerson.route,
                arguments = listOf(navArgument("personName") { type = NavType.StringType })
            ) { backStackEntry ->
                val personName =
                    backStackEntry.arguments?.getString("personName") ?: return@composable
                Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                    PersonScreen(
                        personName = personName,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToAddVouch = { selectedPersonName ->
                            navController.navigate(Screen.AddVouch.createRoute(selectedPersonName))
                        },
                        onNavigateToEditVouch = { vouchId, isGroup ->
                            if (isGroup) {
                                navController.navigate(Screen.EditGroupVouch.createRoute(vouchId))
                            } else {
                                navController.navigate(Screen.EditVouch.createRoute(vouchId))
                            }
                        },
                        onNavigateToPerson = { selectedPersonName ->
                            if (selectedPersonName != personName) {
                                navController.navigate(Screen.ViewPerson.createRoute(selectedPersonName))
                            }
                        },
                        onNavigateToSettledSpace = { targetPersonName ->
                            navController.navigate(Screen.SettledSpace.createRoute(targetPersonName))
                        }
                    )
                }
            }
            composable(
                route = Screen.SettledSpace.route,
                arguments = listOf(
                    navArgument(Screen.SettledSpace.PERSON_NAME_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val targetPersonName =
                    backStackEntry.arguments?.getString(Screen.SettledSpace.PERSON_NAME_ARG)
                Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                    SettledSpaceScreen(
                        initialPersonName = targetPersonName,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToEditVouch = { vouchId, isGroup ->
                            if (isGroup) {
                                navController.navigate(Screen.EditGroupVouch.createRoute(vouchId))
                            } else {
                                navController.navigate(Screen.EditVouch.createRoute(vouchId))
                            }
                        },
                        onNavigateToPerson = { selectedPersonName ->
                            navController.navigate(Screen.ViewPerson.createRoute(selectedPersonName))
                        }
                    )
                }
            }
            composable(
                route = Screen.EditVouch.route,
                arguments = listOf(navArgument("vouchId") { type = NavType.IntType })
            ) { backStackEntry ->
                val vouchId = backStackEntry.arguments?.getInt("vouchId") ?: return@composable
                Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                    SingleVouchScreen(
                        vouchId = vouchId.toLong(),
                        edit = true,
                        onNavigateBack = {
                            navController.popBackStack()
                        })
                }
            }
            composable(
                route = Screen.EditGroupVouch.route,
                arguments = listOf(navArgument("vouchId") { type = NavType.IntType })
            ) { backStackEntry ->
                val vouchId = backStackEntry.arguments?.getInt("vouchId") ?: return@composable
                Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                    GroupVouchScreen(
                        vouchId = vouchId.toLong(),
                        edit = true,
                        onNavigateBack = {
                            navController.popBackStack()
                        })
                }
            }
        }
    }
}
