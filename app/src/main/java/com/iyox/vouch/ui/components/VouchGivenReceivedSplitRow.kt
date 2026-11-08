package com.iyox.vouch.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager

@Composable
fun VouchGivenReceivedSplitRow(
    amountGiven: Long,
    amountReceived: Long,
    modifier: Modifier = Modifier,
    outerCornerRadius: Dp = 14.dp,
    splitSpacing: Dp = 8.dp
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(splitSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Pill: Given (rounded only on outer start edges)
        PillItem(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.label_given),
            amount = amountGiven,
            icon = R.drawable.ic_arrow_outward,
            isPositiveDirection = true,
            shape = RoundedCornerShape(
                topStart = outerCornerRadius,
                bottomStart = outerCornerRadius,
                topEnd = 0.dp,
                bottomEnd = 0.dp
            )
        )

        // Right Pill: Received (rounded only on outer end edges)
        PillItem(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.label_received),
            amount = amountReceived,
            icon = R.drawable.ic_south_west,
            isPositiveDirection = false,
            shape = RoundedCornerShape(
                topStart = 0.dp,
                bottomStart = 0.dp,
                topEnd = outerCornerRadius,
                bottomEnd = outerCornerRadius
            )
        )
    }
}

@Composable
private fun PillItem(
    label: String,
    amount: Long,
    @DrawableRes icon: Int,
    isPositiveDirection: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    val iconContainerColor = if (isPositiveDirection) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    val iconColor = if (isPositiveDirection) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconContainerColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = CurrencyManager.formatAmountWithSymbol(amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
