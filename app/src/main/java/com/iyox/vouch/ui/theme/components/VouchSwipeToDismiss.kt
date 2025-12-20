package com.best.deskclock.compose_migration

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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VouchSwipeToDismiss(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = dismissState,
        onDismiss = {
            if (it == SwipeToDismissBoxValue.StartToEnd) {
                onDismiss()
            }
        },
        backgroundContent = {
            val direction = dismissState.dismissDirection

            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                val density = LocalDensity.current
                val offsetPx = try {
                    dismissState.requireOffset()
                } catch (_: Exception) {
                    0f
                }

                val widthDp = with(density) {
                    if (offsetPx.isNaN()) 0.dp else offsetPx.coerceAtLeast(0f).toDp()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .width(widthDp)
                            .fillMaxHeight()
                            .background(Color(0xFF156A5B), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.CenterStart,

                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true)
                        ) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Settle",
                                tint = Color.White,
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = "Settle",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        content = {
            content()
        },
        enableDismissFromEndToStart = false
    )
}
