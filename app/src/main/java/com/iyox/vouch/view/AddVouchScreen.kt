package com.iyox.vouch.view

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.ButtonGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
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

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            viewModel.resetSaveSuccess()
            onNavigateBack()
        }
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
                    onClick = { },
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
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
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
                            "00.00", style = MaterialTheme.typography.displayLarge
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
