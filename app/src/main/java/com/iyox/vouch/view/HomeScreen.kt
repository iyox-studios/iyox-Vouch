package com.iyox.vouch.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToAddVouch: () -> Unit = {}
) {
    val vouches by viewModel.vouches.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadVouches()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vouches", style = MaterialTheme.typography.displayLarge) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddVouch
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Vouch"
                )
            }
        }
    ) { innerPadding ->
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
                itemsIndexed(vouches) { index, vouch ->
                    VouchItem(
                        vouch = vouch,
                        isFirst = index == 0,
                        isLast = index == vouches.lastIndex,
                        isOnly = vouches.size == 1
                    )
                }
            }
        }
    }
}

@Composable
fun VouchItem(
    vouch: VouchDataEntity,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    isOnly: Boolean = false
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    val largeRadius = 28.dp
    val smallRadius = 8.dp

    val shape = when {
        isOnly -> RoundedCornerShape(largeRadius)
        isFirst -> RoundedCornerShape(
            topStart = largeRadius,
            topEnd = largeRadius,
            bottomStart = smallRadius,
            bottomEnd = smallRadius
        )

        isLast -> RoundedCornerShape(
            topStart = smallRadius,
            topEnd = smallRadius,
            bottomStart = largeRadius,
            bottomEnd = largeRadius
        )

        else -> RoundedCornerShape(smallRadius)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
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
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${vouch.amount}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = vouch.description.ifBlank { dateFormat.format(vouch.timestamp) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

