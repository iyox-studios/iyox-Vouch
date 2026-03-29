package com.iyox.vouch.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.ui.components.VouchSwipeToDismiss
import com.iyox.vouch.model.Vouch
import com.iyox.vouch.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(), onNavigateToAddVouch: () -> Unit = {}
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
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            TopAppBar(
                title = {
                    Text(
                        "Vouches", style = MaterialTheme.typography.displayMedium
                    )
                },
                windowInsets = WindowInsets(
                    top = 20.dp
                )
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Top)
            ) {
                val displayed =
                    viewModel.getDisplayedVouches(showOnlyUnsettled, showOnlyGiven, vouches)
                val displayedSorted = when (sortedBy) {
                    0 -> displayed.sortedBy { it.timestamp }
                    1 -> displayed.sortedBy { it.amount }
                    2 -> displayed.sortedBy { it.personName ?: "" }
                    else -> displayed.sortedBy { it.timestamp }
                }.reversed()
                itemsIndexed(
                    displayedSorted, { _, item -> item.id }) { _, vouch ->
                    if (vouch.settled) {
                        Box(modifier = Modifier
                            .animateItem()
                            .alpha(0.6f)) {
                            VouchItem(
                                vouch = vouch,
                                canSettle = false,
                                onSettle = { viewModel.unSettleVouch(vouch) },
                                onDelete = { viewModel.deleteVouch(vouch) })
                        }
                    } else {
                        Box(modifier = Modifier.animateItem()) {
                            VouchItem(
                                vouch = vouch,
                                onSettle = { viewModel.settleVouch(vouch) },
                                onDelete = { viewModel.deleteVouch(vouch) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VouchItem(
    vouch: Vouch,
    canSettle: Boolean = true,
    onSettle: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())


    val content = @Composable {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.largeIncreased,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (vouch.given) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (vouch.given) Icons.Outlined.ArrowOutward else Icons.Outlined.SouthWest,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = vouch.amount.toString() + " " + vouch.currency.symbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = vouch.description.ifBlank { dateFormat.format(vouch.timestamp) },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                if (vouch.personName != null) Text(
                    text = vouch.personName,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (canSettle) {
        VouchSwipeToDismiss(
            enableDismissFromStartToEnd = true,
            onStartToEnd = onSettle,
            enableDismissFromEndToStart = true,
            onEndToStart = onDelete
        ) {
            content()
        }
    } else {
        VouchSwipeToDismiss(
            enableDismissFromStartToEnd = true,
            onStartToEnd = onSettle,
            enableDismissFromEndToStart = true,
            onEndToStart = onDelete,
            settleText = "Unsettle"
        ) {
            content()
        }
    }
}