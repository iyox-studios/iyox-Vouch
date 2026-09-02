package com.iyox.vouch.feature.vouch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iyox.vouch.R
import com.iyox.vouch.core.utils.rememberToastManager
import com.iyox.vouch.ui.components.VouchBackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleVouchScreen(
    vouchId: Long? = null,
    prefilledPersonName: String? = null,
    onNavigateBack: () -> Unit,
    edit: Boolean = false,
    viewModel: SingleVouchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val toastManager = rememberToastManager()
    val focusManager = LocalFocusManager.current
    val amountFocusRequester = remember { FocusRequester() }
    val msgVouchCreated = stringResource(R.string.msg_vouch_created)
    val msgSaveFailed = stringResource(R.string.msg_save_failed)
    val actionUndo = stringResource(R.string.action_undo)

    LaunchedEffect(vouchId) {
        if (vouchId != null) {
            viewModel.loadVouch(vouchId, prefilledPersonName)
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && !edit) {
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                actions = {
                    Button(
                        modifier = Modifier.padding(end = 16.dp),
                        onClick = {
                            if (!edit) {
                                toastManager.executeWithUndo(
                                    message = msgVouchCreated,
                                    actionLabel = actionUndo,
                                    errorMessage = msgSaveFailed,
                                    performAction = { viewModel.saveVouchDirect() },
                                    undoAction = { savedVouch ->
                                        if (savedVouch != null) {
                                            viewModel.sharedActions.deleteVouch(savedVouch.vouch.id)
                                        }
                                    }
                                )
                            } else {
                                viewModel.saveVouch()
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
                    VouchBackButton(onClick = onNavigateBack)
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
                VouchAmountHeader(
                    amount = uiState.amount,
                    onAmountChange = viewModel::onAmountChange,
                    focusManager = focusManager,
                    amountFocusRequester = amountFocusRequester
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
                    focusManager = focusManager
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}