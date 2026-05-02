package com.iyox.vouch.feature.settings

import android.app.UiModeManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.R
import com.iyox.vouch.ui.components.PageHeader

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeDropdown by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportData(uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importData(uri)
        }
    }

    Scaffold(topBar = { PageHeader(stringResource(R.string.screen_settings)) { } }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(stringResource(R.string.settings_section_appearance))

            SettingItem(
                icon = Icons.Outlined.Translate,
                title = stringResource(R.string.settings_language),
                subtitle = "English"
            )

            Box {
                SettingItem(
                    icon = Icons.Outlined.Brightness4,
                    title = stringResource(R.string.settings_theme),
                    subtitle = when (uiState.theme) {
                        UiModeManager.MODE_NIGHT_NO -> stringResource(R.string.theme_light)
                        UiModeManager.MODE_NIGHT_YES -> stringResource(R.string.theme_dark)
                        UiModeManager.MODE_NIGHT_AUTO -> stringResource(R.string.theme_system)
                        else -> stringResource(R.string.theme_error)
                    },
                    onClick = { showThemeDropdown = true }
                )
                DropdownMenu(
                    expanded = showThemeDropdown,
                    onDismissRequest = { showThemeDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.theme_system)) },
                        onClick = {
                            viewModel.setTheme(UiModeManager.MODE_NIGHT_AUTO); showThemeDropdown =
                            false
                        })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.theme_dark)) },
                        onClick = {
                            viewModel.setTheme(UiModeManager.MODE_NIGHT_YES); showThemeDropdown =
                            false
                        })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.theme_light)) },
                        onClick = {
                            viewModel.setTheme(UiModeManager.MODE_NIGHT_NO); showThemeDropdown =
                            false
                        })
                }
            }

            SettingItem(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.settings_dynamic_colors),
                subtitle = stringResource(R.string.settings_dynamic_colors_subtitle),
                trailing = {
                    Switch(
                        checked = uiState.dynamicColors,
                        onCheckedChange = { viewModel.setDynamicColors(it) }
                    )
                },
                onClick = { viewModel.setDynamicColors(!uiState.dynamicColors) }
            )

            SectionHeader(stringResource(R.string.settings_section_data))

            SettingItem(
                icon = Icons.Outlined.Upload,
                title = stringResource(R.string.settings_export_data),
                subtitle = stringResource(R.string.settings_export_data_subtitle),
                onClick = { exportLauncher.launch("vouch_backup.json") }
            )

            SettingItem(
                icon = Icons.Outlined.Download,
                title = stringResource(R.string.settings_import_data),
                subtitle = stringResource(R.string.settings_import_data_subtitle),
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
            )

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

            SettingItem(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.settings_app_version),
                subtitle = viewModel.appVersion
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(16.dp))
            trailing()
        }
    }
}
