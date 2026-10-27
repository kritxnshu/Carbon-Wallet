package app.carbonwallet.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.carbonwallet.R
import app.carbonwallet.core.ui.Lang
import app.carbonwallet.core.ui.LocalLang
import app.carbonwallet.core.ui.UiPrefs

/** The app's tokens: white cards on a soft green-grey ground, one deep green for actions, amber for highlights. */
object Cw {
    val ink = Color(0xFF15181D)
    val inkMuted = Color(0xFF5D6470)
    val inkLight = Color(0xFF8B919B)
    val canvas = Color(0xFFF3F6F4)
    val card = Color(0xFFFFFFFF)
    val subtle = Color(0xFFEDF1EE)
    val border = Color(0xFFE1E6E2)
    val borderHover = Color(0xFFC9D1CB)

    val terracotta = Color(0xFFC2603A)
    val terracottaSoft = Color(0xFFF8E4DA)
    val moss = Color(0xFF1F7A55)
    val mossSoft = Color(0xFFE3F2EA)

    val success = Color(0xFF3F6B4E)
    val successSoft = Color(0xFFE6ECE2)
    val successText = Color(0xFF2C4D37)
    val successBorder = Color(0xFFBFCFB8)
    val warning = Color(0xFFB7862B)
    val warningSoft = Color(0xFFF6EDD8)
    val warningText = Color(0xFF7A5716)
    val warningBorder = Color(0xFFE5CF9C)
    val critical = Color(0xFFA3362A)
    val criticalSoft = Color(0xFFF5E1DC)
    val criticalText = Color(0xFF7E2419)
    val criticalBorder = Color(0xFFE3B6AC)

    // brand: deep green for actions and headers, mint surfaces, amber highlights (names kept from the first design)
    val govNavy = Color(0xFF0E6B4A)
    val govDeep = Color(0xFF0A4F37)
    val govBlue = Color(0xFF12805C)
    val govSky = Color(0xFFE6F4EC)
    val saffron = Color(0xFFF2A23A)
    val saffronSoft = Color(0xFFFEF2E0)
    val indiaGreen = Color(0xFF138808)
    val tricolourSaffron = Color(0xFFFF9933)

    // data-viz categories
    val vizElectricity = Color(0xFFB4532F)
    val vizTransport = Color(0xFF2F5D7C)
    val vizCooking = Color(0xFF6E5A8A)
    val vizFlights = Color(0xFF48443B)
    val vizOther = Color(0xFFA49C8B)

    fun category(name: String): Color = when (name.lowercase()) {
        "electricity" -> vizElectricity
        "transport" -> vizTransport
        "cooking" -> vizCooking
        "travel", "flights" -> vizFlights
        else -> vizOther
    }
}

object Fonts {
    val plexSans = FontFamily(
        Font(R.font.plex_sans_regular, FontWeight.Normal),
        Font(R.font.plex_sans_medium, FontWeight.Medium),
        Font(R.font.plex_sans_semibold, FontWeight.SemiBold),
        Font(R.font.plex_sans_semibold, FontWeight.Bold),
    )
    /** Plex Devanagari also carries Latin letters, so mixed Hindi + English text stays in one typeface. */
    val plexDeva = FontFamily(
        Font(R.font.plex_deva_regular, FontWeight.Normal),
        Font(R.font.plex_deva_regular, FontWeight.Medium),
        Font(R.font.plex_deva_semibold, FontWeight.SemiBold),
        Font(R.font.plex_deva_semibold, FontWeight.Bold),
    )
    val newsreader = FontFamily(
        Font(R.font.newsreader_medium, FontWeight.Medium),
        Font(R.font.newsreader_semibold, FontWeight.SemiBold),
        Font(R.font.newsreader_semibold, FontWeight.Bold),
    )
    val mono = FontFamily(Font(R.font.plex_mono_medium, FontWeight.Medium))
}

private fun typography(lang: Lang): Typography {
    val body = if (lang == Lang.HI) Fonts.plexDeva else Fonts.plexSans
    // one clean sans for everything: Plex Sans, or Plex Devanagari for Hindi
    val heading = body
    fun s(family: FontFamily, size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) =
        // no colour here: text takes its colour from the surface it sits on (ink on paper, paper on buttons)
        TextStyle(fontFamily = family, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking.sp)
    // a calm, compact scale: one big number per screen, everything else small and quiet
    return Typography(
        displayLarge = s(heading, 38, 44, FontWeight.SemiBold, -0.5),
        displayMedium = s(heading, 30, 36, FontWeight.SemiBold, -0.4),
        displaySmall = s(heading, 26, 32, FontWeight.SemiBold, -0.3),
        headlineLarge = s(heading, 24, 30, FontWeight.SemiBold, -0.2),
        headlineMedium = s(heading, 20, 26, FontWeight.SemiBold, -0.2),
        headlineSmall = s(heading, 18, 24, FontWeight.SemiBold),
        titleLarge = s(heading, 17, 23, FontWeight.SemiBold),
        titleMedium = s(body, 15, 21, FontWeight.SemiBold),
        titleSmall = s(body, 13, 19, FontWeight.SemiBold),
        bodyLarge = s(body, 15, 22, FontWeight.Normal),
        bodyMedium = s(body, 13, 19, FontWeight.Normal),
        bodySmall = s(body, 12, 16, FontWeight.Normal),
        labelLarge = s(body, 13, 18, FontWeight.Medium),
        labelMedium = s(body, 12, 16, FontWeight.Medium, 0.2),
        labelSmall = s(body, 10, 13, FontWeight.Medium, 0.4),
    )
}

private val colors = lightColorScheme(
    primary = Cw.govNavy,
    onPrimary = Color.White,
    primaryContainer = Cw.govSky,
    onPrimaryContainer = Cw.govDeep,
    secondary = Cw.terracotta,
    onSecondary = Color.White,
    secondaryContainer = Cw.terracottaSoft,
    onSecondaryContainer = Cw.ink,
    tertiary = Cw.moss,
    onTertiary = Color.White,
    tertiaryContainer = Cw.mossSoft,
    onTertiaryContainer = Cw.ink,
    background = Cw.canvas,
    onBackground = Cw.ink,
    surface = Cw.card,
    onSurface = Cw.ink,
    surfaceVariant = Cw.subtle,
    onSurfaceVariant = Cw.inkMuted,
    surfaceContainer = Cw.card,
    surfaceContainerLow = Cw.card,
    surfaceContainerHigh = Cw.card,
    surfaceContainerHighest = Cw.subtle,
    surfaceContainerLowest = Color.White,
    outline = Cw.border,
    outlineVariant = Cw.border,
    error = Cw.critical,
    onError = Color.White,
    errorContainer = Cw.criticalSoft,
    onErrorContainer = Cw.criticalText,
    inverseSurface = Cw.ink,
    inverseOnSurface = Cw.card,
)

// Soft, friendly corners
private val shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Text size A / A+ / A++, the same three steps as the website's utility bar. */
val TEXT_SCALES = floatArrayOf(1f, 1.12f, 1.25f)

@Composable
fun CwTheme(content: @Composable () -> Unit) {
    val lang = UiPrefs.lang
    val density = LocalDensity.current
    val scaled = Density(density.density, density.fontScale * TEXT_SCALES[UiPrefs.textSize.coerceIn(0, 2)])
    CompositionLocalProvider(LocalLang provides lang, LocalDensity provides scaled) {
        MaterialTheme(colorScheme = colors, typography = typography(lang), shapes = shapes, content = content)
    }
}
