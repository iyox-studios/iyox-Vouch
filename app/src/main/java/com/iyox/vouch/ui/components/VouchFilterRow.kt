package com.iyox.vouch.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iyox.vouch.R
import com.iyox.vouch.core.model.VouchSortOrder
import com.iyox.vouch.core.model.VouchTypeFilter

@Composable
fun VouchFilterRow(
    sortedBy: VouchSortOrder,
    showOnlyGiven: VouchTypeFilter,
    onToggleSort: () -> Unit,
    onToggleGiven: () -> Unit,
    modifier: Modifier = Modifier,
    onSettledClick: (() -> Unit)? = null,
    searchQuery: String = "",
    onSearchQueryChange: ((String) -> Unit)? = null,
    searchPlaceholder: String = stringResource(R.string.search_vouches_placeholder)
) {
    val haptics = LocalHapticFeedback.current

    val handleToggle: (Boolean, () -> Unit) -> Unit = { isTurningOff, callback ->
        haptics.performHapticFeedback(
            if (isTurningOff) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
        )
        callback()
    }

    ExpandableFilterSearchRow(
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        searchPlaceholder = searchPlaceholder,
        modifier = modifier,
    ) {
        // Sort Chip
        FilterChip(
            selected = true,
            onClick = { handleToggle(false, onToggleSort) },
            label = {
                Text(
                    when (sortedBy) {
                        VouchSortOrder.DATE -> stringResource(R.string.filter_date)
                        VouchSortOrder.AMOUNT -> stringResource(R.string.filter_amount)
                        VouchSortOrder.PERSON -> stringResource(R.string.filter_person)
                    }
                )
            },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_sort), null, Modifier.size(18.dp))
            },
            modifier = Modifier.padding(end = 8.dp)
        )

        // Given/Received Filter Chip
        FilterChip(
            selected = showOnlyGiven != VouchTypeFilter.ALL,
            onClick = { handleToggle(showOnlyGiven == VouchTypeFilter.RECEIVED, onToggleGiven) },
            label = {
                Text(
                    when (showOnlyGiven) {
                        VouchTypeFilter.ALL -> stringResource(R.string.filter_given_received_short)
                        VouchTypeFilter.GIVEN -> stringResource(R.string.label_given)
                        VouchTypeFilter.RECEIVED -> stringResource(R.string.label_received)
                    }
                )
            },
            leadingIcon = {
                if (showOnlyGiven != VouchTypeFilter.ALL) {
                    Icon(painterResource(R.drawable.ic_filter_list), null, Modifier.size(18.dp))
                }
            }
        )

        if (onSettledClick != null) {
            Spacer(modifier = Modifier.width(6.dp))
            FilledTonalButton(
                onClick = onSettledClick,
                shape = FilterChipDefaults.shape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_archive),
                    contentDescription = stringResource(R.string.screen_settled_space),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_view_settled_space),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
