package com.iyox.vouch.ui.components.vouches

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

data class GroupSplitSection(
    val amount: Long, val color: Color, val label: String? = null
)

@Composable
fun GroupVouchSplitSlider(
    totalAmountMinor: Long, sections: List<GroupSplitSection>, modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val density = LocalDensity.current
    val verticalOffsetPx = with(density) { 32.dp.roundToPx() }

    val activeSections = remember(sections, totalAmountMinor) {
        val filtered = sections.filter { it.amount > 0L }
        val sumAssigned = filtered.sumOf { it.amount }
        val unassigned = totalAmountMinor - sumAssigned
        if (unassigned > 0L && filtered.none { it.label == null && it.color == trackColor }) {
            filtered + GroupSplitSection(amount = unassigned, color = trackColor)
        } else {
            filtered
        }
    }

    var selectedSection by remember { mutableStateOf<GroupSplitSection?>(null) }

    LaunchedEffect(selectedSection) {
        if (selectedSection != null) {
            delay(2500.milliseconds)
            selectedSection = null
        }
    }

    LaunchedEffect(sections) {
        selectedSection = null
    }

    if (activeSections.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(24.dp), contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(trackColor)
            )
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (section in activeSections) {
            val isSelected = selectedSection == section && !section.label.isNullOrBlank()

            Box(
                modifier = Modifier
                    .weight(section.amount.toFloat())
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        selectedSection = if (selectedSection == section) null else section
                    }, contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(section.color)
                )

                if (isSelected) {
                    Popup(
                        alignment = Alignment.TopCenter,
                        offset = IntOffset(0, -verticalOffsetPx),
                        onDismissRequest = { selectedSection = null },
                        properties = PopupProperties(
                            focusable = false,
                            dismissOnClickOutside = false,
                            dismissOnBackPress = true
                        )
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.inverseSurface,
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = section.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
