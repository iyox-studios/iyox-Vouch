package com.iyox.vouch.ui.components.vouches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.ui.components.PersonAvatar
import java.text.SimpleDateFormat

@Composable
fun SingleVouchItem(
    vouch: VouchWithPerson,
    canSettle: Boolean = true,
    onSettle: () -> Unit,
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {},
    onPersonClick: (String) -> Unit = {}
) {
    val dateFormat =
        SimpleDateFormat(stringResource(R.string.format_vouch_date), LocalLocale.current.platformLocale)
    val settleLabel = if (canSettle) {
        stringResource(R.string.action_settle)
    } else {
        stringResource(R.string.action_unsettle)
    }
    val deleteLabel = stringResource(R.string.action_delete)
    val personName = vouch.persons.firstOrNull()?.person?.name
    val hasDescription = vouch.vouch.description.isNotBlank()
    val descriptionText = if (hasDescription) {
        vouch.vouch.description
    } else {
        stringResource(R.string.label_no_description)
    }
    val formattedDate = dateFormat.format(vouch.vouch.timestamp)

    val content = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .clickable(onClick = onEdit),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (vouch.vouch.given) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (vouch.vouch.given) Icons.Outlined.ArrowOutward else Icons.Outlined.SouthWest,
                            contentDescription = null,
                            tint = if (vouch.vouch.given) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = CurrencyManager.formatAmountWithSymbol(vouch.vouch.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasDescription) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!personName.isNullOrBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .widthIn(max = 120.dp)
                                .clip(CircleShape)
                                .clickable { onPersonClick(personName) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PersonAvatar(name = personName, size = 18.dp)
                                Text(
                                    text = personName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    if (canSettle) {
        VouchSwipeToDismiss(
            enableDismissFromStartToEnd = true,
            onStartToEnd = onSettle,
            enableDismissFromEndToStart = true,
            onEndToStart = onDelete,
            settleText = settleLabel,
            deleteText = deleteLabel
        ) {
            content()
        }
    } else {
        VouchSwipeToDismiss(
            enableDismissFromStartToEnd = true,
            onStartToEnd = onSettle,
            enableDismissFromEndToStart = true,
            onEndToStart = onDelete,
            settleText = settleLabel,
            deleteText = deleteLabel
        ) {
            content()
        }
    }
}