package app.carbonwallet.ghar

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.carbonwallet.core.net.Http
import app.carbonwallet.core.net.Store
import app.carbonwallet.core.shell.LocalNav
import app.carbonwallet.core.ui.Banner
import app.carbonwallet.core.ui.CwCard
import app.carbonwallet.core.ui.CwTextField
import app.carbonwallet.core.ui.DateField
import app.carbonwallet.core.ui.Eyebrow
import app.carbonwallet.core.ui.Lucide
import app.carbonwallet.core.ui.PrimaryButton
import app.carbonwallet.core.ui.SecondaryButton
import app.carbonwallet.core.ui.Tag
import app.carbonwallet.core.ui.Tone
import app.carbonwallet.core.ui.date
import app.carbonwallet.core.ui.t
import app.carbonwallet.core.ui.theme.Cw
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate

/** Electric vehicles need no PUC; for the rest it is pending until added, then valid or expired (as on the website). */
private fun pucState(v: Vehicle): String = when {
    v.fuel == "Electric" -> "NOT_NEEDED"
    v.pucNumber.isNullOrBlank() -> "PENDING"
    v.pucValidUntil?.let { runCatching { Instant.parse(it) }.getOrNull() }?.isBefore(Instant.now()) == true -> "EXPIRED"
    else -> "VALID"
}

/** Under each vehicle: its PUC status, its GPS ID, and a way to add the PUC once it's issued. */
@Composable
fun PucLine(home: ActiveHome, v: Vehicle) {
    val nav = LocalNav.current
    val state = pucState(v)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        when (state) {
            "NOT_NEEDED" -> Tag(t("PUC not needed (electric)", "PUC ज़रूरी नहीं (इलेक्ट्रिक)"))
            "PENDING" -> Tag(t("PUC pending", "PUC बाकी"), color = Cw.warningText, background = Cw.warningSoft)
            "EXPIRED" -> Tag(t("PUC expired", "PUC की अवधि ख़त्म"), color = Cw.criticalText, background = Cw.criticalSoft)
            else -> Tag(
                v.pucValidUntil?.let { t("PUC valid till ${date(it)}", "PUC ${date(it)} तक मान्य") } ?: t("PUC added", "PUC जुड़ा है"),
                color = Cw.successText,
                background = Cw.successSoft,
            )
        }
        v.gpsDeviceId?.let { Tag("GPS $it") }
    }
    if (home.isOwner && (state == "PENDING" || state == "EXPIRED")) {
        Spacer(Modifier.height(8.dp))
        SecondaryButton(t("Add PUC", "PUC जोड़ें"), { nav.push("Add PUC", "PUC जोड़ें") { AddPuc(home, v) } }, Modifier.fillMaxWidth(), icon = Lucide.FileCheck)
    }
}

/** DigiLocker on the website (it needs the browser sign-in), or type the certificate number and validity here. */
@Composable
private fun AddPuc(home: ActiveHome, v: Vehicle) {
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var number by rememberSaveable { mutableStateOf("") }
    var validUntil by rememberSaveable { mutableStateOf(LocalDate.now().plusMonths(6).toString()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val needNumber = t("Enter the PUC certificate number.", "PUC प्रमाणपत्र नंबर लिखें।")
    val added = t("PUC added", "PUC जुड़ गया")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(v.name, style = MaterialTheme.typography.titleLarge)
        CwCard {
            Eyebrow(t("From DigiLocker", "DigiLocker से"))
            Text(
                t("Your PUC certificate is issued into DigiLocker. Open Card & items on the website, sign in, and tap Add from DigiLocker.",
                    "आपका PUC प्रमाणपत्र DigiLocker में आता है। वेबसाइट पर Card & items खोलें, साइन इन करें और Add from DigiLocker दबाएँ।"),
                style = MaterialTheme.typography.bodyMedium,
                color = Cw.inkMuted,
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(t("Open DigiLocker on the website", "वेबसाइट पर DigiLocker खोलें"), {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${Http.baseUrl}/assets")))
            }, Modifier.fillMaxWidth(), icon = Lucide.External)
        }
        CwCard {
            Eyebrow(t("Or type it in", "या ख़ुद लिखें"))
            error?.let { Banner(Tone.BAD, it) }
            CwTextField(number, { number = it.uppercase().take(30) }, t("PUC certificate number", "PUC प्रमाणपत्र नंबर"), icon = Lucide.FileCheck)
            Spacer(Modifier.height(8.dp))
            DateField(t("Valid until", "कब तक मान्य"), validUntil, { validUntil = it }, allowFuture = true)
            Spacer(Modifier.height(10.dp))
            PrimaryButton(t("Save PUC", "PUC सेव करें"), {
                if (number.trim().length < 4) {
                    error = needNumber
                    return@PrimaryButton
                }
                busy = true
                error = null
                scope.launch {
                    try {
                        Http.send("POST", homePath(home.id, "vehicles/${v.id}/puc"), buildJsonObject {
                            put("pucNumber", number.trim())
                            put("validUntil", validUntil)
                        }, PucSaved.serializer())
                        Store.changed()
                        nav.pop()
                        nav.toast(added)
                    } catch (e: Exception) {
                        error = e.message
                    }
                    busy = false
                }
            }, Modifier.fillMaxWidth(), icon = Lucide.Check, busy = busy)
        }
    }
}
