package com.iyox.vouch.feature.vouch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.utils.LocalSnackbarHostState
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val STEP_DETAILS = 1
private const val STEP_MEMBERS = 2

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupVouchScreen(
    viewModel: GroupVouchViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    val focusManager = LocalFocusManager.current
    val amountFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = uiState.step == STEP_MEMBERS) {
        viewModel.backToDetails()
    }

    LaunchedEffect(uiState.step) {
        if (uiState.step == STEP_DETAILS) {
            amountFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(uiState.error) {
        val message = uiState.error
        if (message != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.step == STEP_DETAILS) {
                        Text(
                            text = stringResource(R.string.title_add_vouch)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.title_distribution)
                        )
                    }
                },
                actions = {
                    if (uiState.step == STEP_DETAILS) {
                        Button(
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = viewModel::continueToMembers
                        ) {
                            Text(stringResource(R.string.action_continue))
                        }
                    } else {
                        Button(
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = viewModel::saveGroupVouches,
                            enabled = uiState.members.isNotEmpty()
                        ) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.step == STEP_MEMBERS) {
                                viewModel.backToDetails()
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.step == STEP_DETAILS) {
                GroupVouchAmountHeader(
                    amount = uiState.amount,
                    onAmountChange = viewModel::onAmountChange,
                    focusManager = focusManager,
                    amountFocusRequester = amountFocusRequester
                )

                GroupVouchDirectionToggle(
                    isGiven = uiState.given,
                    onToggle = viewModel::onGivenChange
                )

                GroupVouchDescriptionField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    focusManager = focusManager
                )

                Spacer(modifier = Modifier.weight(1f))
            } else {
                GroupVouchSummaryCard(
                    amount = uiState.amount,
                    isGiven = uiState.given,
                    description = uiState.description,
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
                    focusManager = focusManager
                )

                GroupMemberList(
                    members = uiState.members,
                    onAmountChange = viewModel::onMemberAmountChange,
                    onResetMember = viewModel::resetMemberToEqualSplit,
                    onRemove = viewModel::removeMember,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GroupVouchSummaryCard(
    amount: String,
    isGiven: Boolean,
    description: String,
    members: List<GroupMemberInput>,
    onResetAllToEqual: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAmountMinor = CurrencyManager.parseToMinorUnits(amount) ?: 0L
    val sumAssignedMinor = members.sumOf { CurrencyManager.parseToMinorUnits(it.amount) ?: 0L }
    val remainingMinor = totalAmountMinor - sumAssignedMinor
    val hasCustomSplit = members.any { it.isCustom }
    val isBalanced = remainingMinor == 0L && members.isNotEmpty()

    val progressRatio = if (totalAmountMinor > 0) {
        (sumAssignedMinor.toFloat() / totalAmountMinor.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

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
                Column {
                    Text(
                        text = stringResource(R.string.group_vouch_split_summary).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = CurrencyManager.formatAmountWithSymbol(totalAmountMinor),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isGiven) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isGiven) Icons.Outlined.ArrowOutward else Icons.Outlined.SouthWest,
                            contentDescription = null,
                            tint = if (isGiven) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isGiven) stringResource(R.string.label_given) else stringResource(
                                R.string.label_received
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isGiven) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            if (description.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progressRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = when {
                    isBalanced -> MaterialTheme.colorScheme.primary
                    remainingMinor < 0L -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
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
                } else if (members.isNotEmpty()) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.person_count,
                            members.size,
                            members.size
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
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
    focusManager: FocusManager,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(results) {
        expanded = results.isNotEmpty()
    }

    val availableSuggestions = remember(suggestedPersons, currentMembers) {
        val currentNames = currentMembers.map { it.personName.lowercase() }.toSet()
        suggestedPersons
            .filter { it.person.name.lowercase() !in currentNames }
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
                            focusManager.clearFocus()
                        }
                    }
                ),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            ExposedDropdownMenu(
                expanded = expanded && results.isNotEmpty(),
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                results.forEach { searchResult ->
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

        if (query.isBlank() && availableSuggestions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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

@Composable
private fun PersonListItem(
    name: String,
    totalAmount: Long,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(name, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = {
            PersonAvatar(name = name, size = 32.dp)
        },
        trailingContent = {
            if (totalAmount != 0L) {
                Text(
                    text = CurrencyManager.formatAmountWithSymbol(totalAmount),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun GroupMemberList(
    members: List<GroupMemberInput>,
    onAmountChange: (String, String) -> Unit,
    onResetMember: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
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
                    shape = CircleShape,
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
        items(members, key = { it.personName }) { member ->
            GroupMemberCard(
                member = member,
                currencySymbol = currency.symbol,
                onAmountChange = { onAmountChange(member.personName, it) },
                onResetToEqual = { onResetMember(member.personName) },
                onRemove = { onRemove(member.personName) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun GroupMemberCard(
    member: GroupMemberInput,
    currencySymbol: String,
    onAmountChange: (String) -> Unit,
    onResetToEqual: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
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
                modifier = Modifier.width(115.dp),
                prefix = {
                    Text(
                        text = currencySymbol,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
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

@Composable
private fun PersonAvatar(
    name: String,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun GroupVouchAmountHeader(
    amount: String,
    onAmountChange: (String) -> Unit,
    focusManager: FocusManager,
    amountFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val currency = CurrencyManager.getCurrency()
        Text(
            text = currency.symbol,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(amountFocusRequester),
            textStyle = MaterialTheme.typography.displayLarge,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) }
            ),
            singleLine = true,
            placeholder = {
                Text(
                    stringResource(R.string.placeholder_amount),
                    style = MaterialTheme.typography.displayLarge
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GroupVouchDirectionToggle(
    isGiven: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val givenLabel = stringResource(R.string.label_given)
        val receivedLabel = stringResource(R.string.label_received)
        listOf(true to givenLabel, false to receivedLabel).forEach { (type, label) ->
            val isSelected = isGiven == type
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onToggle(type) },
                modifier = Modifier.weight(1f),
                colors = ToggleButtonDefaults.toggleButtonColors(),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun GroupVouchDescriptionField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    focusManager: FocusManager
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.label_description)) },
        leadingIcon = {
            Icon(
                Icons.Filled.Description,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
            }
        ),
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}
