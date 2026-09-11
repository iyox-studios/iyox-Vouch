package com.iyox.vouch.ui.components.vouches

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.ui.components.GroupParticipantsDialog
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun GroupVouchSingleItem(
    vouch: VouchWithPerson,
    person: Person?,
    canSettle: Boolean = true,
    onSettle: () -> Unit,
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {},
    onPersonClick: (String) -> Unit = {}
) {
    var showParticipantsDialog by remember { mutableStateOf(false) }
    val dateFormat =
        SimpleDateFormat(stringResource(R.string.format_vouch_date), Locale.getDefault())
    val settleLabel = if (canSettle) {
        stringResource(R.string.action_settle)
    } else {
        stringResource(R.string.action_unsettle)
    }
    val deleteLabel = stringResource(R.string.action_delete)
    val personAmount = vouch.persons.find { it.person == person }?.amount ?: 0L
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
                .combinedClickable(onClick = onEdit, onLongClick = {}),
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
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = CurrencyManager.formatAmountWithSymbol(personAmount),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "/ ${CurrencyManager.formatAmountWithSymbol(vouch.vouch.amount)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(bottom = 1.dp)
                        )
                    }
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
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showParticipantsDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Group,
                                contentDescription = stringResource(R.string.cd_view_participants),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            val nonSelfCount = vouch.persons.count { !it.isSelf }
                            Text(
                                text = pluralStringResource(
                                    R.plurals.person_count,
                                    nonSelfCount,
                                    nonSelfCount
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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

    if (showParticipantsDialog) {
        GroupParticipantsDialog(
            persons = vouch.persons,
            onDismiss = { showParticipantsDialog = false },
            onPersonClick = onPersonClick
        )
    }
}