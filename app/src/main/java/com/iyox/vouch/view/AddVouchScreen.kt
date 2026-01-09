package com.iyox.vouch.view

import android.icu.util.Currency
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.viewmodel.AddVouchViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddVouchScreen(
    viewModel: AddVouchViewModel = viewModel(), onNavigateBack: () -> Unit
) {
    val amount by viewModel.amount.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val given by viewModel.given.collectAsState()
    val person by viewModel.person.collectAsState()
    val description by viewModel.description.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    var showCurrencyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            viewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentCurrency = currency,
            onDismiss = { showCurrencyDialog = false },
            onCurrencySelected = { selectedCurrency ->
                viewModel.onCurrencyChange(selectedCurrency)
                showCurrencyDialog = false
            })
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Add Vouch") }, navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            })
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showCurrencyDialog = true },
                    modifier = Modifier.defaultMinSize(minWidth = 0.dp, minHeight = 0.dp),
                    shape = MaterialTheme.shapes.small,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            currency.symbol,
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.padding(start = 10.dp) //strangely more padding than for icon, but only this looks right
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        viewModel.onAmountChange(it)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.displayLarge,
                    singleLine = true,
                    placeholder = {
                        Text(
                            "00" + (if (currency.defaultFractionDigits > 0) "." else "") + "0".repeat(
                                currency.defaultFractionDigits
                            ), style = MaterialTheme.typography.displayLarge
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.onGivenChange(true)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (given) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (given) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Text("Given")
                }

                Button(
                    onClick = {
                        viewModel.onGivenChange(false)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!given) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (!given) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Text("Received")
                }
            }

            OutlinedTextField(
                value = person,
                onValueChange = {
                    viewModel.onPersonChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleLarge,
                label = { Text("Person") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Person, contentDescription = null
                    )
                },
                maxLines = 3,
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    viewModel.onDescriptionChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleLarge,
                label = { Text("Description") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Description, contentDescription = null
                    )
                },
                maxLines = 3,
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.saveVouch() }, modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Vouch")
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
            Currency.getInstance("USD"), // US Dollar
            Currency.getInstance("EUR"), // Euro
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