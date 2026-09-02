package com.iyox.vouch.navigation


import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.iyox.vouch.R
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
import kotlinx.coroutines.launch

data class NavigationItem(
    val targetPage: Int,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val isSelected: (NavDestination?, Int) -> Boolean
)

@Composable
fun AppNavigation(
    snackbarHost: @Composable (() -> Unit) = {},
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val uiState by settingsViewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })

    val haptic = LocalHapticFeedback.current

    val navigationItems = listOf(
        NavigationItem(
            targetPage = 0,
            titleRes = R.string.screen_vouches,
            icon = Icons.Outlined.Payments,
            iconSelected = Icons.Filled.Payments,
            isSelected = { dest, page ->
                (dest?.hasRoute<VouchesRoute>() == true && page == 0) ||
                        dest?.hasRoute<SingleVouchRoute>() == true ||
                        dest?.hasRoute<GroupVouchRoute>() == true ||
                        dest?.hasRoute<SettledSpaceRoute>() == true
            }
        ),
        NavigationItem(
            targetPage = 1,
            titleRes = R.string.screen_people,
            icon = Icons.Outlined.Group,
            iconSelected = Icons.Filled.Group,
            isSelected = { dest, page ->
                (dest?.hasRoute<VouchesRoute>() == true && page == 1) ||
                        dest?.hasRoute<ViewPersonRoute>() == true
            }
        ),
        NavigationItem(
            targetPage = 2,
            titleRes = R.string.screen_settings,
            icon = Icons.Outlined.Settings,
            iconSelected = Icons.Filled.Settings,
            isSelected = { dest, page ->
                dest?.hasRoute<VouchesRoute>() == true && page == 2
            }
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isNotOnboarding = currentDestination?.hasRoute<OnboardingRoute>() != true

    val startDestination: Any = remember {
        if (settingsViewModel.isOnboardingCompleted()) VouchesRoute else OnboardingRoute
    }

    val isMainDestination = currentDestination?.hasRoute<VouchesRoute>() == true
    BackHandler(enabled = isMainDestination && pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.scrollToPage(0)
        }
    }

    Scaffold(
        snackbarHost = snackbarHost,
        bottomBar = {
            if (isNotOnboarding) {
                ShortNavigationBar {
                    navigationItems.forEach { item ->
                        val isSelected = item.isSelected(currentDestination, pagerState.currentPage)
                        val icon = if (isSelected) item.iconSelected else item.icon
                        ShortNavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = stringResource(item.titleRes)
                                )
                            },
                            label = { Text(stringResource(item.titleRes)) },
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                if (!isMainDestination) {
                                    navController.popBackStack<VouchesRoute>(inclusive = false)
                                }
                                if (pagerState.currentPage != item.targetPage) {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(item.targetPage)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(
                    onFinish = {
                        settingsViewModel.setOnboardingCompleted(true)
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(VouchesRoute) {
                                popUpTo<OnboardingRoute> { inclusive = true }
                            }
                        }
                    },
                    currentCurrencyCode = uiState.currencyCode,
                    onCurrencySelected = { currencyCode ->
                        settingsViewModel.setCurrency(currencyCode)
                    }
                )
            }

            composable<VouchesRoute> {
                val vouchesViewModel: VouchesViewModel = hiltViewModel()
                val personsViewModel: PersonsViewModel = hiltViewModel()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        beyondViewportPageCount = 2,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> {
                                VouchesScreen(
                                    viewModel = vouchesViewModel,
                                    onNavigateToAddVouch = {
                                        navController.navigate(SingleVouchRoute())
                                    },
                                    onNavigateToAddGroupVouch = {
                                        navController.navigate(GroupVouchRoute())
                                    },
                                    onNavigateToEditVouch = { vouchId, isGroup ->
                                        if (isGroup) {
                                            navController.navigate(GroupVouchRoute(vouchId = vouchId))
                                        } else {
                                            navController.navigate(SingleVouchRoute(vouchId = vouchId))
                                        }
                                    },
                                    onNavigateToPerson = { personName ->
                                        navController.navigate(ViewPersonRoute(personName = personName))
                                    },
                                    onNavigateToSettledSpace = {
                                        navController.navigate(SettledSpaceRoute())
                                    }
                                )
                            }

                            1 -> {
                                PersonsScreen(
                                    viewModel = personsViewModel,
                                    onNavigateToViewPerson = { personName ->
                                        navController.navigate(ViewPersonRoute(personName = personName))
                                    }
                                )
                            }

                            2 -> {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onNavigateToOnboarding = {
                                        navController.navigate(OnboardingRoute)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            composable<PersonsRoute> {
                LaunchedEffect(Unit) {
                    pagerState.scrollToPage(1)
                    navController.popBackStack<VouchesRoute>(inclusive = false)
                }
            }

            composable<SettingsRoute> {
                LaunchedEffect(Unit) {
                    pagerState.scrollToPage(2)
                    navController.popBackStack<VouchesRoute>(inclusive = false)
                }
            }

            composable<SingleVouchRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<SingleVouchRoute>()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    SingleVouchScreen(
                        vouchId = route.vouchId,
                        prefilledPersonName = route.personName,
                        edit = route.vouchId != null,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable<GroupVouchRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<GroupVouchRoute>()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    GroupVouchScreen(
                        vouchId = route.vouchId,
                        edit = route.vouchId != null,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable<ViewPersonRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ViewPersonRoute>()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    PersonScreen(
                        personName = route.personName,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToAddVouch = { selectedPersonName ->
                            navController.navigate(SingleVouchRoute(personName = selectedPersonName))
                        },
                        onNavigateToEditVouch = { vouchId, isGroup ->
                            if (isGroup) {
                                navController.navigate(GroupVouchRoute(vouchId = vouchId))
                            } else {
                                navController.navigate(SingleVouchRoute(vouchId = vouchId))
                            }
                        },
                        onNavigateToPerson = { selectedPersonName ->
                            if (selectedPersonName != route.personName) {
                                navController.navigate(ViewPersonRoute(personName = selectedPersonName))
                            }
                        },
                        onNavigateToSettledSpace = { targetPersonName ->
                            navController.navigate(SettledSpaceRoute(personName = targetPersonName))
                        }
                    )
                }
            }

            composable<SettledSpaceRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<SettledSpaceRoute>()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    SettledSpaceScreen(
                        initialPersonName = route.personName,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToEditVouch = { vouchId, isGroup ->
                            if (isGroup) {
                                navController.navigate(GroupVouchRoute(vouchId = vouchId))
                            } else {
                                navController.navigate(SingleVouchRoute(vouchId = vouchId))
                            }
                        },
                        onNavigateToPerson = { selectedPersonName ->
                            navController.navigate(ViewPersonRoute(personName = selectedPersonName))
                        }
                    )
                }
            }
        }
    }
}
