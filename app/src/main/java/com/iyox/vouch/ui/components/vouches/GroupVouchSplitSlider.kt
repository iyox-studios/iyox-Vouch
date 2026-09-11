package com.iyox.vouch.ui.components.vouches

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class GroupSplitSection(
    val amount: Long,
    val color: Color
)

@Composable
fun GroupVouchSplitSlider(
    totalAmountMinor: Long,
    sections: List<GroupSplitSection>,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
    ) {
        val width = size.width
        val height = size.height

        val activeSections = sections.filter { it.amount > 0L }
        val sumAssignedMinor = activeSections.sumOf { it.amount }

        if (activeSections.isEmpty()) {
            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = CornerRadius(height / 2f, height / 2f)
            )
            return@Canvas
        }

        val baseAmount = maxOf(sumAssignedMinor, totalAmountMinor)
        val gap = if (activeSections.size > 1) 4.dp.toPx() else 0f
        val totalGaps = (activeSections.size - 1) * gap
        val availableWidth = (width - totalGaps).coerceAtLeast(0f)

        var currentX = 0f

        for (section in activeSections) {
            val sectionWidth = if (baseAmount > 0L) {
                (availableWidth * (section.amount.toFloat() / baseAmount.toFloat())).coerceAtLeast(0f)
            } else {
                0f
            }

            if (sectionWidth > 0f) {
                val segmentCornerRadius = CornerRadius(
                    minOf(sectionWidth / 2f, height / 2f),
                    height / 2f
                )
                drawRoundRect(
                    color = section.color,
                    topLeft = Offset(x = currentX, y = 0f),
                    size = Size(width = sectionWidth, height = height),
                    cornerRadius = segmentCornerRadius
                )
                currentX += sectionWidth + gap
            }
        }
    }
}
