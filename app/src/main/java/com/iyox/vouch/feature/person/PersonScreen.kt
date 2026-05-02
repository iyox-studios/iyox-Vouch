package com.iyox.vouch.feature.person

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.R
import com.iyox.vouch.core.model.PersonWithTotal
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.utils.LocalSnackbarHostState
import com.iyox.vouch.core.utils.rememberUndoCoordinator
import com.iyox.vouch.ui.components.vouches.VouchItem
import com.iyox.vouch.ui.components.vouches.VouchList
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    personName: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditVouch: (Int) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: PersonViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val exportLayer = rememberGraphicsLayer()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHostState.current
    rememberUndoCoordinator()
    val msgPersonSettled = stringResource(R.string.msg_person_settled)
    val msgVouchesNetted = stringResource(R.string.msg_vouches_netted)
    val actionUndo = stringResource(R.string.action_undo)

    var showSettleDialog by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, personName) {
        val observer = LifecycleEventObserver { _, event ->
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
            TopAppBar(title = { Text(personName) }, navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            })
        }) { innerPadding ->
        if (state.isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                PersonInfoCard(
                    personData = state.personData,
                    amountGiven = state.amountGiven,
                    amountOwed = state.amountOwed
                )

                QuickActionsCard(onSettleClick = { showSettleDialog = true }, onShareClick = {
                    coroutineScope.launch {
                        capturing = true
                        awaitFrame()
                        val bitmap = exportLayer.toImageBitmap().asAndroidBitmap()
                        shareBitmap(context, bitmap, personName)
                        capturing = false
                    }
                })

                if (state.vouches.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.empty_person_vouches))
                    }
                } else {
                    VouchList(
                        displayedSorted = state.vouches,
                        listState = rememberLazyListState(),
                        innerPadding = PaddingValues(0.dp),
                        sharedVouchActions = viewModel.sharedActions,
                        onEdit = { onNavigateToEditVouch(it) })
                }
            }
        }

        if (capturing) {
            Box(modifier = Modifier.absoluteOffset(x = (-10000).dp)) {
                Column(
                    modifier = Modifier
                        .wrapContentHeight(unbounded = true)
                        .background(MaterialTheme.colorScheme.surface)
                        .drawWithContent {
                            exportLayer.record {
                                this@drawWithContent.drawContent()
                            }
                        }) {
                    VouchExportTemplate(
                        personName,
                        state.personData,
                        state.amountGiven,
                        state.amountOwed,
                        state.vouches
                    )
                }
            }
        }

        if (showSettleDialog) {
            SettlePersonDialog(
                personName = personName,
                personData = state.personData,
                onDismiss = { showSettleDialog = false },
                onConfirm = { fullySettle ->
                    showSettleDialog = false
                    coroutineScope.launch {
                        if (fullySettle) {
                            val settled = viewModel.settlePerson()
                            val result = snackbarHost.showSnackbar(msgPersonSettled, actionUndo)
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoSettlePerson(settled)
                            }
                        } else {
                            val (settled, nettingVouch) = viewModel.netVouches()
                            val result = snackbarHost.showSnackbar(msgVouchesNetted, actionUndo)
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoNetVouches(settled, nettingVouch)
                            }
                        }
                    }
                })
        }
    }
}

@Composable
fun PersonInfoCard(
    personData: PersonWithTotal?,
    amountGiven: BigDecimal,
    amountOwed: BigDecimal,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ), shape = MaterialTheme.shapes.large, modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val total = personData?.totalAmount?.toPlainString() ?: "0"
            val count = personData?.vouchCount ?: 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_total_balance),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape,
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.vouch_count,
                            count,
                            count
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = stringResource(R.string.amount_euro, total),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Medium,
                color = if ((personData?.totalAmount?.signum()
                        ?: 0) >= 0
                ) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        stringResource(R.string.label_given),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        stringResource(R.string.amount_euro, amountGiven.toPlainString()),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.label_received),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        stringResource(R.string.amount_euro, amountOwed.toPlainString()),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    onSettleClick: () -> Unit, onShareClick: () -> Unit, modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
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
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_add_vouch)
                )
            }
            FilledTonalButton(
                onClick = { /* TODO */ },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = stringResource(R.string.cd_auto_settle_vouches)
                )
            }
            FilledTonalButton(
                onClick = onSettleClick,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DoneAll,
                    contentDescription = stringResource(R.string.cd_settle_person)
                )
            }
            FilledTonalButton(
                onClick = onShareClick,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.cd_export_vouch_list)
                )
            }
        }
    }
}

@Composable
fun SettlePersonDialog(
    personName: String,
    personData: PersonWithTotal?,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var fullySettle by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_settle_person)) },
        text = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val detail = when (personData?.totalAmount?.signum()) {
                -1 -> stringResource(
                    R.string.settle_person_you_owe,
                    personName,
                    stringResource(
                        R.string.amount_euro,
                        personData.totalAmount.abs().toPlainString()
                    )
                )

                1 -> stringResource(
                    R.string.settle_person_owes_you,
                    personName,
                    stringResource(
                        R.string.amount_euro,
                        personData.totalAmount.abs().toPlainString()
                    )
                )

                0 -> ""
                else -> stringResource(R.string.settle_person_error)
            }
            val message = if (detail.isNotEmpty()) {
                stringResource(R.string.settle_person_message_with_detail, detail)
            } else {
                stringResource(R.string.settle_person_message_no_detail)
            }
            Text(text = message, style = MaterialTheme.typography.bodyLarge)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { fullySettle = !fullySettle }) {
                Checkbox(
                    checked = fullySettle, onCheckedChange = { fullySettle = !fullySettle }
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.label_difference_paid))
            }
        }
    }, confirmButton = {
        Button(onClick = { onConfirm(fullySettle) }) {
            Text(stringResource(R.string.action_settle))
        }
    }, dismissButton = {
        OutlinedButton(onClick = onDismiss) {
            Text(stringResource(R.string.action_cancel))
        }
    })
}

@Composable
fun VouchExportTemplate(
    personName: String,
    personData: PersonWithTotal?,
    amountGiven: BigDecimal,
    amountOwed: BigDecimal,
    vouches: List<Vouch>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(personName, style = MaterialTheme.typography.headlineMedium)
        PersonInfoCard(personData, amountGiven, amountOwed)
        Spacer(Modifier.height(16.dp))
        Column(modifier = Modifier.padding(16.dp)) {
            vouches.filter { vouch -> !vouch.settled }.forEach { vouch ->
                VouchItem(vouch, canSettle = false, onSettle = {}, onDelete = {}, onEdit = {})
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun shareBitmap(context: Context, bitmap: Bitmap, personName: String) {
    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdirs()

    val fileName = "vouch_${personName.replace(" ", "_")}.png"
    val file = File(cachePath, fileName)

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    val uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(
            context.contentResolver,
            context.getString(R.string.vouch_summary_label),
            uri
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            context.getString(R.string.share_vouch_summary_title)
        )
    )

    bitmap.recycle()
}
