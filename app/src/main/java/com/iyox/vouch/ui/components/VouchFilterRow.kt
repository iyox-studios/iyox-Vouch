package com.iyox.vouch.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iyox.vouch.R

@Composable
fun VouchFilterRow(
    sortedBy: Int,
    showOnlyGiven: Int,
    onToggleSort: () -> Unit,
    onToggleGiven: () -> Unit,
    modifier: Modifier = Modifier,
    onSettledClick: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current

    // Helper to handle haptic + callback
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
            }
        )

        if (onSettledClick != null) {
            Spacer(modifier = Modifier.weight(1f))

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
                    imageVector = Icons.Outlined.Archive,
                    contentDescription = null,
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
