package com.hdvideodownloader.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0C1220)
val BgCard = Color(0xFF162033)
val BgCardGlass = Color(0xCC182336)
val BorderSubtle = Color(0xFF2D3C54)
val EmeraldGlow = Color(0xFF34E8B0)
val TealAccent = Color(0xFF22D3A6)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

private val VisionDarkColors = darkColorScheme(
    primary = EmeraldGlow,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = TealAccent,
    onSecondary = Color(0xFF062825),
    background = BgDark,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF451A1A),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun HDVideoDownloaderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VisionDarkColors,
        content = content
    )
}
