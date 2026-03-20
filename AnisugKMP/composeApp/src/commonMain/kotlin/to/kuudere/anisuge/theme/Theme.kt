package to.kuudere.anisuge.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ─────────────────────────────────────────────────────────────────
val Background   = Color(0xFF000000)
val Surface      = Color(0xFF000000)
val SurfaceVar   = Color(0xFF0C0C0C)
val DarkSurface  = Color(0xFF080808)
val OnBackground = Color(0xFFFFFFFF)
val OnSurface    = Color(0xFFFFFFFF)
val Muted        = Color(0xFF999999)
val Border       = Color(0xFF1F1F1F)
val Accent       = Color(0xFFFFFFFF)
val Error        = Color(0xFFBF80FF)
val KuudereRed   = Color(0xFFBF80FF)

private val DarkColorScheme = darkColorScheme(
    primary          = Accent,
    onPrimary        = Color.Black,
    secondary        = Muted,
    onSecondary      = Color.White,
    tertiary         = SurfaceVar,
    background       = Background,
    onBackground     = OnBackground,
    surface          = Surface,
    onSurface        = OnSurface,
    surfaceVariant   = SurfaceVar,
    onSurfaceVariant = Muted,
    error            = Error,
    outline          = Border,
)

@Composable
fun AnisugTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AnisugTypography(),
        content     = content,
    )
}
