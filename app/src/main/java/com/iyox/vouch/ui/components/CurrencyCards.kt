package com.iyox.vouch.ui.components

import android.icu.util.Currency
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iyox.vouch.R
import java.util.Locale

@Composable
fun CurrencyCard(
    currency: Currency,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1.35f
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0] ?: Locale.getDefault()

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
    } else {
        Modifier
    }

    Card(
        onClick = onSelect,
        modifier = modifier
            .aspectRatio(aspectRatio)
            .then(borderModifier)
            .semantics { selected = isSelected },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currency.getSymbol(locale),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currency.currencyCode,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Composable
fun CurrencySearchCard(
    isCustomSelected: Boolean,
    selectedCurrency: Currency?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1.35f
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0] ?: Locale.getDefault()

    val containerColor = if (isCustomSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isCustomSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderModifier = if (isCustomSelected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
    } else {
        Modifier
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(aspectRatio)
            .then(borderModifier)
            .semantics { selected = isCustomSelected },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isCustomSelected && selectedCurrency != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = selectedCurrency.getSymbol(locale),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.currency_search_button),
                        tint = contentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedCurrency.currencyCode,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.currency_search_button),
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.currency_search_button),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }
}
