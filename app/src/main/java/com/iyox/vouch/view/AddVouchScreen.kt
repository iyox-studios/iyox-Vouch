package com.iyox.vouch.view

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
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
                    shape = MaterialTheme.shapes.small,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            currency.symbol,
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.padding(end = 8.dp),
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

            OutlinedTextField(
                value = description,
                onValueChange = {
                    viewModel.onDescriptionChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Description, contentDescription = null
                    )
                },
                maxLines = 3
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
