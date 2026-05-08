package com.iyox.vouch.feature.vouch

import android.icu.util.Currency
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.R
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithTotal
import com.iyox.vouch.core.utils.LocalSnackbarHostState
import com.iyox.vouch.core.utils.rememberUndoCoordinator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VouchScreen(
    viewModel: VouchViewModel = viewModel(),
    vouchId: Long? = null,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit = onNavigateBack,
    edit: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    rememberScrollState()

    LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    LocalSnackbarHostState.current
    val undoCoordinator = rememberUndoCoordinator()
    val focusManager = LocalFocusManager.current
    val msgVouchCreated = stringResource(R.string.msg_vouch_created)

    LaunchedEffect(vouchId) {
        viewModel.loadVouch(vouchId)
        focusManager.moveFocus(FocusDirection.Down)
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.resetSaveSuccess()
            onSaveSuccess()
        }
    }

    var showCurrencyDialog by remember { mutableStateOf(false) }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentCurrency = uiState.currency,
            onDismiss = { showCurrencyDialog = false },
            onCurrencySelected = { selectedCurrency ->
                viewModel.onCurrencyChange(selectedCurrency)
                showCurrencyDialog = false
            })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (edit) stringResource(R.string.title_edit_vouch) else stringResource(
                            R.string.title_add_vouch
                        )
                    )
                },
                actions = {
                    Button(
                        modifier = Modifier.padding(end = 16.dp),
                        onClick = {
                            if (!edit) {
                                undoCoordinator.executeWithUndo(
                                    message = msgVouchCreated,
                                    performAction = { viewModel.saveVouch() },
                                    undoAction = { savedVouch ->
                                        if (savedVouch != null) {
                                            viewModel.sharedActions.deleteVouch(savedVouch)
                                        }
                                    }
                                )
                            } else {
                                coroutineScope.launch {
                                    viewModel.saveVouch()
                                }
                            }
                        },
                    ) {
                        Text(
                            if (edit) stringResource(R.string.action_update) else stringResource(
                                R.string.action_save
                            )
                        )
                    }
                },
                navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            })
        }) { innerPadding ->
        if (uiState.isLoading && edit) {
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VouchAmountHeader(
                    amount = uiState.amount,
                    currency = uiState.currency,
                    onAmountChange = viewModel::onAmountChange,
                    onCurrencyClick = { showCurrencyDialog = true },
                    focusManager = focusManager
                )

                VouchDirectionToggle(
                    isGiven = uiState.given,
                    onToggle = viewModel::onGivenChange
                )

                PersonAutocompleteField(
                    value = uiState.personName,
                    results = uiState.personSearchResults,
                    onValueChange = viewModel::onPersonChange,
                    onPersonSelected = viewModel::onPersonSelected,
                    focusManager = focusManager
                )

                VouchDescriptionField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    focusManager = focusManager,
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CurrencySelectionDialog(
    currentCurrency: Currency, onDismiss: () -> Unit, onCurrencySelected: (Currency) -> Unit
) {
    val currencies = remember {
        listOf(
            Currency.getInstance("EUR"), // Euro
            Currency.getInstance("USD"), // US Dollar
            Currency.getInstance("GBP"), // British Pound
            Currency.getInstance("JPY"), // Japanese Yen
            Currency.getInstance("CHF"), // Swiss Franc
            Currency.getInstance("CAD"), // Canadian Dollar
            Currency.getInstance("AUD"), // Australian Dollar
            Currency.getInstance("CNY"), // Chinese Yuan
            Currency.getInstance("INR")  // Indian Rupee
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_select_currency)) },
        text = {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(currencies) { currency ->
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onCurrencySelected(currency) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (currency.currencyCode == currentCurrency.currencyCode) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currency.symbol,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currency.currencyCode,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        })
}

@Composable
private fun VouchAmountHeader(
    amount: String,
    currency: Currency,
    onAmountChange: (String) -> Unit,
    onCurrencyClick: () -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onCurrencyClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(currency.symbol, style = MaterialTheme.typography.displayLarge)
            Icon(Icons.Filled.ArrowDropDown, null)
        }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VouchDirectionToggle(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonAutocompleteField(
    value: String,
    results: List<PersonWithTotal>,
    onValueChange: (String) -> Unit,
    onPersonSelected: (Person) -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Close menu if results are empty
    LaunchedEffect(results) {
        expanded = results.isNotEmpty()
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            label = { Text(stringResource(R.string.label_person)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocus(FocusDirection.Next)
                    expanded = false
                }
            ),
            leadingIcon = {
                Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary)
            },
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
                            totalAmount = searchResult.totalAmount?.toDouble() ?: 0.0
                        )
                    },
                    onClick = {
                        onPersonSelected(searchResult.person)
                        focusManager.moveFocus(FocusDirection.Next)
                        expanded = false
                    },
                    contentPadding = PaddingValues(0.dp)
                )
            }
        }
    }
}

@Composable
private fun PersonListItem(
    name: String,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(name, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        trailingContent = {
            Text(
                text = stringResource(R.string.amount_euro, totalAmount),
                style = MaterialTheme.typography.titleMedium
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun VouchDescriptionField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    focusManager: FocusManager,
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
