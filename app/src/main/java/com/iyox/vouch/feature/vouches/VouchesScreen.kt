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
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.R
import com.iyox.vouch.ui.components.EmptySearchContent
import com.iyox.vouch.ui.components.PageHeader
import com.iyox.vouch.ui.components.VouchFilterRow
import com.iyox.vouch.ui.components.vouches.VouchList

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VouchesScreen(
    viewModel: VouchesViewModel = viewModel(),
    onNavigateToAddVouch: () -> Unit = {},
    onNavigateToAddGroupVouch: () -> Unit = {},
    onNavigateToEditVouch: (Long, Boolean) -> Unit = { _, _ -> },
    onNavigateToPerson: (String) -> Unit = {},
    onNavigateToSettledSpace: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val fabMenuExpanded = remember { mutableStateOf(false) }

    val listState = remember(uiState.showOnlyGiven, uiState.sortedBy) {
        LazyListState()
    }

    Scaffold(
        topBar = {
            PageHeader(
                title = stringResource(R.string.screen_vouches)
            ) {
                VouchFilterRow(
                    sortedBy = uiState.sortedBy,
                    showOnlyGiven = uiState.showOnlyGiven,
                    onToggleSort = viewModel::toggleSortedBy,
                    onToggleGiven = viewModel::toggleShowOnlyGiven,
                    onSettledClick = onNavigateToSettledSpace,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    searchPlaceholder = stringResource(R.string.search_vouches_placeholder)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = fabMenuExpanded.value,
                button = {
                    FloatingActionButton(
                        onClick = { fabMenuExpanded.value = !fabMenuExpanded.value }
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
                        fabMenuExpanded.value = false
                        onNavigateToAddGroupVouch()
                    },
                    icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_add_group_vouch)) }
                )
                FloatingActionButtonMenuItem(
                    onClick = {
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
            if (uiState.vouches.isEmpty()) {
                if (uiState.searchQuery.isNotBlank()) {
                    EmptySearchContent(
                        searchQuery = uiState.searchQuery,
                        onClearSearch = { viewModel.setSearchQuery("") }
                    )
                } else {
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
                                        imageVector = if (uiState.settledCount > 0) Icons.Outlined.Archive else Icons.Filled.Payments,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (uiState.settledCount > 0) stringResource(R.string.all_settled_title) else stringResource(
                                    R.string.empty_vouches_title
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.settledCount > 0) stringResource(R.string.all_settled_global_subtitle) else stringResource(
                                    R.string.empty_vouches_subtitle
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (uiState.settledCount > 0) {
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
                        }
                    }
                }
            } else {
                VouchList(
                    displayedSorted = uiState.vouches,
                    listState = listState,
                    innerPadding = PaddingValues(0.dp),
                    sharedVouchActions = viewModel.sharedActions,
                    onEdit = onNavigateToEditVouch,
                    onNavigateToPerson = onNavigateToPerson
                )
            }
        }
    }
}