package to.kuudere.anisuge.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ─────────────────────────────────────────────────────────────────
val Background   = Color(0xFF0B0B0B)
val Surface      = Color(0xFF141217)
val SurfaceVar   = Color(0xFF1C1B1F)
val DarkSurface  = Color(0xFF1E1E1E)
val OnBackground = Color(0xFFFFFFFF)
val OnSurface    = Color(0xFFFFFFFF)
val Muted        = Color(0xFF999999)
val Border       = Color(0xFF333333)
val Accent       = Color(0xFFFFFFFF)
val Error        = Color(0xFFE53935)
val KuudereRed   = Color(0xFFE50914)

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
