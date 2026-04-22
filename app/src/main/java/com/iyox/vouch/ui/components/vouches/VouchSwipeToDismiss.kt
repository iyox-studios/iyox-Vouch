package com.iyox.vouch.ui.components.vouches

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VouchSwipeToDismiss(
    onStartToEnd: () -> Unit = {},
    onEndToStart: () -> Unit = {},
    enableDismissFromStartToEnd: Boolean = false,
    enableDismissFromEndToStart: Boolean = false,
    settleText: String = "Settle",
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    lateinit var dismissState: SwipeToDismissBoxState
    val strictConfirmValueChange: (SwipeToDismissBoxValue) -> Boolean = { targetValue ->
        if (targetValue == SwipeToDismissBoxValue.Settled) {
            true
        } else {
            val currentOffset = try {
                dismissState.requireOffset()
            } catch (e: Exception) {
                0f
            }
            abs(currentOffset) >= 500
        }
    }


    dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = strictConfirmValueChange
    )

    LaunchedEffect(Unit) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        onDismiss = {
            haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
            if (it == SwipeToDismissBoxValue.StartToEnd) {
                onStartToEnd()
            }
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onEndToStart()
            }
        },
        backgroundContent = {
            val direction = dismissState.dismissDirection

            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                VouchSwipeToDismissBackground(
                    dismissState,
                    Color(0xFF156A5B),
                    settleText,
                    Icons.Default.Done
                )
            } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                VouchSwipeToDismissBackground(
                    dismissState,
                    MaterialTheme.colorScheme.errorContainer,
                    "Delete",
                    Icons.Default.Delete
                )
            }
        },
        content = {
            content()
        },
        enableDismissFromEndToStart = enableDismissFromEndToStart,
        enableDismissFromStartToEnd = enableDismissFromStartToEnd
    )
}

@Composable
private fun VouchSwipeToDismissBackground(
    dismissState: SwipeToDismissBoxState, color: Color, text: String, icon: ImageVector
) {
    val density = LocalDensity.current
    val direction = dismissState.dismissDirection
    val contentAlignment =
        if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    val offsetPx = try {
        dismissState.requireOffset()
    } catch (_: Exception) {
        0f
    }

    val widthDp = with(density) {
        if (offsetPx.isNaN()) 0.dp else abs(offsetPx).toDp()
    }

    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = contentAlignment
    ) {
        Box(
            modifier = Modifier
                .width(widthDp)
                .fillMaxHeight()
                .background(color, RoundedCornerShape(20.dp)),
            contentAlignment = contentAlignment,

            ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.wrapContentWidth(
                    align = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.Start else Alignment.End,
                    unbounded = true
                )
            ) {
                val icon = @Composable {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = Color.White,
                    )
                }

                val text = @Composable {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val spacer = @Composable {
                    Spacer(modifier = Modifier.width(16.dp))
                }


                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    spacer()
                    icon()
                    spacer()
                    text()
                } else {
                    spacer()
                    text()
                    spacer()
                    icon()
                    spacer()
                }
            }
        }
    }
}
