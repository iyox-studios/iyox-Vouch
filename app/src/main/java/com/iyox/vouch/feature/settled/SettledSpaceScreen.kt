package com.iyox.vouch.feature.settled

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.feature.vouch.PersonAvatar
import com.iyox.vouch.ui.components.VouchBackButton
import com.iyox.vouch.ui.components.vouches.VouchList

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettledSpaceScreen(
    initialPersonName: String? = null,
    viewModel: SettledSpaceViewModel = viewModel(
        factory = SettledSpaceViewModel.factory(initialPersonName)
    ),
    onNavigateBack: () -> Unit,
    onNavigateToEditVouch: (Int, Boolean) -> Unit = { _, _ -> },
    onNavigateToPerson: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.selectedPersonName != null) {
                            stringResource(R.string.title_person_settled_space, uiState.selectedPersonName!!)
                        } else {
                            stringResource(R.string.screen_settled_space)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    VouchBackButton(onClick = onNavigateBack)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Settled Metrics Hero Card
            SettledSpaceHeroCard(
                totalResolved = uiState.totalSettledVolume,
                givenSettled = uiState.totalSettledGiven,
                receivedSettled = uiState.totalSettledReceived,
                settledCount = uiState.settledCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter & Sort Row
            SettledFilterRow(
                sortedBy = uiState.sortedBy,
                showOnlyGiven = uiState.showOnlyGiven,
                selectedPersonName = uiState.selectedPersonName,
                onToggleSort = viewModel::toggleSortedBy,
                onToggleGiven = viewModel::toggleShowOnlyGiven,
                onClearPerson = { viewModel.setSelectedPerson(null) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (uiState.vouches.isEmpty()) {
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
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(88.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Archive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.empty_settled_vouches_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.selectedPersonName != null) {
                                stringResource(
                                    R.string.empty_person_settled_vouches,
                                    uiState.selectedPersonName!!
                                )
                            } else {
                                stringResource(R.string.empty_settled_vouches_subtitle)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (uiState.selectedPersonName != null) {
                            OutlinedButton(
                                onClick = { viewModel.setSelectedPerson(null) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(stringResource(R.string.action_show_all_settled))
                            }
                        }
                    }
                }
            } else {
                VouchList(
                    displayedSorted = uiState.vouches,
                    listState = listState,
                    innerPadding = PaddingValues(0.dp),
                    sharedVouchActions = viewModel.sharedActions,
                    onEdit = onNavigateToEditVouch,
                    onNavigateToPerson = onNavigateToPerson,
                    isSettled = { true }
                )
            }
        }
    }
}

@Composable
private fun SettledSpaceHeroCard(
    totalResolved: Long,
    givenSettled: Long,
    receivedSettled: Long,
    settledCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Archive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.settled_space_total_resolved),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.vouch_count,
                            settledCount,
                            settledCount
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = CurrencyManager.formatAmountWithSymbol(totalResolved),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowOutward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.label_given) + ":",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = CurrencyManager.formatAmountWithSymbol(givenSettled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SouthWest,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.label_received) + ":",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = CurrencyManager.formatAmountWithSymbol(receivedSettled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun SettledFilterRow(
    sortedBy: Int,
    showOnlyGiven: Int,
    selectedPersonName: String?,
    onToggleSort: () -> Unit,
    onToggleGiven: () -> Unit,
    onClearPerson: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    val handleToggle: (Boolean, () -> Unit) -> Unit = { isTurningOff, callback ->
        haptics.performHapticFeedback(
            if (isTurningOff) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
        )
        callback()
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort Chip
        FilterChip(
            selected = true,
            onClick = { handleToggle(false, onToggleSort) },
            label = {
                Text(
                    when (sortedBy) {
                        0 -> stringResource(R.string.filter_date)
                        1 -> stringResource(R.string.filter_amount)
                        2 -> stringResource(R.string.filter_person)
                        else -> stringResource(R.string.filter_date)
                    }
                )
            },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(18.dp))
            },
            modifier = Modifier.padding(end = 8.dp)
        )

        // Given/Received Filter Chip
        FilterChip(
            selected = showOnlyGiven > 0,
            onClick = { handleToggle(showOnlyGiven == 2, onToggleGiven) },
            label = {
                Text(
                    when (showOnlyGiven) {
                        0 -> stringResource(R.string.filter_given_received_short)
                        1 -> stringResource(R.string.label_given)
                        2 -> stringResource(R.string.label_received)
                        else -> ""
                    }
                )
            },
            leadingIcon = {
                if (showOnlyGiven > 0) {
                    Icon(Icons.Default.FilterList, null, Modifier.size(18.dp))
                }
            },
            modifier = Modifier.padding(end = 8.dp)
        )

        // Person Chip if filtered
        if (selectedPersonName != null) {
            InputChip(
                selected = true,
                onClick = onClearPerson,
                label = {
                    Text(
                        text = selectedPersonName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                avatar = {
                    PersonAvatar(name = selectedPersonName, size = 18.dp)
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}
