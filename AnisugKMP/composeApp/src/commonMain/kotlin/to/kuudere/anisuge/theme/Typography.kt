package to.kuudere.anisuge.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import anisurge.composeapp.generated.resources.Res
import anisurge.composeapp.generated.resources.Poppins_Regular
import anisurge.composeapp.generated.resources.Poppins_Medium
import anisurge.composeapp.generated.resources.Poppins_SemiBold
import anisurge.composeapp.generated.resources.Poppins_Bold
import org.jetbrains.compose.resources.Font

@Composable
fun PoppinsFamily() = FontFamily(
    Font(Res.font.Poppins_Regular,  FontWeight.Normal),
    Font(Res.font.Poppins_Medium,   FontWeight.Medium),
    Font(Res.font.Poppins_SemiBold, FontWeight.SemiBold),
    Font(Res.font.Poppins_Bold,     FontWeight.Bold),
)

@Composable
fun AnisugTypography(): Typography {
    val poppins = PoppinsFamily()
    return Typography(
        displayLarge   = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Bold,     fontSize = 57.sp),
        displayMedium  = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Bold,     fontSize = 45.sp),
        headlineLarge  = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Bold,     fontSize = 42.sp, letterSpacing = (-1.5).sp),
        headlineMedium = TextStyle(fontFamily = poppins, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
        headlineSmall  = TextStyle(fontFamily = poppins, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
        titleLarge     = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Bold,     fontSize = 22.sp),
        titleMedium    = TextStyle(fontFamily = poppins, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleSmall     = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Medium,   fontSize = 14.sp),
        bodyLarge      = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium     = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 22.4.sp),
        bodySmall      = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Normal,   fontSize = 12.sp),
        labelLarge     = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Bold,     fontSize = 15.sp, letterSpacing = 0.5.sp),
        labelMedium    = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Medium,   fontSize = 13.sp),
        labelSmall     = TextStyle(fontFamily = poppins, fontWeight = FontWeight.Medium,   fontSize = 11.sp),
    )
}
