package com.iyox.vouch.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VouchBalanceHeroCard(
    amountGiven: Long,
    amountOwed: Long,
    modifier: Modifier = Modifier,
    total: Long = amountGiven - amountOwed,
    title: String = stringResource(R.string.label_total_balance),
    vouchCount: Int? = null,
    enableRotation: Boolean = true,
    rotationDurationMillis: Int = 40_000,
    shapeRotation: Float? = null
) {
    val cardShape = MaterialTheme.shapes.largeIncreased

    val rotation = if (shapeRotation != null) {
        shapeRotation
    } else if (enableRotation) {
        val infiniteTransition = rememberInfiniteTransition(label = "heroShapeRotation")
        val animatedAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = rotationDurationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "heroShapeRotationAngle"
        )
        animatedAngle
    } else {
        0f
    }

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val cookieShape = MaterialShapes.Cookie9Sided.toShape()
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clipToBounds()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 65.dp, y = (-55).dp)
                        .size(200.dp)
                        .graphicsLayer {
                            rotationZ = rotation
                        }
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = cookieShape
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = when {
                                total > 0L -> MaterialTheme.colorScheme.primaryContainer
                                total < 0L -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when {
                                        total > 0L -> Icons.Outlined.ArrowOutward
                                        total < 0L -> Icons.Outlined.SouthWest
                                        else -> Icons.Filled.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        total > 0L -> MaterialTheme.colorScheme.onPrimaryContainer
                                        total < 0L -> MaterialTheme.colorScheme.onErrorContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = CurrencyManager.formatAmountWithSymbol(total),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (total >= 0L) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (vouchCount != null) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.vouch_count,
                                    vouchCount,
                                    vouchCount
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                VouchGivenReceivedSplitRow(
                    amountGiven = amountGiven,
                    amountReceived = amountOwed,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
