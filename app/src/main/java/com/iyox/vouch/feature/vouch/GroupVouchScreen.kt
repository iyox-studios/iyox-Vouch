package com.iyox.vouch.feature.vouch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.utils.rememberToastManager
import com.iyox.vouch.ui.components.PersonAvatar
import com.iyox.vouch.ui.components.VouchBackButton
import com.iyox.vouch.ui.components.vouches.GroupSplitSection
import com.iyox.vouch.ui.components.vouches.GroupVouchSplitSlider
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupVouchScreen(
    vouchId: Long? = null,
    edit: Boolean = false,
    onNavigateBack: () -> Unit,
    viewModel: GroupVouchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val toastManager = rememberToastManager()
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val amountFocusRequester = remember { FocusRequester() }
    val msgGroupVouchCreated = stringResource(R.string.msg_group_vouch_created)
    val msgSaveFailed = stringResource(R.string.msg_save_failed)
    val actionUndo = stringResource(R.string.action_undo)

    LaunchedEffect(vouchId) {
        if (vouchId != null) {
            viewModel.loadVouch(vouchId)
        }
    }

    BackHandler(enabled = uiState.step == STEP_MEMBERS) {
        viewModel.backToDetails()
    }

    LaunchedEffect(uiState.step, uiState.isLoading) {
        if (uiState.step == STEP_DETAILS && !uiState.isLoading && !edit) {
            try {
                amountFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessageRes) {
        uiState.errorMessageRes?.let { resId ->
            toastManager.showError(resId)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                actions = {
                    if (uiState.step == STEP_DETAILS) {
                        Button(
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = {
                                viewModel.continueToMembers()
                                if (viewModel.uiState.value.amountError != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                    try {
                                        amountFocusRequester.requestFocus()
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.action_continue))
                        }
                    } else {
                        val totalAmountMinor = CurrencyManager.parseToMinorUnits(uiState.amount) ?: 0L
                        val sumAssignedMinor = uiState.members.sumOf { CurrencyManager.parseToMinorUnits(it.amount) ?: 0L }
                        val isOverallocated = totalAmountMinor > 0L && sumAssignedMinor > totalAmountMinor
                        val hasInvalidMemberAmount = uiState.members.any { CurrencyManager.parseToMinorUnits(it.amount) == null }
                        val canSave = uiState.members.count { !it.isSelf } >= 2 && !isOverallocated && !hasInvalidMemberAmount && totalAmountMinor > 0L

                        Button(
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = {
                                if (!edit) {
                                    toastManager.executeWithUndo(
                                        message = msgGroupVouchCreated,
                                        actionLabel = actionUndo,
                                        errorMessage = msgSaveFailed,
                                        performAction = { viewModel.saveGroupVouchesDirect() },
                                        undoAction = { savedVouch ->
                                            if (savedVouch != null) {
                                                viewModel.sharedActions.deleteVouch(savedVouch.vouch.id)
                                            }
                                        }
                                    )
                                } else {
                                    viewModel.saveGroupVouches()
                                }
                            },
                            enabled = canSave
                        ) {
                            Text(
                                if (edit) stringResource(R.string.action_update) else stringResource(
                                    R.string.action_save
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    VouchBackButton(
                        onClick = {
                            if (uiState.step == STEP_MEMBERS) {
                                viewModel.backToDetails()
                            } else {
                                onNavigateBack()
                            }
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && edit) {
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.step == STEP_DETAILS) {
                    VouchAmountHeader(
                        amount = uiState.amount,
                        onAmountChange = viewModel::onAmountChange,
                        focusManager = focusManager,
                        amountFocusRequester = amountFocusRequester,
                        isError = uiState.amountError != null,
                        errorMessage = uiState.amountError?.let { stringResource(it) }
                    )

                    VouchDirectionToggle(
                        isGiven = uiState.given,
                        onToggle = viewModel::onGivenChange
                    )

                    VouchDescriptionField(
                        value = uiState.description,
                        onValueChange = viewModel::onDescriptionChange,
                        focusManager = focusManager
                    )

                    VouchDateField(
                        timestamp = uiState.timestamp,
                        onDateSelected = viewModel::onDateChange
                    )

                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    GroupVouchSummaryCard(
                        amount = uiState.amount,
                        isGiven = uiState.given,
                        timestamp = uiState.timestamp,
                        members = uiState.members,
                        onResetAllToEqual = viewModel::resetAllToEqualSplit
                    )

                    GroupMemberSelector(
                        query = uiState.personQuery,
                        results = uiState.personSearchResults,
                        suggestedPersons = uiState.suggestedPersons,
                        currentMembers = uiState.members,
                        onQueryChange = viewModel::onPersonQueryChange,
                        onPersonSelected = viewModel::onPersonSelected,
                        onAddPerson = viewModel::addMemberFromQuery,
                        onAddSelf = viewModel::addSelfMember,
                        focusManager = focusManager
                    )

                    val totalAmountMinor = CurrencyManager.parseToMinorUnits(uiState.amount) ?: 0L

                    GroupMemberList(
                        members = uiState.members,
                        totalAmountMinor = totalAmountMinor,
                        onAmountChange = viewModel::onMemberAmountChange,
                        onResetMember = viewModel::resetMemberToEqualSplit,
                        onRemove = viewModel::removeMember,
                        focusManager = focusManager,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupVouchSummaryCard(
    amount: String,
    isGiven: Boolean,
    members: List<GroupMemberInput>,
    onResetAllToEqual: () -> Unit,
    modifier: Modifier = Modifier,
    timestamp: Long = System.currentTimeMillis()
) {
    val totalAmountMinor = CurrencyManager.parseToMinorUnits(amount) ?: 0L
    val sumAssignedMinor = members.sumOf { CurrencyManager.parseToMinorUnits(it.amount) ?: 0L }
    val remainingMinor = totalAmountMinor - sumAssignedMinor
    val hasCustomSplit = members.any { it.isCustom }
    val isBalanced = remainingMinor == 0L && members.isNotEmpty()
    val dateFormat =
        SimpleDateFormat(stringResource(R.string.format_vouch_date), Locale.getDefault())
    val formattedDate = dateFormat.format(timestamp)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.largeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isGiven) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isGiven) Icons.Outlined.ArrowOutward else Icons.Outlined.SouthWest,
                                contentDescription = null,
                                tint = if (isGiven) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = stringResource(R.string.group_vouch_split_summary).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = CurrencyManager.formatAmountWithSymbol(totalAmountMinor),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (members.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.person_count,
                                        members.size,
                                        members.size
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            val fillColor = when {
                remainingMinor < 0L -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
            val activeMembers = members.mapNotNull { member ->
                val memberAmount = CurrencyManager.parseToMinorUnits(member.amount) ?: 0L
                if (memberAmount > 0L) member to memberAmount else null
            }
            val sections = mutableListOf<GroupSplitSection>()
            for ((member, amount) in activeMembers) {
                val memberLabel = if (member.isSelf) {
                    stringResource(R.string.group_vouch_self_member)
                } else {
                    member.personName
                }
                sections.add(
                    GroupSplitSection(
                        amount = amount,
                        color = fillColor,
                        label = memberLabel
                    )
                )
            }
            if (remainingMinor > 0L) {
                sections.add(GroupSplitSection(amount = remainingMinor, color = trackColor))
            }

            GroupVouchSplitSlider(
                totalAmountMinor = totalAmountMinor,
                sections = sections
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (members.isEmpty()) {
                    Text(
                        text = stringResource(R.string.empty_group_members),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isBalanced) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.group_vouch_balanced),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (remainingMinor > 0L) {
                    Text(
                        text = stringResource(
                            R.string.group_vouch_remaining,
                            CurrencyManager.formatAmountWithSymbol(remainingMinor)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSystemInDarkTheme()) Color(0xFFFFC107) else Color(0xFFB45309),
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.group_vouch_overallocated,
                            CurrencyManager.formatAmountWithSymbol(abs(remainingMinor))
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (hasCustomSplit && members.isNotEmpty()) {
                    AssistChip(
                        onClick = onResetAllToEqual,
                        label = {
                            Text(
                                text = stringResource(R.string.group_vouch_reset_equal),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        shape = CircleShape,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupMemberSelector(
    query: String,
    results: List<PersonTotal>,
    suggestedPersons: List<PersonTotal>,
    currentMembers: List<GroupMemberInput>,
    onQueryChange: (String) -> Unit,
    onPersonSelected: (Person) -> Unit,
    onAddPerson: () -> Unit,
    onAddSelf: (String) -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier
) {
    val currentMemberNames = remember(currentMembers) {
        currentMembers.map { it.personName.trim().lowercase() }.toSet()
    }

    val filteredResults = remember(results, currentMemberNames) {
        results.filter { it.person.name.trim().lowercase() !in currentMemberNames }
    }

    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(filteredResults) {
        expanded = filteredResults.isNotEmpty()
    }

    val isSelfAdded = currentMembers.any { it.isSelf }
    val selfLabel = stringResource(R.string.group_vouch_self_member)

    val availableSuggestions = remember(suggestedPersons, currentMemberNames) {
        suggestedPersons
            .filter { it.person.name.trim().lowercase() !in currentMemberNames }
            .sortedByDescending { it.vouchCount }
            .take(5)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    onQueryChange(it)
                    expanded = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                label = { Text(stringResource(R.string.label_person)) },
                leadingIcon = {
                    Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (query.isNotBlank()) {
                                onAddPerson()
                                focusManager.clearFocus()
                            }
                        },
                        enabled = query.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.action_add_person),
                            tint = if (query.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            )
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (query.isNotBlank()) {
                            onAddPerson()
                        }
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            ExposedDropdownMenu(
                expanded = expanded && filteredResults.isNotEmpty(),
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                filteredResults.forEach { searchResult ->
                    DropdownMenuItem(
                        text = {
                            PersonListItem(
                                name = searchResult.person.name,
                                totalAmount = searchResult.amount
                            )
                        },
                        onClick = {
                            onPersonSelected(searchResult.person)
                            expanded = false
                            focusManager.clearFocus()
                        },
                        contentPadding = PaddingValues(0.dp)
                    )
                }
            }
        }

        if (query.isBlank()) {
            GroupMemberSuggestionsRow(
                isSelfAdded = isSelfAdded,
                selfLabel = selfLabel,
                availableSuggestions = availableSuggestions,
                onAddSelf = onAddSelf,
                onPersonSelected = onPersonSelected,
                focusManager = focusManager
            )
        }
    }
}

@Composable
fun GroupMemberSuggestionsRow(
    isSelfAdded: Boolean,
    selfLabel: String,
    availableSuggestions: List<PersonTotal>,
    onAddSelf: (String) -> Unit,
    onPersonSelected: (Person) -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier
) {
    if (!isSelfAdded || availableSuggestions.isNotEmpty()) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isSelfAdded) {
                SuggestionChip(
                    onClick = {
                        onAddSelf(selfLabel)
                        focusManager.clearFocus()
                    },
                    label = {
                        Text(
                            text = selfLabel,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = MaterialTheme.shapes.medium
                )

                if (availableSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(22.dp)
                            .background(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            if (availableSuggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    availableSuggestions.forEach { personTotal ->
                        SuggestionChip(
                            onClick = {
                                onPersonSelected(personTotal.person)
                                focusManager.clearFocus()
                            },
                            label = {
                                Text(
                                    text = personTotal.person.name,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupMemberList(
    modifier: Modifier = Modifier,
    members: List<GroupMemberInput>,
    totalAmountMinor: Long = 0L,
    onAmountChange: (GroupMemberInput, String) -> Unit,
    onResetMember: (GroupMemberInput) -> Unit,
    onRemove: (GroupMemberInput) -> Unit,
    focusManager: FocusManager
) {
    val currency = CurrencyManager.getCurrency()

    if (members.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
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
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.GroupAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.group_vouch_empty_members_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.group_vouch_empty_members_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(members, key = { if (it.isSelf) "__self__" else it.personName }) { member ->
            val memberAmountMinor = CurrencyManager.parseToMinorUnits(member.amount)
            val isError = (member.amount.isNotBlank() && memberAmountMinor == null) ||
                (memberAmountMinor != null && totalAmountMinor > 0L && memberAmountMinor > totalAmountMinor)

            GroupMemberCard(
                member = member,
                currencySymbol = currency.symbol,
                isError = isError,
                onAmountChange = { onAmountChange(member, it) },
                onResetToEqual = { onResetMember(member) },
                onRemove = { onRemove(member) },
                focusManager = focusManager,
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun GroupMemberCard(
    modifier: Modifier = Modifier,
    member: GroupMemberInput,
    currencySymbol: String,
    isError: Boolean = false,
    onAmountChange: (String) -> Unit,
    onResetToEqual: () -> Unit,
    onRemove: () -> Unit,
    focusManager: FocusManager
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PersonAvatar(name = member.personName, size = 36.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.personName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (member.isCustom) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable { onResetToEqual() }
                            .padding(top = 2.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.group_vouch_custom_split),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = stringResource(R.string.group_vouch_reset_equal),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.group_vouch_equal_split),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = member.amount,
                onValueChange = onAmountChange,
                isError = isError,
                supportingText = if (isError) {
                    {
                        val parsed = CurrencyManager.parseToMinorUnits(member.amount)
                        val errText = if (parsed == null) {
                            stringResource(R.string.error_invalid_member_amount)
                        } else {
                            stringResource(R.string.error_group_overallocated)
                        }
                        Text(
                            errText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else null,
                modifier = Modifier.width(135.dp),
                prefix = {
                    Text(
                        text = currencySymbol,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
