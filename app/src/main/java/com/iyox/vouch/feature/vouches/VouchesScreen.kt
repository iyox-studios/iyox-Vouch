package com.iyox.vouch.feature.vouches

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.R
import com.iyox.vouch.core.utils.rememberUndoCoordinator
import com.iyox.vouch.ui.components.PageHeader
import com.iyox.vouch.ui.components.vouches.VouchList

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VouchesScreen(
    viewModel: VouchesViewModel = viewModel(),
    onNavigateToAddVouch: () -> Unit = {},
    onNavigateToEditVouch: (Int) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    rememberUndoCoordinator()

    val uiState by viewModel.uiState.collectAsState()

    val listState = remember(uiState.showOnlyUnsettled, uiState.showOnlyGiven, uiState.sortedBy) {
        LazyListState()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadVouches()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(topBar = {
        PageHeader(stringResource(R.string.screen_vouches)) {
            VouchFilterRow(
                sortedBy = uiState.sortedBy,
                showOnlyGiven = uiState.showOnlyGiven,
                showOnlyUnsettled = uiState.showOnlyUnsettled,
                onToggleSort = viewModel::toggleSortedBy,
                onToggleGiven = viewModel::toggleShowOnlyGiven,
                onToggleUnsettled = viewModel::toggleShowOnlyUnsettled
            )
        }
    }, floatingActionButton = {
        FloatingActionButton(
            onClick = onNavigateToAddVouch
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_add_vouch)
            )
        }
    }) { innerPadding ->
        if (uiState.vouches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_vouches_prompt),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            VouchList(
                displayedSorted = uiState.vouches,
                listState = listState,
                innerPadding = innerPadding,
                sharedVouchActions = viewModel.sharedActions,
                onEdit = { onNavigateToEditVouch(it) }
            )
        }
    }
}

@Composable
private fun VouchFilterRow(
    sortedBy: Int,
    showOnlyGiven: Int,
    showOnlyUnsettled: Boolean,
    onToggleSort: () -> Unit,
    onToggleGiven: () -> Unit,
    onToggleUnsettled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    // Helper to handle haptic + callback
    val handleToggle: (Boolean, () -> Unit) -> Unit = { isTurningOff, callback ->
        haptics.performHapticFeedback(
            if (isTurningOff) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
        )
        callback()
    }

    Row(modifier = modifier) {
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

        // Unsettled Filter Chip
        FilterChip(
            selected = showOnlyUnsettled,
            onClick = { handleToggle(showOnlyUnsettled, onToggleUnsettled) },
            label = { Text(stringResource(R.string.filter_unsettled)) },
            leadingIcon = {
                if (showOnlyUnsettled) {
                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                }
            }
        )
    }
}