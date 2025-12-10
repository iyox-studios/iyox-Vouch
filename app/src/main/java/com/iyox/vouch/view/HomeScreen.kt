package com.iyox.vouch.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    LaunchedEffect(Unit) {
        viewModel.loadVouches()
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Vouches", style = MaterialTheme.typography.displayLargeEmphasized) })
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
                val displayed = vouches.asReversed()
                itemsIndexed(displayed) { index, vouch ->
                    VouchItem(
                        vouch = vouch,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VouchItem(
    vouch: VouchDataEntity,
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    val largeRadius = 20.dp

    val shape = RoundedCornerShape(largeRadius)

    Card(
        modifier = Modifier.fillMaxWidth(), shape = shape, colors = CardDefaults.cardColors(
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

