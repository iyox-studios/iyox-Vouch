package com.iyox.vouch.feature.vouch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.utils.LocalSnackbarHostState
import kotlinx.coroutines.launch

private const val STEP_DETAILS = 1
private const val STEP_MEMBERS = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupVouchScreen(
    viewModel: GroupVouchViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    val focusManager = LocalFocusManager.current

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
                title = { Text(stringResource(R.string.title_add_group_vouch)) },
                actions = {
                    if (uiState.step == STEP_DETAILS) {
                        TextButton(
                            modifier = Modifier.padding(end = 8.dp),
                            onClick = viewModel::continueToMembers
                        ) {
                            Text(stringResource(R.string.action_continue))
                        }
                    } else {
                        TextButton(
                            modifier = Modifier.padding(end = 8.dp),
                            onClick = viewModel::saveGroupVouches
                        ) {
                            Text(stringResource(R.string.action_create))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                    focusManager = focusManager
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
                GroupMemberSelector(
                    query = uiState.personQuery,
                    results = uiState.personSearchResults,
                    onQueryChange = viewModel::onPersonQueryChange,
                    onPersonSelected = viewModel::onPersonSelected,
                    onAddPerson = viewModel::addMemberFromQuery,
                    focusManager = focusManager
                )

                GroupMemberList(
                    members = uiState.members,
                    onAmountChange = viewModel::onMemberAmountChange,
                    onRemove = viewModel::removeMember
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupMemberSelector(
    query: String,
    results: List<PersonTotal>,
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

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onAddPerson()
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            ExposedDropdownMenu(
                expanded = expanded && results.isNotEmpty(),
                onDismissRequest = { expanded = false }
            ) {
                results.forEach { result ->
                    DropdownMenuItem(
                        text = {
                            Text(result.person.name)
                        },
                        onClick = {
                            onPersonSelected(result.person)
                            expanded = false
                            focusManager.clearFocus()
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    )
                }
            }
        }

        IconButton(onClick = onAddPerson) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.action_add_person)
            )
        }
    }
}

@Composable
private fun GroupMemberList(
    members: List<GroupMemberInput>,
    onAmountChange: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (members.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth()) {
            Text(stringResource(R.string.empty_group_members))
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(members, key = { it.personName }) { member ->
            ListItem(
                headlineContent = { Text(member.personName) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = member.amount,
                            onValueChange = { onAmountChange(member.personName, it) },
                            modifier = Modifier.width(110.dp),
                            label = { Text(stringResource(R.string.label_amount)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true
                        )
                        IconButton(onClick = { onRemove(member.personName) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete)
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@Composable
private fun GroupVouchAmountHeader(
    amount: String,
    onAmountChange: (String) -> Unit,
    focusManager: FocusManager,
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
            modifier = Modifier.weight(1f),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
            androidx.compose.material3.ToggleButton(
                checked = isSelected,
                onCheckedChange = { onToggle(type) },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ToggleButtonDefaults.toggleButtonColors(),
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
        leadingIcon = { Icon(Icons.Filled.Description, null) },
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
