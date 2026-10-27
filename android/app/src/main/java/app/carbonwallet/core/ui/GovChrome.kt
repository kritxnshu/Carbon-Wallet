package app.carbonwallet.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.carbonwallet.core.model.PortalUpdate
import app.carbonwallet.core.ui.theme.Cw

/** Saffron · white · green hairline, the first thing on every public-portal page. */
@Composable
fun Tricolour(modifier: Modifier = Modifier, height: Dp = 3.dp) {
    Row(modifier.fillMaxWidth().height(height)) {
        Box(Modifier.weight(1f).fillMaxHeight().background(Cw.tricolourSaffron))
        Box(Modifier.weight(1f).fillMaxHeight().background(Color.White))
        Box(Modifier.weight(1f).fillMaxHeight().background(Cw.indiaGreen))
    }
}

/** Carbon Wallet's app mark: a white home with a mint leaf on a deep-green rounded square. */
@Composable
fun Seal(size: Dp, modifier: Modifier = Modifier, @Suppress("UNUSED_PARAMETER") withText: Boolean = true) {
    Canvas(modifier.size(size).semantics { contentDescription = "Carbon Wallet" }) {
        val k = this.size.width / 120f
        scale(k, k, pivot = Offset.Zero) {
            drawRoundRect(Cw.govNavy, cornerRadius = CornerRadius(32f, 32f), size = Size(120f, 120f))
            val house = Path().apply {
                moveTo(30f, 66f); lineTo(60f, 40f); lineTo(90f, 66f); lineTo(90f, 92f); lineTo(30f, 92f); close()
            }
            drawPath(house, Color.White)
            drawRect(Cw.govNavy, Offset(53f, 72f), Size(14f, 20f))
            val leaf = Path().apply {
                moveTo(64f, 44f); cubicTo(72f, 28f, 90f, 24f, 97f, 21f); cubicTo(95f, 38f, 82f, 51f, 64f, 44f); close()
            }
            drawPath(leaf, Color(0xFF8EE3B8))
        }
    }
}

/**
 * The top bar: the app mark and name, which app this is, then text size (Aa) and language, then the screen's
 * actions. Text size and language stay one tap away on every screen.
 */
@Composable
fun GovHeader(
    serviceEn: String,
    serviceHi: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Column(modifier.fillMaxWidth().background(Cw.card).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Seal(34.dp, withText = false)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Carbon Wallet", style = MaterialTheme.typography.titleMedium, color = Cw.ink, maxLines = 1)
                Text(t(serviceEn, serviceHi), style = MaterialTheme.typography.labelSmall, color = Cw.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextSizeButton()
            LanguageToggle(light = false)
            actions()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Cw.border))
    }
}

/** Text size and language on their own, for screens without the top bar (sign-in). */
@Composable
fun UtilityBar() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextSizeButton()
        LanguageToggle(light = false)
    }
}

/** "Aa": each tap steps the text size Normal → Larger → Largest, the same three steps as the website. */
@Composable
fun TextSizeButton() {
    val names = listOf(t("Text size: normal", "अक्षर: सामान्य"), t("Text size: larger", "अक्षर: बड़े"), t("Text size: largest", "अक्षर: सबसे बड़े"))
    val step = UiPrefs.textSize
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable { UiPrefs.updateTextSize((step + 1) % 3) }
            .semantics { contentDescription = names[step] },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("A", color = Cw.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("a", color = Cw.ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            if (step > 0) Text("+".repeat(step), color = Cw.govNavy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LanguageToggle(light: Boolean = true) {
    val hi = UiPrefs.lang == Lang.HI
    val fg = if (light) Color.White else Cw.ink
    Row(
        Modifier
            .padding(horizontal = 2.dp)
            .clip(CircleShape)
            .border(1.dp, fg.copy(alpha = 0.25f), CircleShape)
            .clickable { UiPrefs.updateLang(if (hi) Lang.EN else Lang.HI) }
            .semantics { contentDescription = if (hi) "Switch to English" else "हिंदी में देखें" }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Lucide.Languages, null, tint = fg, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(if (hi) "EN" else "हिं", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun HeaderIcon(icon: ImageVector, description: String, onClick: () -> Unit, badge: Int = 0) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (badge > 0) "$description ($badge)" else description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Cw.ink, modifier = Modifier.size(20.dp))
        if (badge > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 5.dp)
                    .heightIn(min = 16.dp)
                    .background(Cw.critical, CircleShape)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (badge > 9) "9+" else "$badge", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** "What's new" as a soft rounded strip. Moving text can be paused, as accessibility rules ask. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Ticker(updates: List<PortalUpdate>, modifier: Modifier = Modifier, onOpen: (PortalUpdate) -> Unit = {}) {
    if (updates.isEmpty()) return
    var paused by remember { mutableStateOf(false) }
    val lang = UiPrefs.lang
    Row(
        modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(12.dp)).background(Cw.govSky),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.padding(start = 8.dp).background(Cw.govNavy, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(t("New", "नया"), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
        val line = updates.joinToString("     •     ") { lang.pick(it.en, it.hi) }
        Box(Modifier.weight(1f).fillMaxHeight().clickable { onOpen(updates.first()) }, contentAlignment = Alignment.CenterStart) {
            Text(
                line,
                color = Cw.govDeep,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .then(if (paused) Modifier else Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 800, velocity = 40.dp)),
            )
        }
        Box(
            Modifier
                .size(38.dp)
                .clickable { paused = !paused }
                .semantics { contentDescription = if (paused) "Play updates" else "Pause updates" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (paused) Lucide.Play else Lucide.Pause, null, tint = Cw.govNavy, modifier = Modifier.size(15.dp))
        }
    }
}

/** A page title with an optional line under it. */
@Composable
fun PortalBand(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(title, color = Cw.ink, style = MaterialTheme.typography.headlineSmall)
        if (subtitle != null) Text(subtitle, color = Cw.inkMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun HelplineStrip(number: String, label: String, onCall: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Cw.saffronSoft, CardShape)
            .border(1.dp, Cw.saffron.copy(alpha = 0.35f), CardShape)
            .clickable(onClick = onCall)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Lucide.Phone, null, tint = Cw.warningText, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Cw.warningText)
            Text(number, style = MaterialTheme.typography.titleMedium, color = Cw.ink)
        }
        Text(t("Call", "कॉल करें"), style = MaterialTheme.typography.labelLarge, color = Cw.govBlue)
    }
}
