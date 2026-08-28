package com.iyox.vouch.ui.theme

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.iyox.vouch.R

enum class AppColorTheme(
    val key: String,
    @StringRes val labelRes: Int,
    val previewColor: Color?
) {
    DYNAMIC("dynamic", R.string.color_theme_dynamic, null),
    PURPLE("purple", R.string.color_theme_purple, Color(0xFF6750A4)),
    BLUE("blue", R.string.color_theme_blue, Color(0xFF0061A4)),
    GREEN("green", R.string.color_theme_green, Color(0xFF006D44)),
    TEAL("teal", R.string.color_theme_teal, Color(0xFF006874)),
    ORANGE("orange", R.string.color_theme_orange, Color(0xFF9C4300)),
    ROSE("rose", R.string.color_theme_rose, Color(0xFF9C4146));

    companion object {
        fun fromKey(key: String): AppColorTheme {
            return entries.firstOrNull { it.key == key } ?: DYNAMIC
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IyoxVouchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorTheme: AppColorTheme = AppColorTheme.DYNAMIC,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when (colorTheme) {
        AppColorTheme.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
            }
        }
        AppColorTheme.PURPLE -> if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
        AppColorTheme.BLUE -> if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
        AppColorTheme.GREEN -> if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme
        AppColorTheme.TEAL -> if (darkTheme) TealDarkColorScheme else TealLightColorScheme
        AppColorTheme.ORANGE -> if (darkTheme) OrangeDarkColorScheme else OrangeLightColorScheme
        AppColorTheme.ROSE -> if (darkTheme) RoseDarkColorScheme else RoseLightColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}