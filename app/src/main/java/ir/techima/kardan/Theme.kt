package ir.techima.kardan

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density

@Immutable
class KColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val card: Color,
    val cardHigh: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val warn: Color,
    val danger: Color,
    val ok: Color,
    val heroA: Color,
    val heroB: Color
) {
    val hero: Brush get() = Brush.linearGradient(listOf(heroA, heroB))
}

private val DarkK = KColors(
    dark = true, bg = Color(0xFF0A111F), surface = Color(0xFF111A2C), card = Color(0xFF172238), cardHigh = Color(0xFF1F2D4A),
    line = Color(0xFF2A3956), text = Color(0xFFF1F5FB), muted = Color(0xFF9CAAC4), primary = Color(0xFF3DDCB4),
    onPrimary = Color(0xFF05241C), accent = Color(0xFF8DA2FF), warn = Color(0xFFFFC76B), danger = Color(0xFFFF7A8A),
    ok = Color(0xFF5BE49B), heroA = Color(0xFF14407A), heroB = Color(0xFF0E7A73)
)

private val LightK = KColors(
    dark = false, bg = Color(0xFFF2F6FB), surface = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF), cardHigh = Color(0xFFE8EFF9),
    line = Color(0xFFD8E2F0), text = Color(0xFF0E1A2E), muted = Color(0xFF5A6A84), primary = Color(0xFF0B9C84),
    onPrimary = Color(0xFFFFFFFF), accent = Color(0xFF4A63E0), warn = Color(0xFFB7791F), danger = Color(0xFFD64560),
    ok = Color(0xFF16915A), heroA = Color(0xFF1F5FBF), heroB = Color(0xFF0FA38F)
)

val LocalK = staticCompositionLocalOf { DarkK }

object K {
    val c: KColors @Composable get() = LocalK.current
}

@Composable
fun KardanTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val k = if (dark) DarkK else LightK
    val scheme: ColorScheme = if (dark) darkColorScheme(
        primary = k.primary, onPrimary = k.onPrimary, secondary = k.accent, background = k.bg, surface = k.surface,
        onBackground = k.text, onSurface = k.text, surfaceVariant = k.card, onSurfaceVariant = k.muted, outline = k.line,
        error = k.danger, secondaryContainer = k.cardHigh, onSecondaryContainer = k.text
    ) else lightColorScheme(
        primary = k.primary, onPrimary = k.onPrimary, secondary = k.accent, background = k.bg, surface = k.surface,
        onBackground = k.text, onSurface = k.text, surfaceVariant = k.card, onSurfaceVariant = k.muted, outline = k.line,
        error = k.danger, secondaryContainer = k.cardHigh, onSecondaryContainer = k.text
    )
    val base = LocalDensity.current
    val scale = (if (settings.senior) maxOf(settings.fontScale, 1.3f) else settings.fontScale).coerceIn(0.85f, 1.8f)
    val typography = Typography(
        headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Black, lineHeight = 38.sp),
        headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),
        titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp),
        titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 27.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 24.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 20.sp),
        labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
    )
    CompositionLocalProvider(
        LocalK provides k,
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides Density(base.density, base.fontScale * scale)
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
