package com.iyox.vouch.view

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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.model.PersonWithTotal
import com.iyox.vouch.viewmodel.VouchViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VouchScreen(
    viewModel: VouchViewModel = viewModel(), vouchId: Long? = null, onNavigateBack: () -> Unit, edit : Boolean = false
) {
    val state by viewModel.uiState.collectAsState()

    var showCurrencyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(vouchId) {
        if (edit) {
            viewModel.loadVouch(vouchId)
        } else {
            viewModel.loadVouch(null)
        }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            viewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentCurrency = state.currency,
            onDismiss = { showCurrencyDialog = false },
            onCurrencySelected = { selectedCurrency ->
                viewModel.onCurrencyChange(selectedCurrency)
                showCurrencyDialog = false
            })
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { if (edit) Text("Edit Vouch") else Text("Add Vouch") }, navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VouchAmountHeader(
                    amount = state.amount,
                    currency = state.currency,
                    onAmountChange = viewModel::onAmountChange,
                    onCurrencyClick = { showCurrencyDialog = true }
                )

                VouchDirectionToggle(
                    isGiven = state.given,
                    onToggle = viewModel::onGivenChange
                )

                PersonAutocompleteField(
                    value = state.personName,
                    results = state.personSearchResults,
                    onValueChange = viewModel::onPersonChange,
                    onPersonSelected = viewModel::onPersonSelected
                )

                VouchDescriptionField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.saveVouch() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Vouch")
                }
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

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Select Currency") }, text = {
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
    }, confirmButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

@Composable
private fun VouchAmountHeader(
    amount: String,
    currency: Currency,
    onAmountChange: (String) -> Unit,
    onCurrencyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onCurrencyClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(currency.symbol, style = MaterialTheme.typography.displayLarge)
            Icon(Icons.Filled.ArrowDropDown, null)
        }
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.displayLarge,
            placeholder = { Text("0.00", style = MaterialTheme.typography.displayLarge) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

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
        listOf(true to "Given", false to "Received").forEach { (type, label) ->
            val isSelected = isGiven == type
            Button(
                onClick = { onToggle(type) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.small,
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
    results: List<PersonWithTotal>, // Replace with your actual import
    onValueChange: (String) -> Unit,
    onPersonSelected: (com.iyox.vouch.model.Person) -> Unit,
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
            label = { Text("Person") },
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
                shape = androidx.compose.foundation.shape.CircleShape,
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
                text = "$totalAmount€",
                style = MaterialTheme.typography.titleMedium
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun VouchDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("Description") },
        leadingIcon = { Icon(Icons.Filled.Description, null) },
        maxLines = 3,
        shape = MaterialTheme.shapes.medium
    )
}