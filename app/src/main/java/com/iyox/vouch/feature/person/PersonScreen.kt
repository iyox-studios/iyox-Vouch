package com.iyox.vouch.feature.person

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.model.VouchTypeFilter
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.model.isSettledForPerson
import com.iyox.vouch.core.utils.rememberToastManager
import com.iyox.vouch.ui.components.EmptySearchContent
import com.iyox.vouch.ui.components.PersonAvatar
import com.iyox.vouch.ui.components.RenamePersonDialog
import com.iyox.vouch.ui.components.VouchBackButton
import com.iyox.vouch.ui.components.VouchBalanceHeroCard
import com.iyox.vouch.ui.components.VouchBalanceHeroDecoration
import com.iyox.vouch.ui.components.VouchFilterRow
import com.iyox.vouch.ui.components.vouches.VouchList
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import kotlin.math.abs
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PersonScreen(
    personName: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddVouch: (String) -> Unit = {},
    onVouchClick: ((Long, Boolean) -> Unit)? = null,
    onNavigateToPerson: (String) -> Unit = {},
    onNavigateToSettledSpace: (String) -> Unit = {},
    onNavigateToEditVouch: (Long, Boolean) -> Unit = { _, _ -> },
    settleAllOnOpen: Boolean = false,
    shareOnOpen: Boolean = false
) {
    val handleVouchClick = onVouchClick ?: onNavigateToEditVouch
    val viewModel: PersonViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val exportLayer = rememberGraphicsLayer()

    val currentPersonName = state.personData?.person?.name ?: personName

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val toastManager = rememberToastManager()
    val msgPersonSettled = stringResource(R.string.msg_person_settled)
    val msgPersonRenamed = stringResource(R.string.msg_person_renamed)
    val msgRenameFailed = stringResource(R.string.msg_rename_failed)
    val msgPersonAlreadyExists = stringResource(R.string.error_person_already_exists)
    val msgVouchesNetted = stringResource(R.string.msg_vouches_netted)
    val nettingDescription = stringResource(R.string.vouch_description_netting)
    val msgSettleFailed = stringResource(R.string.msg_settle_failed)
    val actionUndo = stringResource(R.string.action_undo)
    val vouchSummaryLabel = stringResource(R.string.vouch_summary_label)
    val shareVouchSummaryTitle = stringResource(R.string.share_vouch_summary_title)
    val msgNoActiveVouches = stringResource(R.string.msg_no_active_vouches_to_settle)

    var showSettleDialog by rememberSaveable(personName) { mutableStateOf(settleAllOnOpen) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameErrorMessage by remember { mutableStateOf<String?>(null) }
    var capturing by remember { mutableStateOf(false) }
    var shareOnOpenHandled by rememberSaveable(personName) { mutableStateOf(false) }

    suspend fun sharePersonSummary() {
        capturing = true
        try {
            // Allow the export content to compose and draw before reading its graphics layer.
            awaitFrame()
            awaitFrame()
            val bitmap = exportLayer.toImageBitmap().asAndroidBitmap()
            shareBitmap(
                context = context,
                bitmap = bitmap,
                personName = currentPersonName,
                vouchSummaryLabel = vouchSummaryLabel,
                shareVouchSummaryTitle = shareVouchSummaryTitle
            )
        } catch (_: Exception) {
            toastManager.showError(R.string.msg_share_failed)
        } finally {
            capturing = false
        }
    }

    LaunchedEffect(shareOnOpen, state.isLoading) {
        if (shareOnOpen && !state.isLoading && !shareOnOpenHandled) {
            shareOnOpenHandled = true
            sharePersonSummary()
        }
    }

    val listState = remember(state.showOnlyGiven, state.sortedBy) {
        LazyListState()
    }

    LaunchedEffect(personName) {
        viewModel.loadPerson(personName)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.size(48.dp))
                        Text(
                            text = currentPersonName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        IconButton(
                            onClick = {
                                renameErrorMessage = null
                                showRenameDialog = true
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(R.string.cd_rename_person)
                            )
                        }
                    }
                },
                navigationIcon = {
                    VouchBackButton(onClick = onNavigateBack)
                },
                actions = {
                    Spacer(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .width(ButtonDefaults.MinWidth)
                    )
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                PersonInfoCard(
                    personData = state.personData,
                    amountGiven = state.amountGiven,
                    amountOwed = state.amountOwed,
                    vouchCount = state.personData?.vouchCount ?: state.vouches.size
                )

                QuickActionsCard(
                    onSettleClick = { showSettleDialog = true },
                    onAddVouch = {
                        onNavigateToAddVouch(currentPersonName)
                    },
                    onSettledSpaceClick = {
                        onNavigateToSettledSpace(currentPersonName)
                    },
                    onShareClick = {
                        coroutineScope.launch {
                            sharePersonSummary()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(22.dp))

                VouchFilterRow(
                    sortedBy = state.sortedBy,
                    showOnlyGiven = state.showOnlyGiven,
                    onToggleSort = viewModel::toggleSortedBy,
                    onToggleGiven = viewModel::toggleShowOnlyGiven,
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    searchPlaceholder = stringResource(R.string.search_person_vouches_placeholder),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 6.dp)
                )

                if (state.vouches.isEmpty()) {
                    if (state.searchQuery.isNotBlank()) {
                        EmptySearchContent(
                            searchQuery = state.searchQuery,
                            onClearSearch = { viewModel.setSearchQuery("") }
                        )
                    } else if (state.settledCount > 0 && state.showOnlyGiven == VouchTypeFilter.ALL) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = MaterialShapes.Cookie9Sided.toShape(),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(88.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_archive),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.all_settled_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.all_settled_subtitle, currentPersonName),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                OutlinedButton(
                                    onClick = { onNavigateToSettledSpace(currentPersonName) },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_archive),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.action_view_settled_history,
                                            state.settledCount
                                        ),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.empty_person_vouches))
                        }
                    }
                } else {
                    VouchList(
                        displayedSorted = state.vouches,
                        listState = listState,
                        innerPadding = PaddingValues(0.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 0.dp,
                            bottom = 24.dp
                        ),
                        sharedVouchActions = viewModel.sharedActions,
                        onEdit = handleVouchClick,
                        onNavigateToPerson = onNavigateToPerson,
                        isSettled = { vouch ->
                            state.personData?.person?.id?.let { pid ->
                                vouch.isSettledForPerson(pid)
                            } ?: vouch.isSettledForPerson(currentPersonName)
                        },
                        groupSingleContextPersonId = state.personData?.person?.id
                    )
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
                        currentPersonName,
                        state.personData,
                        state.amountGiven,
                        state.amountOwed,
                        state.vouches
                    )
                }
            }
        }

        if (showRenameDialog) {
            RenamePersonDialog(
                currentName = currentPersonName,
                errorMessage = renameErrorMessage,
                onDismiss = {
                    showRenameDialog = false
                    renameErrorMessage = null
                },
                onConfirm = { newName ->
                    coroutineScope.launch {
                        val result = viewModel.renamePerson(newName)
                        if (result.isSuccess) {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            showRenameDialog = false
                            renameErrorMessage = null
                            toastManager.show(msgPersonRenamed)
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.Reject)
                            val error = result.exceptionOrNull()
                            renameErrorMessage = if (error is IllegalStateException) {
                                msgPersonAlreadyExists
                            } else {
                                msgRenameFailed
                            }
                        }
                    }
                }
            )
        }

        if (showSettleDialog && !state.isLoading) {
            SettlePersonDialog(
                personName = currentPersonName,
                personData = state.personData,
                onDismiss = { showSettleDialog = false },
                onConfirm = { fullySettle ->
                    showSettleDialog = false
                    if (state.vouches.isEmpty()) {
                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                        toastManager.show(msgNoActiveVouches)
                        return@SettlePersonDialog
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    if (fullySettle) {
                        toastManager.executeWithUndo(
                            message = msgPersonSettled,
                            actionLabel = actionUndo,
                            errorMessage = msgSettleFailed,
                            performAction = { viewModel.settlePerson() },
                            undoAction = { settled ->
                                viewModel.undoSettlePerson(settled)
                            }
                        )
                    } else {
                        toastManager.executeWithUndo(
                            message = msgVouchesNetted,
                            actionLabel = actionUndo,
                            errorMessage = msgSettleFailed,
                            performAction = { viewModel.netVouches(nettingDescription) },
                            undoAction = { result ->
                                result?.let { (settled, nettingVouch) ->
                                    viewModel.undoNetVouches(settled, nettingVouch)
                                }
                            }
                        )
                    }
                })
        }
    }
}

@Composable
fun PersonInfoCard(
    personData: PersonTotal?,
    amountGiven: Long,
    amountOwed: Long,
    modifier: Modifier = Modifier,
    vouchCount: Int = personData?.vouchCount ?: 0
) {
    VouchBalanceHeroCard(
        amountGiven = amountGiven,
        amountOwed = amountOwed,
        total = personData?.amount ?: (amountGiven - amountOwed),
        vouchCount = vouchCount,
        decoration = VouchBalanceHeroDecoration.PentagonBottomStart,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
    )
}

@Composable
fun QuickActionsCard(
    onSettleClick: () -> Unit,
    onShareClick: () -> Unit,
    onAddVouch: () -> Unit,
    modifier: Modifier = Modifier,
    onSettledSpaceClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onSettledSpaceClick()
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_archive),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.action_view_settled_space),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FilledTonalButton(
            onClick = onSettleClick,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_done_all),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.action_settle),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FilledTonalButton(
            onClick = onShareClick,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(width = 44.dp, height = 44.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = stringResource(R.string.cd_export_vouch_list),
                modifier = Modifier.size(18.dp)
            )
        }
        Button(
            onClick = onAddVouch,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(width = 44.dp, height = 44.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.cd_add_vouch),
                modifier = Modifier.size(20.dp)
            )
        }

    }
}

@Composable
fun SettlePersonDialog(
    personName: String,
    personData: PersonTotal?,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var fullySettle by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_settle_person)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val totalAmount = personData?.amount ?: 0L
                val detail = when {
                    totalAmount < 0L -> stringResource(
                        R.string.settle_person_you_owe,
                        personName,
                        CurrencyManager.formatAmountWithSymbol(abs(totalAmount))
                    )

                    totalAmount > 0L -> stringResource(
                        R.string.settle_person_owes_you,
                        personName,
                        CurrencyManager.formatAmountWithSymbol(abs(totalAmount))
                    )

                    else -> ""
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
    personData: PersonTotal?,
    amountGiven: Long,
    amountOwed: Long,
    vouches: List<VouchWithPerson>,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat(stringResource(R.string.format_vouch_date), LocalLocale.current.platformLocale)
    val totalAmount = personData?.amount ?: (amountGiven - amountOwed)
    val activeVouches = vouches.filter { !it.isSettledForPerson(personName) }

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
            // Header: App Branding + Export Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
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
                    text = dateFormat.format(System.currentTimeMillis()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Person Hero: Avatar + Name + Net Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PersonAvatar(name = personName, size = 48.dp)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = personName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (totalAmount == 0L) {
                        Text(
                            text = stringResource(R.string.label_settled),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = CurrencyManager.formatAmountWithSymbol(abs(totalAmount)),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.Bold,
                    color = when {
                        totalAmount > 0L -> MaterialTheme.colorScheme.primary
                        totalAmount < 0L -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            }


            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Active Vouches List
            if (activeVouches.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.all_settled_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activeVouches.forEach { vouch ->
                        val personShare = vouch.persons.firstOrNull {
                            it.person.name == personName || it.person.id == personData?.person?.id
                        }?.amount ?: vouch.vouch.amount
                        val isGiven = vouch.vouch.given
                        val vouchDate = dateFormat.format(vouch.vouch.timestamp)
                        val description = vouch.vouch.description.ifBlank {
                            stringResource(R.string.label_no_description)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (isGiven) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(if (isGiven) R.drawable.ic_arrow_outward else R.drawable.ic_south_west),
                                        contentDescription = null,
                                        tint = if (isGiven) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = vouchDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Text(
                                text = CurrencyManager.formatAmountWithSymbol(personShare),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun shareBitmap(
    context: Context,
    bitmap: Bitmap,
    personName: String,
    vouchSummaryLabel: String,
    shareVouchSummaryTitle: String
) {
    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdirs()

    val fileName = "vouch_${personName.replace(" ", "_")}_${System.currentTimeMillis()}.png"
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
            vouchSummaryLabel,
            uri
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            shareVouchSummaryTitle
        )
    )

    bitmap.recycle()
}
