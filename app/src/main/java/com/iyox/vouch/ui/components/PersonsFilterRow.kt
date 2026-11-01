package com.iyox.vouch.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iyox.vouch.R

@Composable
fun PersonsFilterRow(
    showOnlyNonZero: Boolean,
    onToggleNonZero: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchPlaceholder: String = stringResource(R.string.search_people_placeholder)
) {
    val haptics = LocalHapticFeedback.current

    ExpandableFilterSearchRow(
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        searchPlaceholder = searchPlaceholder,
        modifier = modifier
    ) {
        FilterChip(
            selected = showOnlyNonZero,
            onClick = {
                haptics.performHapticFeedback(
                    if (showOnlyNonZero) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
                )
                onToggleNonZero()
            },
            label = {
                Text(stringResource(R.string.filter_unsettled))
            },
            leadingIcon = {
                if (showOnlyNonZero) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.cd_selected),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )
    }
}
