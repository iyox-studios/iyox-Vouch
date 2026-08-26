package com.iyox.vouch.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PageHeader(
    title: String,
    actions: (@Composable RowScope.() -> Unit)? = null,
    filterChips: (@Composable (RowScope.() -> Unit))? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Text(
                    title, style = MaterialTheme.typography.displayMedium
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
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                filterChips()
            }
        }
    }
}