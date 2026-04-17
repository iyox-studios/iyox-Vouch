package com.iyox.vouch.view

import androidx.compose.foundation.layout.Box
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
import com.iyox.vouch.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(), onNavigateToAddVouch: () -> Unit = {}, onNavigateToEditVouch: (Int) -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val vouches by viewModel.vouches.collectAsState()
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
            FilterChip(
                selected = true, onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                viewModel.toggleSortedBy()
            }, label = {
                when (sortedBy) {
                    0 -> Text("Date")
                    1 -> Text("Amount")
                    2 -> Text("Person")
                }
            }, leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Selected",
                    modifier = Modifier.size(18.dp)
                )
            }, modifier = Modifier.padding(end = 16.dp)
            )
            FilterChip(
                selected = showOnlyGiven > 0, onClick = {
                if (showOnlyGiven == 2) haptics.performHapticFeedback(HapticFeedbackType.ToggleOff)
                else haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                viewModel.toggleShowOnlyGiven()
            }, label = {
                when (showOnlyGiven) {
                    0 -> Text("Giv. & Rec.")
                    1 -> Text("Given")
                    2 -> Text("Received")
                }
            }, leadingIcon = {
                if (showOnlyGiven > 0) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Selected",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }, modifier = Modifier.padding(end = 16.dp)
            )
            FilterChip(
                selected = showOnlyUnsettled, onClick = {
                if (showOnlyUnsettled) haptics.performHapticFeedback(HapticFeedbackType.ToggleOff)
                else haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                viewModel.toggleShowOnlyUnsettled()
            }, label = { Text("Unsettled") }, leadingIcon = {
                if (showOnlyUnsettled) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }, modifier = Modifier.padding(end = 16.dp)
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
        if (vouches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Tap + to add a new vouch")
            }
        } else {
            val displayed =
                viewModel.getDisplayedVouches(showOnlyUnsettled, showOnlyGiven, vouches)
            val displayedSorted = when (sortedBy) {
                0 -> displayed.sortedBy { it.timestamp }
                1 -> displayed.sortedBy { it.amount }
                2 -> displayed.sortedBy { it.personName ?: "" }
                else -> displayed.sortedBy { it.timestamp }
            }.reversed()
            com.iyox.vouch.ui.components.vouches.VouchList(
                displayedSorted = displayedSorted,
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