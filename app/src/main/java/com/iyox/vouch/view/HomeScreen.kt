package com.iyox.vouch.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.ui.components.VouchSwipeToDismiss
import com.iyox.vouch.model.VouchDataEntity
import com.iyox.vouch.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(), onNavigateToAddVouch: () -> Unit = {}
) {
    val vouches by viewModel.vouches.collectAsState()
    val showOnlyUnsettled by viewModel.showOnlyUnsettled.collectAsState()
    val showOnlyGiven by viewModel.showOnlyGiven.collectAsState()


    LaunchedEffect(Unit) {
        viewModel.loadVouches()
    }

    Scaffold(topBar = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            TopAppBar(title = {
                Text(
                    "Vouches", style = MaterialTheme.typography.displayLargeEmphasized
                )
            })
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                FilterChip(
                    selected = showOnlyUnsettled,
                    onClick = { viewModel.toggleShowOnlyUnsettled() },
                    label = { Text("Unsettled") },
                    leadingIcon = {
                        if (showOnlyUnsettled) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.padding(end = 16.dp)
                )
                FilterChip(
                    selected = showOnlyGiven > 0,
                    onClick = { viewModel.toggleShowOnlyGiven() },
                    label = {
                        when (showOnlyGiven) {
                            0 -> Text("Given or Rec.")
                            1 -> Text("Given")
                            2 -> Text("Received")
                        }
                    },
                    leadingIcon = {
                        if (showOnlyGiven > 0) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.padding(end = 16.dp)
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val displayed = viewModel.getDisplayedVouches(showOnlyUnsettled, showOnlyGiven, vouches)
                itemsIndexed(
                    displayed, { _, item -> item.id }) { _, vouch ->
                    if (vouch.settled) {
                        Box(modifier = Modifier.alpha(0.6f)) {
                            VouchItem(
                                vouch = vouch,
                                canSettle = false,
                                onSettle = { viewModel.unSettleVouch(vouch) },
                                onDelete = { viewModel.deleteVouch(vouch) })
                        }
                    } else {
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VouchItem(
    vouch: VouchDataEntity,
    canSettle: Boolean = true,
    onSettle: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    val largeRadius = 20.dp

    val content = @Composable {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(largeRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
                    color = if (vouch.given) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
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

                if (vouch.person != null) Text(
                    text = vouch.person,
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