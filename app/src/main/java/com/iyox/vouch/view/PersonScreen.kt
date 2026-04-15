package com.iyox.vouch.view

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.viewmodel.PersonViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var isCapturing by remember { mutableStateOf(false) }

    var showSettleDialog by remember { mutableStateOf(false) }
    var fullySettle by remember { mutableStateOf(false) }

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
                .drawWithContent {
                    graphicsLayer.record(size = this.size.let {
                        androidx.compose.ui.unit.IntSize(
                            it.width.toInt(),
                            it.height.toInt()
                        )
                    }) {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                }
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
                                style = MaterialTheme.typography.labelLarge,
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

            // Quick Actions Card
            if (!isCapturing) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    FilledTonalButton(
                        onClick = { /* TODO */ },
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Vouch")
                    }
                    FilledTonalButton(
                        onClick = { /* TODO */ },
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = "Auto-Settle Vouches"
                        )
                    }
                    FilledTonalButton(
                        onClick = { showSettleDialog = true },
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = "Settle Person"
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                isCapturing = true
                                kotlinx.coroutines.delay(100) // allow recomposition to hide the row
                                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                shareBitmap(context, bitmap, personName)
                                isCapturing = false
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Export Vouch-List"
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

        if (showSettleDialog) {
            AlertDialog(
                onDismissRequest = { showSettleDialog = false },
                title = { Text("Settle Person?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "All vouches with this person (given and received) will be settled.\n${
                                when (personData?.totalAmount?.signum()) {
                                    -1 -> "you currently owe $personName ${personData?.totalAmount?.abs()}€"
                                    1 -> "$personName currently owes you ${personData?.totalAmount?.abs()}€"
                                    0 -> ""
                                    else -> "something went wrong"
                                }
                            }.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fullySettle = !fullySettle }
                        ) {
                            Checkbox(
                                checked = fullySettle,
                                onCheckedChange = { fullySettle = it }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Difference has been paid")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSettleDialog = false
                            if (fullySettle) {
                                viewModel.settlePerson()
                            } else {
                                viewModel.netVouches()
                            }
                        }
                    ) {
                        Text("Settle")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showSettleDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun shareBitmap(context: Context, bitmap: Bitmap, personName: String) {
    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdirs()
    val file = File(cachePath, "vouch_summary.png")
    val fileOutputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
    fileOutputStream.flush()
    fileOutputStream.close()

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newUri(context.contentResolver, "Vouch Summary", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share Vouch Summary"))
}
