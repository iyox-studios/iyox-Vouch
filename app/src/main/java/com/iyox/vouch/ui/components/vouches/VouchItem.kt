package com.iyox.vouch.ui.components.vouches

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iyox.vouch.R
import com.iyox.vouch.core.model.Vouch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun VouchItem(
    vouch: Vouch,
    canSettle: Boolean = true,
    onSettle: () -> Unit,
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {}
) {
    val dateFormat =
        SimpleDateFormat(stringResource(R.string.format_vouch_date), Locale.getDefault())
    val settleLabel = if (canSettle) {
        stringResource(R.string.action_settle)
    } else {
        stringResource(R.string.action_unsettle)
    }
    val deleteLabel = stringResource(R.string.action_delete)

    val content = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.largeIncreased)
                .combinedClickable(onClick = onEdit, onLongClick = {}),
            shape = MaterialTheme.shapes.largeIncreased,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (vouch.given) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (vouch.given) Icons.Outlined.ArrowOutward else Icons.Outlined.SouthWest,
                            contentDescription = stringResource(R.string.cd_home),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = vouch.amount.toString() + " " + vouch.currency.symbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = vouch.description.ifBlank { dateFormat.format(vouch.timestamp) },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                if (vouch.personName != null) Text(
                    text = vouch.personName,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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