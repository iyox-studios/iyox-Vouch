package com.iyox.vouch.ui.components.vouches

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iyox.vouch.R
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun VouchSwipeToDismiss(
    onStartToEnd: () -> Unit = {},
    onEndToStart: () -> Unit = {},
    enableDismissFromStartToEnd: Boolean = false,
    enableDismissFromEndToStart: Boolean = false,
    settleText: String,
    deleteText: String,
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val minDragDistancePx = with(density) { 100.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()
    val currentOnStartToEnd by rememberUpdatedState(onStartToEnd)
    val currentOnEndToStart by rememberUpdatedState(onEndToStart)
    var isDismissing by remember { mutableStateOf(false) }

    lateinit var dismissState: SwipeToDismissBoxState
    @Suppress("DEPRECATION")
    dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.4f },
        confirmValueChange = { targetValue ->
            targetValue == SwipeToDismissBoxValue.Settled || (!isDismissing && run {
                val offset = try {
                    dismissState.requireOffset()
                } catch (_: Exception) {
                    0f
                }
                abs(offset) >= minDragDistancePx
            })
        }
    )

    LaunchedEffect(Unit) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    val currentOffset = try {
        dismissState.requireOffset()
    } catch (_: Exception) {
        0f
    }
    var thresholdCrossed by remember { mutableStateOf(false) }

    LaunchedEffect(currentOffset, isDismissing) {
        val crossed = abs(currentOffset) >= minDragDistancePx
        if (crossed && !thresholdCrossed && !isDismissing) {
            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            thresholdCrossed = true
        } else if (!crossed && thresholdCrossed) {
            thresholdCrossed = false
        }
    }

    val handleDismiss: (SwipeToDismissBoxValue) -> Unit = remember {
        { value ->
            if (!isDismissing) {
                isDismissing = true
                haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                coroutineScope.launch {
                    try {
                        dismissState.reset()
                    } catch (_: Exception) {
                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                    } finally {
                        isDismissing = false
                    }
                }
                if (value == SwipeToDismissBoxValue.StartToEnd) {
                    currentOnStartToEnd()
                } else if (value == SwipeToDismissBoxValue.EndToStart) {
                    currentOnEndToStart()
                }
            }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        onDismiss = handleDismiss,
        gesturesEnabled = !isDismissing,
        backgroundContent = {
            val direction = dismissState.dismissDirection

            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                VouchSwipeToDismissBackground(
                    dismissState,
                    Color(0xFF156A5B),
                    settleText,
                    R.drawable.ic_check
                )
            } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                VouchSwipeToDismissBackground(
                    dismissState,
                    MaterialTheme.colorScheme.errorContainer,
                    deleteText,
                    R.drawable.ic_delete
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
    dismissState: SwipeToDismissBoxState, color: Color, text: String, @DrawableRes icon: Int
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
                        painter = painterResource(icon),
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
