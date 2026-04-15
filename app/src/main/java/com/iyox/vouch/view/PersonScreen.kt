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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.viewmodel.PersonViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    viewModel: PersonViewModel = viewModel(),
    personName: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditVouch: (Int) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val personData by viewModel.personData.collectAsState()
    val vouches by viewModel.vouches.collectAsState()
    val amountGiven by viewModel.amountGiven.collectAsState()
    val amountOwed by viewModel.amountOwed.collectAsState()

    DisposableEffect(lifecycleOwner, personName) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadPerson(personName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.loadPerson(personName)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(personName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Person Info Card (styled like in PersonsScreen)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val total = personData?.totalAmount?.toPlainString() ?: "0"
                    val count = personData?.vouchCount ?: 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Balance",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = androidx.compose.foundation.shape.CircleShape,
                        ) {
                            Text(
                                text = "$count ${if (count == 1) "Vouch" else "Vouches"}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "$total€",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Medium,
                        color = if ((personData?.totalAmount?.signum()
                                ?: 0) >= 0
                        ) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Given", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${amountGiven.toPlainString()}€",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Received", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${amountOwed.toPlainString()}€",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Vouches List (like in HomeScreen)
            if (vouches.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No vouches for this person")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Top)
                ) {
                    itemsIndexed(vouches, key = { _, item -> item.id }) { _, vouch ->
                        if (vouch.settled) {
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .alpha(0.6f)
                            ) {
                                VouchItem(
                                    vouch = vouch,
                                    canSettle = false,
                                    onSettle = { viewModel.unSettleVouch(vouch) },
                                    onDelete = { viewModel.deleteVouch(vouch) },
                                    onEdit = { onNavigateToEditVouch(vouch.id) }
                                )
                            }
                        } else {
                            Box(modifier = Modifier.animateItem()) {
                                VouchItem(
                                    vouch = vouch,
                                    onSettle = { viewModel.settleVouch(vouch) },
                                    onDelete = { viewModel.deleteVouch(vouch) },
                                    onEdit = { onNavigateToEditVouch(vouch.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
