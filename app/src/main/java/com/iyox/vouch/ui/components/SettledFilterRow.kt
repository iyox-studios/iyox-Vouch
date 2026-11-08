package com.iyox.vouch.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iyox.vouch.R
import com.iyox.vouch.core.model.VouchSortOrder
import com.iyox.vouch.core.model.VouchTypeFilter

@Composable
fun SettledFilterRow(
    sortedBy: VouchSortOrder,
    showOnlyGiven: VouchTypeFilter,
    selectedPersonName: String?,
    onToggleSort: () -> Unit,
    onToggleGiven: () -> Unit,
    onClearPerson: () -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    onSearchQueryChange: ((String) -> Unit)? = null,
    searchPlaceholder: String = stringResource(R.string.search_settled_placeholder)
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
        modifier = modifier
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
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.action_cancel),
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}
