package com.iyox.vouch.feature.settled

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iyox.vouch.R
import com.iyox.vouch.ui.components.EmptySearchContent
import com.iyox.vouch.ui.components.SettledFilterRow
import com.iyox.vouch.ui.components.VouchBackButton
import com.iyox.vouch.ui.components.vouches.VouchList

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettledSpaceScreen(
    initialPersonName: String? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToEditVouch: (Int, Boolean) -> Unit = { _, _ -> },
    onNavigateToPerson: (String) -> Unit = {},
    viewModel: SettledSpaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.selectedPersonName != null) {
                            stringResource(R.string.title_person_settled_space, uiState.selectedPersonName!!)
                        } else {
                            stringResource(R.string.screen_settled_space)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    VouchBackButton(onClick = onNavigateBack)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Filter & Sort Row
            SettledFilterRow(
                sortedBy = uiState.sortedBy,
                showOnlyGiven = uiState.showOnlyGiven,
                selectedPersonName = uiState.selectedPersonName,
                onToggleSort = viewModel::toggleSortedBy,
                onToggleGiven = viewModel::toggleShowOnlyGiven,
                onClearPerson = { viewModel.setSelectedPerson(null) },
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 6.dp)
            )

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
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(88.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Archive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.empty_settled_vouches_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.selectedPersonName != null) {
                                    stringResource(
                                        R.string.empty_person_settled_vouches,
                                        uiState.selectedPersonName!!
                                    )
                                } else {
                                    stringResource(R.string.empty_settled_vouches_subtitle)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (uiState.selectedPersonName != null) {
                                OutlinedButton(
                                    onClick = { viewModel.setSelectedPerson(null) },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text(stringResource(R.string.action_show_all_settled))
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
                    onNavigateToPerson = onNavigateToPerson,
                    isSettled = { true }
                )
            }
        }
    }
}
