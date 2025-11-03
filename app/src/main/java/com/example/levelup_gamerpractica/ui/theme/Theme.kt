package com.example.levelup_gamerpractica.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme // Mantenemos un light scheme por si acaso
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,         // Color principal para botones, elementos activos
    onPrimary = PureWhite,          // Texto/iconos sobre el color primario
    primaryContainer = DarkSurface, // Contenedor relacionado al primario
    onPrimaryContainer = PureWhite, // Texto/iconos sobre primaryContainer

    secondary = NeonGreen,          // Color secundario para elementos flotantes, selección
    onSecondary = DeepBlack,        // Texto/iconos sobre secundario (negro para contraste con neón)
    secondaryContainer = DarkSurface, // Contenedor secundario
    onSecondaryContainer = PureWhite, // Texto sobre secundario

    tertiary = LightGray,           // Color terciario (para menor énfasis)
    onTertiary = DeepBlack,         // Texto sobre terciario
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = LightGray,

    error = RedError,               // Color para errores
    onError = PureWhite,            // Texto sobre error
    errorContainer = RedErrorDark,
    onErrorContainer = PureWhite,

    background = DeepBlack,         // Fondo principal de la app
    onBackground = PureWhite,       // Texto principal sobre el fondo

    surface = DarkSurface,          // Superficies de componentes (Cards, Menus)
    onSurface = PureWhite,          // Texto sobre surfaces

    surfaceVariant = DarkSurfaceVariant, // Superficie con ligera variación
    onSurfaceVariant = LightGray,   // Texto secundario o de menor énfasis

    outline = ElectricBlue          // Bordes, divisores (usamos el primario)
)

// --- Esquema Claro (puedes definirlo si planeas tener ambos temas) ---
// Por ahora, lo dejamos con los valores por defecto o unos básicos
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
    /* Otros colores por defecto... */
)

@Composable
fun LevelUpGamerPracticaTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Asegúrate de que Typography esté definido en Type.kt
        content = content
    )
}