package com.iyox.vouch.feature.settings

import android.app.UiModeManager
import android.content.Context
import android.content.ContextWrapper
import android.icu.util.Currency
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iyox.vouch.R
import com.iyox.vouch.core.backup.IyoxEnvelope
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.ui.components.PageHeader
import com.iyox.vouch.ui.theme.AppColorTheme
import java.util.Locale

data class SupportedLanguage(
    val tag: String,
    @field:StringRes val labelRes: Int,
    val displayCode: String,
    val icon: ImageVector? = null
)

private val supportedLanguages = listOf(
    SupportedLanguage("system", R.string.settings_language_system, "AUTO", Icons.Outlined.Language),
    SupportedLanguage("en", R.string.settings_lang_en, "EN"),
    SupportedLanguage("de", R.string.settings_lang_de, "DE"),
)

private fun matchesLanguageTag(supportedTag: String, currentTag: String): Boolean {
    if (supportedTag == currentTag) return true
    if (supportedTag == "system" || currentTag == "system") return false
    val currentLang = Locale.forLanguageTag(currentTag).language
    val supportedLang = Locale.forLanguageTag(supportedTag).language
    return currentLang.isNotEmpty() && currentLang.equals(supportedLang, ignoreCase = true)
}

private fun findSupportedLanguage(tag: String): SupportedLanguage? {
    return supportedLanguages.firstOrNull { it.tag == tag }
        ?: supportedLanguages.firstOrNull { matchesLanguageTag(it.tag, tag) }
}

data class SupportedTheme(
    val mode: Int,
    @field:StringRes val labelRes: Int,
    val icon: ImageVector
)

private val supportedThemes = listOf(
    SupportedTheme(UiModeManager.MODE_NIGHT_AUTO, R.string.theme_system, Icons.Outlined.BrightnessAuto),
    SupportedTheme(UiModeManager.MODE_NIGHT_YES, R.string.theme_dark, Icons.Outlined.DarkMode),
    SupportedTheme(UiModeManager.MODE_NIGHT_NO, R.string.theme_light, Icons.Outlined.LightMode)
)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToOnboarding: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val biometricTitle = stringResource(R.string.biometric_title)
    val biometricSubtitle = stringResource(R.string.biometric_subtitle)
    val biometricCancel = stringResource(R.string.action_cancel)

    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    val isPerAppLanguageSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    LaunchedEffect(Unit) {
        viewModel.refreshLanguage()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(IyoxEnvelope.MIME_TYPE)
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

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguageTag = uiState.languageTag,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { selectedTag ->
                viewModel.setLanguage(selectedTag)
                showLanguageDialog = false
            }
        )
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

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.theme,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { selectedTheme ->
                viewModel.setTheme(selectedTheme)
                showThemeDialog = false
            }
        )
    }

    if (showColorDialog) {
        ColorThemeSelectionDialog(
            currentColorTheme = uiState.colorTheme,
            onDismiss = { showColorDialog = false },
            onColorThemeSelected = { selectedColorTheme ->
                viewModel.setColorTheme(selectedColorTheme)
                showColorDialog = false
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

            SettingItem(
                icon = Icons.Outlined.Translate,
                title = stringResource(R.string.settings_language),
                subtitle = if (isPerAppLanguageSupported) {
                    findSupportedLanguage(uiState.languageTag)
                        ?.let { stringResource(it.labelRes) }
                        ?: uiState.languageTag
                } else {
                    stringResource(R.string.settings_language_android_13_only)
                },
                onClick = if (isPerAppLanguageSupported) {
                    { showLanguageDialog = true }
                } else {
                    null
                }
            )

            SettingItem(
                icon = Icons.Outlined.AttachMoney,
                title = stringResource(R.string.settings_currency),
                subtitle = "${currentCurrency.getSymbol(Locale.getDefault())} (${currentCurrency.currencyCode})",
                onClick = { showCurrencyDialog = true }
            )

            SettingItem(
                icon = Icons.Outlined.Explore,
                title = stringResource(R.string.settings_onboarding_guide),
                subtitle = stringResource(R.string.settings_onboarding_guide_subtitle),
                onClick = onNavigateToOnboarding
            )

            SectionHeader(stringResource(R.string.settings_section_appearance))

            SettingItem(
                icon = Icons.Outlined.Brightness4,
                title = stringResource(R.string.settings_theme),
                subtitle = when (uiState.theme) {
                    UiModeManager.MODE_NIGHT_NO -> stringResource(R.string.theme_light)
                    UiModeManager.MODE_NIGHT_YES -> stringResource(R.string.theme_dark)
                    UiModeManager.MODE_NIGHT_AUTO -> stringResource(R.string.theme_system)
                    else -> stringResource(R.string.theme_error)
                },
                onClick = { showThemeDialog = true }
            )

            SettingItem(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.settings_color_theme),
                subtitle = stringResource(uiState.colorTheme.labelRes),
                onClick = { showColorDialog = true }
            )

            SectionHeader(stringResource(R.string.settings_section_security))

            SettingItem(
                icon = Icons.Outlined.Fingerprint,
                title = stringResource(R.string.settings_biometric_auth),
                subtitle = stringResource(R.string.settings_biometric_auth_subtitle),
                trailing = {
                    Switch(
                        checked = uiState.biometricEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                authenticateAndEnableBiometric(
                                    context = context,
                                    viewModel = viewModel,
                                    title = biometricTitle,
                                    subtitle = biometricSubtitle,
                                    cancelText = biometricCancel
                                )
                            } else {
                                viewModel.setBiometricEnabled(false)
                            }
                        }
                    )
                },
                onClick = {
                    if (!uiState.biometricEnabled) {
                        authenticateAndEnableBiometric(
                            context = context,
                            viewModel = viewModel,
                            title = biometricTitle,
                            subtitle = biometricSubtitle,
                            cancelText = biometricCancel
                        )
                    } else {
                        viewModel.setBiometricEnabled(false)
                    }
                }
            )

            SectionHeader(stringResource(R.string.settings_section_data))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = { exportLauncher.launch(IyoxEnvelope.DEFAULT_BACKUP_FILE_NAME) },
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
                    onClick = { importLauncher.launch(arrayOf("*/*", IyoxEnvelope.MIME_TYPE, "application/octet-stream", "application/json")) },
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
fun LanguageSelectionDialog(
    currentLanguageTag: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_select_language)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(supportedLanguages) { language ->
                    val isSelected = matchesLanguageTag(language.tag, currentLanguageTag)
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onLanguageSelected(language.tag) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
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
                            if (language.icon != null) {
                                Icon(
                                    imageVector = language.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            } else {
                                Text(
                                    text = language.displayCode,
                                    style = MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(language.labelRes),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                    val isSelected = currency.currencyCode == currentCurrency.currencyCode
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onCurrencySelected(currency) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
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
                                text = currency.getSymbol(Locale.getDefault()),
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

@Composable
fun ThemeSelectionDialog(
    currentTheme: Int,
    onDismiss: () -> Unit,
    onThemeSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_select_theme)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(supportedThemes) { theme ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onThemeSelected(theme.mode) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (theme.mode == currentTheme) {
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
                            Icon(
                                imageVector = theme.icon,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = if (theme.mode == currentTheme) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(theme.labelRes),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

@Composable
fun ColorThemeSelectionDialog(
    currentColorTheme: AppColorTheme,
    onDismiss: () -> Unit,
    onColorThemeSelected: (AppColorTheme) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_select_color_theme)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AppColorTheme.entries) { colorTheme ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onColorThemeSelected(colorTheme) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (colorTheme == currentColorTheme) {
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
                            if (colorTheme.previewColor == null) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = if (colorTheme == currentColorTheme) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(color = colorTheme.previewColor, shape = CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(colorTheme.labelRes),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

private fun authenticateAndEnableBiometric(
    context: Context,
    viewModel: SettingsViewModel,
    title: String,
    subtitle: String,
    cancelText: String
) {
    val biometricManager = BiometricManager.from(context)
    when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
            val activity = context.findFragmentActivity() ?: return
            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        viewModel.setBiometricEnabled(true)
                    }

                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(cancelText)
                .build()

            biometricPrompt.authenticate(promptInfo)
        }

        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
            viewModel.showBiometricError(R.string.toast_biometric_none_enrolled)
        }

        else -> {
            viewModel.showBiometricError(R.string.toast_biometric_not_available)
        }
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}


