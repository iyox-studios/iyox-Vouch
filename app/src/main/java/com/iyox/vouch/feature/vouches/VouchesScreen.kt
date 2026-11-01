package com.iyox.vouch.feature.vouches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.R
import com.iyox.vouch.ui.components.EmptySearchContent
import com.iyox.vouch.ui.components.PageHeader
import com.iyox.vouch.ui.components.VouchBalanceHeroCard
import com.iyox.vouch.ui.components.VouchFilterRow
import com.iyox.vouch.ui.components.vouches.VouchList

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VouchesScreen(
    viewModel: VouchesViewModel = viewModel(),
    onNavigateToAddVouch: () -> Unit = {},
    onNavigateToAddGroupVouch: () -> Unit = {},
    onVouchClick: ((Long, Boolean) -> Unit)? = null,
    onNavigateToPerson: (String) -> Unit = {},
    onNavigateToSettledSpace: () -> Unit = {},
    onNavigateToEditVouch: (Long, Boolean) -> Unit = { _, _ -> }
) {
    val handleVouchClick = onVouchClick ?: onNavigateToEditVouch
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    val fabMenuExpanded = remember { mutableStateOf(false) }

    val listState = remember(uiState.showOnlyGiven, uiState.sortedBy) {
        LazyListState()
    }

    Scaffold(
        topBar = {
            PageHeader(
                title = stringResource(R.string.screen_vouches)
            )
        },
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = fabMenuExpanded.value,
                button = {
                    FloatingActionButton(
                        onClick = {
                            val willExpand = !fabMenuExpanded.value
                            haptic.performHapticFeedback(
                                if (willExpand) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                            )
                            fabMenuExpanded.value = willExpand
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.action_add_vouch)
                        )
                    }
                }
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                        fabMenuExpanded.value = false
                        onNavigateToAddGroupVouch()
                    },
                    icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_add_group_vouch)) }
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                        fabMenuExpanded.value = false
                        onNavigateToAddVouch()
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_add_vouch)) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            VouchBalanceHeroCard(
                amountGiven = uiState.totalGiven,
                amountOwed = uiState.totalOwed,
                total = uiState.netTotal,
                vouchCount = uiState.activeVouchesCount,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 12.dp)
            )

            VouchFilterRow(
                sortedBy = uiState.sortedBy,
                showOnlyGiven = uiState.showOnlyGiven,
                onToggleSort = viewModel::toggleSortedBy,
                onToggleGiven = viewModel::toggleShowOnlyGiven,
                onSettledClick = onNavigateToSettledSpace,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::setSearchQuery,
                searchPlaceholder = stringResource(R.string.search_vouches_placeholder),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
            )
            if (uiState.vouches.isEmpty()) {
                if (uiState.searchQuery.isNotBlank()) {
                    EmptySearchContent(
                        searchQuery = uiState.searchQuery,
                        onClearSearch = { viewModel.setSearchQuery("") }
                    )
                } else {
                    val emptyIcon: ImageVector
                    val emptyTitle: String
                    val emptySubtitle: String
                    var showSettledButton = false
                    var showResetFilterButton = false

                    when {
                        uiState.totalVouchesCount == 0 -> {
                            emptyIcon = Icons.Filled.Payments
                            emptyTitle = stringResource(R.string.empty_vouches_title)
                            emptySubtitle = stringResource(R.string.empty_vouches_subtitle)
                        }
                        uiState.activeVouchesCount == 0 -> {
                            emptyIcon = Icons.Outlined.Archive
                            emptyTitle = stringResource(R.string.all_settled_title)
                            emptySubtitle = stringResource(R.string.all_settled_global_subtitle)
                            showSettledButton = uiState.settledCount > 0
                        }
                        else -> {
                            emptyIcon = Icons.Default.FilterList
                            emptyTitle = stringResource(R.string.empty_vouches_filtered_title)
                            emptySubtitle = stringResource(R.string.empty_vouches_filtered_subtitle)
                            showResetFilterButton = true
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = MaterialShapes.Cookie9Sided.toShape(),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(88.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = emptyIcon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                            Text(
                                text = emptyTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = emptySubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (showSettledButton) {
                                OutlinedButton(
                                    onClick = onNavigateToSettledSpace,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Archive,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.action_view_settled_history,
                                            uiState.settledCount
                                        ),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                            if (showResetFilterButton) {
                                OutlinedButton(
                                    onClick = viewModel::resetTypeFilter,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text(text = stringResource(R.string.action_clear_filter))
                                }
                            }
                        }
                    }
                }
            } else {
                VouchList(
                    displayedSorted = uiState.vouches,
                    listState = listState,
                    innerPadding = PaddingValues(0.dp),
                    sharedVouchActions = viewModel.sharedActions,
                    onEdit = handleVouchClick,
                    onNavigateToPerson = onNavigateToPerson
                )
            }
        }
    }
}
