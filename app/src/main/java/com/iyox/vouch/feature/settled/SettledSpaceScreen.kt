package com.iyox.vouch.feature.settled

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.R
import com.iyox.vouch.feature.vouch.PersonAvatar
import com.iyox.vouch.ui.components.VouchBackButton
import com.iyox.vouch.ui.components.vouches.VouchList

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettledSpaceScreen(
    initialPersonName: String? = null,
    viewModel: SettledSpaceViewModel = viewModel(
        factory = SettledSpaceViewModel.factory(initialPersonName)
    ),
    onNavigateBack: () -> Unit,
    onNavigateToEditVouch: (Int, Boolean) -> Unit = { _, _ -> },
    onNavigateToPerson: (String) -> Unit = {}
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
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 6.dp)
            )

            if (uiState.vouches.isEmpty()) {
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


@Composable
private fun SettledFilterRow(
    sortedBy: Int,
    showOnlyGiven: Int,
    selectedPersonName: String?,
    onToggleSort: () -> Unit,
    onToggleGiven: () -> Unit,
    onClearPerson: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    val handleToggle: (Boolean, () -> Unit) -> Unit = { isTurningOff, callback ->
        haptics.performHapticFeedback(
            if (isTurningOff) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
        )
        callback()
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort Chip
        FilterChip(
            selected = true,
            onClick = { handleToggle(false, onToggleSort) },
            label = {
                Text(
                    when (sortedBy) {
                        0 -> stringResource(R.string.filter_date)
                        1 -> stringResource(R.string.filter_amount)
                        2 -> stringResource(R.string.filter_person)
                        else -> stringResource(R.string.filter_date)
                    }
                )
            },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(18.dp))
            },
            modifier = Modifier.padding(end = 8.dp)
        )

        // Given/Received Filter Chip
        FilterChip(
            selected = showOnlyGiven > 0,
            onClick = { handleToggle(showOnlyGiven == 2, onToggleGiven) },
            label = {
                Text(
                    when (showOnlyGiven) {
                        0 -> stringResource(R.string.filter_given_received_short)
                        1 -> stringResource(R.string.label_given)
                        2 -> stringResource(R.string.label_received)
                        else -> ""
                    }
                )
            },
            leadingIcon = {
                if (showOnlyGiven > 0) {
                    Icon(Icons.Default.FilterList, null, Modifier.size(18.dp))
                }
            },
            modifier = Modifier.padding(end = 8.dp)
        )

        // Person Chip if filtered
        if (selectedPersonName != null) {
            InputChip(
                selected = true,
                onClick = onClearPerson,
                label = {
                    Text(
                        text = selectedPersonName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                avatar = {
                    PersonAvatar(name = selectedPersonName, size = 18.dp)
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}
