package com.iyox.vouch.feature.settings

import android.app.UiModeManager
import android.icu.util.Currency
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.ui.components.PageHeader

data class SupportedLanguage(
    val tag: String,
    @field:StringRes val labelRes: Int
)

private val supportedLanguages = listOf(
    SupportedLanguage("system", R.string.settings_language_system),
    SupportedLanguage("en", R.string.settings_lang_en),
    SupportedLanguage("de", R.string.settings_lang_de),
)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeDropdown by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    val isPerAppLanguageSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

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

    val currentCurrency = try {
        Currency.getInstance(uiState.currencyCode)
    } catch (_: Exception) {
        CurrencyManager.getCurrency()
    }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentCurrency = currentCurrency,
            onDismiss = { showCurrencyDialog = false },
            onCurrencySelected = { selectedCurrency ->
                viewModel.setCurrency(selectedCurrency.currencyCode)
                showCurrencyDialog = false
            }
        )
    }

    Scaffold(topBar = { PageHeader(stringResource(R.string.screen_settings)) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(stringResource(R.string.settings_section_general))

            Box {
                SettingItem(
                    icon = Icons.Outlined.Translate,
                    title = stringResource(R.string.settings_language),
                    subtitle = if (isPerAppLanguageSupported) {
                        supportedLanguages
                            .firstOrNull { it.tag == uiState.languageTag }
                            ?.let { stringResource(it.labelRes) }
                            ?: uiState.languageTag
                    } else {
                        stringResource(R.string.settings_language_android_13_only)
                    },
                    onClick = if (isPerAppLanguageSupported) {
                        { showLanguageDropdown = true }
                    } else {
                        null
                    }
                )
                if (isPerAppLanguageSupported) {
                    DropdownMenu(
                        expanded = showLanguageDropdown,
                        onDismissRequest = { showLanguageDropdown = false }
                    ) {
                        supportedLanguages.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(stringResource(language.labelRes)) },
                                onClick = {
                                    viewModel.setLanguage(language.tag)
                                    showLanguageDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            SettingItem(
                icon = Icons.Outlined.AttachMoney,
                title = stringResource(R.string.settings_currency),
                subtitle = "${currentCurrency.symbol} (${currentCurrency.currencyCode})",
                onClick = { showCurrencyDialog = true }
            )

            SectionHeader(stringResource(R.string.settings_section_appearance))

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = { exportLauncher.launch("vouch_backup.json") },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_export_data),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_import_data),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

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

@Composable
fun CurrencySelectionDialog(
    currentCurrency: Currency,
    onDismiss: () -> Unit,
    onCurrencySelected: (Currency) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_select_currency)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CurrencyManager.supportedCurrencies) { currency ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onCurrencySelected(currency) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currency.currencyCode == currentCurrency.currencyCode) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currency.symbol,
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currency.currencyCode,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

