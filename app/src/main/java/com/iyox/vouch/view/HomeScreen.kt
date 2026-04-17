package com.iyox.vouch.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.ui.components.PageHeader
import com.iyox.vouch.ui.components.vouches.VouchList
import com.iyox.vouch.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(), onNavigateToAddVouch: () -> Unit = {}, onNavigateToEditVouch: (Int) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()
    val showOnlyUnsettled by viewModel.showOnlyUnsettled.collectAsState()
    val showOnlyGiven by viewModel.showOnlyGiven.collectAsState()
    val sortedBy by viewModel.sortedBy.collectAsState()

    val listState = androidx.compose.runtime.remember(showOnlyUnsettled, showOnlyGiven, sortedBy) {
        androidx.compose.foundation.lazy.LazyListState()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
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
        PageHeader("Vouches") {
            VouchFilterRow(
                sortedBy = sortedBy,
                showOnlyGiven = showOnlyGiven,
                showOnlyUnsettled = showOnlyUnsettled,
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
                imageVector = Icons.Default.Add, contentDescription = "Add Vouch"
            )
        }
    }) { innerPadding ->
        if (uiState.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Tap + to add a new vouch")
            }
        } else {
            VouchList(
                displayedSorted = uiState,
                listState = listState,
                innerPadding = innerPadding,
                onSettle = { viewModel.settleVouch(it) },
                onUnSettle = { viewModel.unSettleVouch(it) },
                onDelete = { viewModel.deleteVouch(it) },
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
                        0 -> "Date"
                        1 -> "Amount"
                        2 -> "Person"
                        else -> "Date"
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
                        0 -> "Giv. & Rec."
                        1 -> "Given"
                        2 -> "Received"
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
            label = { Text("Unsettled") },
            leadingIcon = {
                if (showOnlyUnsettled) {
                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                }
            }
        )
    }
}