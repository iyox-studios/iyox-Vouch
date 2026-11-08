package com.iyox.vouch.feature.persons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.ui.components.EmptySearchContent
import com.iyox.vouch.ui.components.PageHeader
import com.iyox.vouch.ui.components.PersonAvatar
import com.iyox.vouch.ui.components.PersonsFilterRow
import kotlin.math.abs

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PersonsScreen(
    viewModel: PersonsViewModel = viewModel(),
    onNavigateToViewPerson: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            PageHeader(stringResource(R.string.screen_people)) {
                PersonsFilterRow(
                    showOnlyNonZero = uiState.showOnlyNonZero,
                    onToggleNonZero = viewModel::toggleShowOnlyNonZero,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::setSearchQuery
                )
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.persons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.persons.isEmpty()) {
            if (uiState.searchQuery.isNotBlank()) {
                EmptySearchContent(
                    searchQuery = uiState.searchQuery,
                    onClearSearch = { viewModel.setSearchQuery("") },
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                )
            } else {
                @DrawableRes val emptyIcon: Int
                val emptyTitle: String
                val emptySubtitle: String
                var showResetFilterButton = false

                if (uiState.totalPersonsCount == 0) {
                    emptyIcon = R.drawable.ic_group
                    emptyTitle = stringResource(R.string.empty_people_title)
                    emptySubtitle = stringResource(R.string.empty_people_subtitle)
                } else {
                    emptyIcon = R.drawable.ic_archive
                    emptyTitle = stringResource(R.string.all_settled_title)
                    emptySubtitle = stringResource(R.string.all_settled_people_subtitle)
                    showResetFilterButton = uiState.showOnlyNonZero
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
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
                            modifier = Modifier.size(88.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(emptyIcon),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        Text(
                            text = emptyTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = emptySubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (showResetFilterButton) {
                            OutlinedButton(
                                onClick = viewModel::toggleShowOnlyNonZero,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(text = stringResource(R.string.action_show_all_people))
                            }
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                items(uiState.persons) { personWithTotal ->
                    PersonCard(
                        name = personWithTotal.person.name,
                        amount = personWithTotal.amount,
                        vouchCount = personWithTotal.vouchCount,
                        onView = { onNavigateToViewPerson(personWithTotal.person.name) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PersonCard(
    name: String,
    amount: Long,
    vouchCount: Int,
    onView: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.largeIncreased,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .clip(MaterialTheme.shapes.largeIncreased)
            .testTag("person_card_$name")
            .clickable(onClick = onView),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PersonAvatar(
                    name = name,
                    size = 38.dp
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape,
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.vouch_count,
                            vouchCount,
                            vouchCount
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val arrowIcon = when {
                        amount > 0L -> R.drawable.ic_arrow_outward
                        amount < 0L -> R.drawable.ic_south_west
                        else -> R.drawable.ic_check_circle
                    }
                    val containerColor = when {
                        amount > 0L -> MaterialTheme.colorScheme.primaryContainer
                        amount < 0L -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                    val contentColor = when {
                        amount > 0L -> MaterialTheme.colorScheme.onPrimaryContainer
                        amount < 0L -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = containerColor,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(arrowIcon),
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = CurrencyManager.formatAmountWithSymbol(abs(amount)),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (amount < 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}