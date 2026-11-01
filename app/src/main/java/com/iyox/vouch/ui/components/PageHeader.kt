package com.iyox.vouch.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PageHeader(
    title: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    filterChips: (@Composable (RowScope.() -> Unit))? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                if (title != null)
                    Text(
                        title,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black
                    )
            },
            actions = {
                if (actions != null) {
                    actions()
                }
            },
            windowInsets = WindowInsets(top = 20.dp)
        )
        if (filterChips != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filterChips()
            }
        }
    }
}