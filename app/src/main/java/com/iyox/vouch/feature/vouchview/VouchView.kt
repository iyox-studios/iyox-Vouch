package com.iyox.vouch.feature.vouchview

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.model.isSettled
import com.iyox.vouch.core.utils.rememberToastManager
import com.iyox.vouch.ui.components.PersonAvatar
import com.iyox.vouch.ui.components.VouchBackButton
import com.iyox.vouch.ui.components.vouches.GroupVouchFullItem
import com.iyox.vouch.ui.components.vouches.VouchSwipeToDismiss
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VouchView(
    vouchId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit = {},
    onNavigateToPerson: (String) -> Unit = {},
    viewModel: VouchViewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    VouchViewContent(
        uiState = uiState,
        sharedActions = viewModel.sharedActions,
        onNavigateBack = onNavigateBack,
        onNavigateToEdit = onNavigateToEdit,
        onNavigateToPerson = onNavigateToPerson
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VouchViewContent(
    uiState: VouchViewUiState,
    sharedActions: SharedVouchActions,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToPerson: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val toastManager = rememberToastManager()

    val msgVouchSettled = stringResource(R.string.msg_vouch_settled)
    val msgVouchUnsettled = stringResource(R.string.msg_vouch_unsettled)
    val msgVouchDeleted = stringResource(R.string.msg_vouch_deleted)
    val msgSettleFailed = stringResource(R.string.msg_settle_failed)
    val msgDeleteFailed = stringResource(R.string.msg_delete_failed)
    val actionUndo = stringResource(R.string.action_undo)
    val shareVouchLabel = stringResource(R.string.vouch_summary_label)
    val shareVouchTitle = stringResource(R.string.share_vouch_image_title)

    val exportLayer = rememberGraphicsLayer()
    var capturing by remember { mutableStateOf(false) }
    var hasNavigatedBack by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSettleConfirmDialog by remember { mutableStateOf(false) }

    val safeNavigateBack: () -> Unit = {
        if (!hasNavigatedBack) {
            hasNavigatedBack = true
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.isLoading, uiState.vouch) {
        if (!uiState.isLoading && uiState.vouch == null) {
            safeNavigateBack()
        }
    }

    if (showDeleteConfirmDialog && uiState.vouch != null) {
        val vouch = uiState.vouch
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(text = stringResource(R.string.title_delete_vouch_confirm)) },
            text = { Text(text = stringResource(R.string.msg_delete_vouch_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        safeNavigateBack()
                        toastManager.executeWithUndo(
                            message = msgVouchDeleted,
                            actionLabel = actionUndo,
                            errorMessage = msgDeleteFailed,
                            performAction = { sharedActions.deleteVouch(vouch.vouch.id) },
                            undoAction = { _: Unit -> sharedActions.restoreVouch(vouch) }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showSettleConfirmDialog && uiState.vouch != null) {
        val vouch = uiState.vouch
        val isSettled = vouch.isSettled()
        AlertDialog(
            onDismissRequest = { showSettleConfirmDialog = false },
            title = {
                Text(
                    text = if (isSettled) {
                        stringResource(R.string.title_unsettle_vouch_confirm)
                    } else {
                        stringResource(R.string.title_settle_vouch_confirm)
                    }
                )
            },
            text = {
                Text(
                    text = if (isSettled) {
                        stringResource(R.string.msg_unsettle_vouch_confirm)
                    } else {
                        stringResource(R.string.msg_settle_vouch_confirm)
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettleConfirmDialog = false
                        val message = if (isSettled) msgVouchUnsettled else msgVouchSettled
                        toastManager.executeWithUndo(
                            message = message,
                            actionLabel = actionUndo,
                            errorMessage = msgSettleFailed,
                            performAction = {
                                if (isSettled) sharedActions.unSettleVouch(vouch) else sharedActions.settleVouch(vouch)
                            },
                            undoAction = { _: Unit ->
                                if (isSettled) sharedActions.settleVouch(vouch) else sharedActions.unSettleVouch(vouch)
                            }
                        )
                    }
                ) {
                    Text(
                        text = if (isSettled) {
                            stringResource(R.string.action_unsettle)
                        } else {
                            stringResource(R.string.action_settle)
                        }
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSettleConfirmDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    VouchBackButton(onClick = safeNavigateBack)
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                CircularProgressIndicator()
            }
        } else {
            val vouch = uiState.vouch
            if (vouch != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    // 1. Non-scrollable Top Vouch Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        GroupVouchFullItem(
                            vouch = vouch,
                            canSettle = !vouch.isSettled(),
                            onSettle = { showSettleConfirmDialog = true },
                            onDelete = { showDeleteConfirmDialog = true },
                            onEdit = { onNavigateToEdit(vouch.vouch.id) },
                            onPersonClick = onNavigateToPerson
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Non-scrollable Shortcut Action Bar
                    VouchViewShortcutBar(
                        isSettled = vouch.isSettled(),
                        onSettleClick = { showSettleConfirmDialog = true },
                        onExportClick = {
                            coroutineScope.launch {
                                capturing = true
                                awaitFrame()
                                val bitmap = exportLayer.toImageBitmap().asAndroidBitmap()
                                shareVouchBitmap(
                                    context = context,
                                    bitmap = bitmap,
                                    vouchId = vouch.vouch.id,
                                    shareLabel = shareVouchLabel,
                                    shareTitle = shareVouchTitle
                                )
                                capturing = false
                            }
                        },
                        onEditClick = { onNavigateToEdit(vouch.vouch.id) },
                        onDeleteClick = { showDeleteConfirmDialog = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Scrollable Associated Vouches List (same padding as PersonScreen)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 0.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            vouch.persons,
                            key = { it.person.id }
                        ) { personWithAmount ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                VouchSwipeToDismiss(
                                    enableDismissFromStartToEnd = true,
                                    enableDismissFromEndToStart = false,
                                    onStartToEnd = {
                                        val isParticipantSettled = personWithAmount.settled
                                        val settleMessage = if (isParticipantSettled) msgVouchUnsettled else msgVouchSettled
                                        toastManager.executeWithUndo(
                                            message = settleMessage,
                                            actionLabel = actionUndo,
                                            errorMessage = msgSettleFailed,
                                            performAction = {
                                                sharedActions.setVouchPersonSettlement(
                                                    vouchId = vouch.vouch.id,
                                                    personId = personWithAmount.person.id,
                                                    settled = !isParticipantSettled
                                                )
                                            },
                                            undoAction = { _: Unit ->
                                                sharedActions.setVouchPersonSettlement(
                                                    vouchId = vouch.vouch.id,
                                                    personId = personWithAmount.person.id,
                                                    settled = isParticipantSettled
                                                )
                                            }
                                        )
                                    },
                                    settleText = if (personWithAmount.settled) {
                                        stringResource(R.string.action_unsettle)
                                    } else {
                                        stringResource(R.string.action_settle)
                                    },
                                    deleteText = ""
                                ) {
                                    GroupVouchParticipantItem(
                                        personWithAmount = personWithAmount,
                                        onSettleToggle = {
                                            val isParticipantSettled = personWithAmount.settled
                                            val settleMessage = if (isParticipantSettled) msgVouchUnsettled else msgVouchSettled
                                            toastManager.executeWithUndo(
                                                message = settleMessage,
                                                actionLabel = actionUndo,
                                                errorMessage = msgSettleFailed,
                                                performAction = {
                                                    sharedActions.setVouchPersonSettlement(
                                                        vouchId = vouch.vouch.id,
                                                        personId = personWithAmount.person.id,
                                                        settled = !isParticipantSettled
                                                    )
                                                },
                                                undoAction = { _: Unit ->
                                                    sharedActions.setVouchPersonSettlement(
                                                        vouchId = vouch.vouch.id,
                                                        personId = personWithAmount.person.id,
                                                        settled = isParticipantSettled
                                                    )
                                                }
                                            )
                                        },
                                        onPersonClick = onNavigateToPerson
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Offscreen bitmap render layer
        if (capturing && uiState.vouch != null) {
            Box(modifier = Modifier.absoluteOffset(x = (-10000).dp)) {
                Column(
                    modifier = Modifier
                        .wrapContentHeight(unbounded = true)
                        .background(MaterialTheme.colorScheme.surface)
                        .drawWithContent {
                            exportLayer.record {
                                this@drawWithContent.drawContent()
                            }
                        }
                ) {
                    SingleVouchExportTemplate(vouch = uiState.vouch)
                }
            }
        }
    }
}

@Composable
fun GroupVouchParticipantItem(
    personWithAmount: PersonWithAmount,
    onSettleToggle: () -> Unit,
    onPersonClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSettled = personWithAmount.settled
    val isSelf = personWithAmount.isSelf
    val cardModifier = if (isSelf) {
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
    } else {
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { onPersonClick(personWithAmount.person.name) }
    }

    Card(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                PersonAvatar(
                    name = personWithAmount.person.name,
                    size = 40.dp
                )
                if (isSettled) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.label_settled),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = personWithAmount.person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSettled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isSettled) {
                        stringResource(R.string.label_settled)
                    } else {
                        stringResource(R.string.filter_unsettled)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSettled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = CurrencyManager.formatAmountWithSymbol(personWithAmount.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSettled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isSettled) TextDecoration.LineThrough else TextDecoration.None
                )

                FilledTonalIconButton(
                    onClick = onSettleToggle,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (isSettled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        contentColor = if (isSettled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = if (isSettled) {
                            stringResource(R.string.action_unsettle)
                        } else {
                            stringResource(R.string.action_settle)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VouchViewShortcutBar(
    isSettled: Boolean,
    onSettleClick: () -> Unit,
    onExportClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Settle / Unsettle toggle button
        FilledTonalButton(
            onClick = onSettleClick,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = if (isSettled) Icons.Outlined.Archive else Icons.Filled.DoneAll,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(if (isSettled) R.string.action_unsettle else R.string.action_settle),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Export as Image
        FilledTonalButton(
            onClick = onExportClick,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(width = 44.dp, height = 44.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = stringResource(R.string.cd_export_vouch_image),
                modifier = Modifier.size(18.dp)
            )
        }

        // Edit Vouch
        FilledTonalButton(
            onClick = onEditClick,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(width = 44.dp, height = 44.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.cd_edit_vouch),
                modifier = Modifier.size(18.dp)
            )
        }

        // Delete Vouch
        FilledTonalButton(
            onClick = onDeleteClick,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(width = 44.dp, height = 44.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.cd_delete_vouch),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SingleVouchExportTemplate(
    vouch: VouchWithPerson,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat(stringResource(R.string.format_vouch_date), Locale.getDefault())
    val formattedDate = dateFormat.format(vouch.vouch.timestamp)
    val hasDescription = vouch.vouch.description.isNotBlank()
    val isGroup = vouch.persons.size > 1
    val isSettled = vouch.isSettled()

    Card(
        modifier = modifier
            .width(360.dp)
            .padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: App Branding + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(width = 18.dp, height = 14.dp)
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Direction + Amount + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (vouch.vouch.given) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (vouch.vouch.given) Icons.Outlined.ArrowOutward else Icons.Outlined.SouthWest,
                            contentDescription = null,
                            tint = if (vouch.vouch.given) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = CurrencyManager.formatAmountWithSymbol(vouch.vouch.amount),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (vouch.vouch.given) stringResource(R.string.label_given) else stringResource(R.string.label_received),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSettled) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.label_settled),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Description
            if (hasDescription) {
                Text(
                    text = vouch.vouch.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Single Vouch Person or Group Participants
            if (!isGroup) {
                val person = vouch.persons.firstOrNull()?.person
                if (person != null) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PersonAvatar(name = person.name, size = 32.dp)
                            Text(
                                text = person.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vouch.persons.forEach { participant ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PersonAvatar(name = participant.person.name, size = 28.dp)
                            Text(
                                text = participant.person.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = CurrencyManager.formatAmountWithSymbol(participant.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (participant.settled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (participant.settled) TextDecoration.LineThrough else TextDecoration.None
                            )
                            if (participant.settled) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shareVouchBitmap(
    context: Context,
    bitmap: Bitmap,
    vouchId: Long,
    shareLabel: String,
    shareTitle: String
) {
    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdirs()

    val fileName = "vouch_${vouchId}_${System.currentTimeMillis()}.png"
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
            shareLabel,
            uri
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            shareTitle
        )
    )

    bitmap.recycle()
}
