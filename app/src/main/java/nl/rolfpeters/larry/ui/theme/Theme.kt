package nl.rolfpeters.larry.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LarryDarkColors = darkColorScheme(
    primary = RpAmber,
    onPrimary = RpWhite,
    secondary = RpRed,
    onSecondary = RpWhite,
    background = RpDark,
    onBackground = RpOffWhite,
    surface = RpDark2,
    onSurface = RpOffWhite,
    surfaceVariant = RpDark3,
    onSurfaceVariant = RpGray300,
    error = RpRed,
)

private val LarryLightColors = lightColorScheme(
    primary = RpAmber,
    onPrimary = RpWhite,
    secondary = RpRed,
    onSecondary = RpWhite,
    background = RpOffWhite,
    onBackground = RpDark,
    surface = RpWhite,
    onSurface = RpDark,
    surfaceVariant = RpGray300,
    onSurfaceVariant = RpGray500,
    error = RpRedDark,
)

@Composable
fun LarryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) LarryDarkColors else LarryLightColors
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
