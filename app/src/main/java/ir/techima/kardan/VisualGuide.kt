package ir.techima.kardan

import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Schematic (not literal) illustrations for guide steps. Built from the step text itself, so they never drift from it. */
object VisualGuide {
    /** Extracts a settings path such as «تنظیمات ← Wi-Fi ← شبکه‌های ذخیره‌شده» from step text. */
    fun path(text: String): List<String>? {
        val m = Regex("(?:[^←:،.؛\\n]+←)+[^←:،.؛\\n]+").find(text) ?: return null
        val parts = m.value.split("←").map { clean(it) }.filter { it.isNotBlank() }
        if (parts.size < 2) return null
        return parts.take(4)
    }

    private fun clean(s: String): String {
        var t = s.trim().replace(Regex("\\(.*?\\)"), "").trim()
        val cut = listOf(" را ", " و ", " که ", " تا ", " سپس ", " بعد ")
        for (c in cut) { val i = t.indexOf(c); if (i > 0) { t = t.substring(0, i); break } }
        val w = t.split(" ").filter { it.isNotBlank() }
        return w.take(3).joinToString(" ").trim('«', '»', ' ')
    }

    private val icons: List<Pair<List<String>, ImageVector>> = listOf(
        listOf("wi-fi", "وای‌فای", "wifi", "مودم") to Icons.Filled.Wifi,
        listOf("بلوتوث", "هدفون") to Icons.Filled.Bluetooth,
        listOf("باتری", "شارژ") to Icons.Filled.BatteryFull,
        listOf("حافظه", "فضا", "فایل", "دانلود") to Icons.Filled.Storage,
        listOf("اعلان") to Icons.Filled.Notifications,
        listOf("قفل", "رمز", "اثر انگشت", "تأیید دومرحله") to Icons.Filled.Lock,
        listOf("خرید", "سفارش", "فروشگاه", "مرجوعی", "سبد") to Icons.Filled.ShoppingCart,
        listOf("پرداخت", "کارت", "درگاه", "بانک", "مبلغ") to Icons.Filled.Payment,
        listOf("پیام", "گفت‌وگو", "گروه", "کانال", "چت") to Icons.Filled.Chat,
        listOf("تماس", "شماره", "تلفن", "پیامک") to Icons.Filled.Phone,
        listOf("امنیت", "مجوز", "کلاهبردار", "مشکوک", "جعلی", "حریم") to Icons.Filled.Security,
        listOf("پشتیبان", "انتقال") to Icons.Filled.Backup,
        listOf("جست‌وجو", "پیدا") to Icons.Filled.Search,
        listOf("خاموش و روشن", "ریستارت", "راه‌اندازی مجدد", "دوباره") to Icons.Filled.PowerSettingsNew,
        listOf("به‌روزرسانی", "بروزرسانی", "نسخه") to Icons.Filled.Refresh,
        listOf("حذف", "پاک") to Icons.Filled.Delete,
        listOf("دانلود") to Icons.Filled.Download,
        listOf("آنتن", "داده همراه", "اپراتور", "سیم") to Icons.Filled.SignalCellularAlt,
        listOf("لینک", "آدرس", "سایت", "اینترنت") to Icons.Filled.Language,
        listOf("اشتراک", "ارسال", "بفرست") to Icons.Filled.Share,
        listOf("کپی") to Icons.Filled.ContentCopy,
        listOf("هشدار", "مراقب", "خطر") to Icons.Filled.Warning,
        listOf("تنظیمات") to Icons.Filled.Settings
    )

    fun iconFor(text: String): ImageVector {
        val t = Persian.normalize(text)
        for ((keys, ic) in icons) if (keys.any { t.contains(Persian.normalize(it)) }) return ic
        return Icons.Filled.Info
    }
}

@Composable
private fun pulse(): Float {
    val reduce = LocalApp.current.settings.reduceMotion
    if (reduce) return 1f
    val t = rememberInfiniteTransition(label = "pulse")
    val v by t.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "p")
    return v
}

@Composable
private fun MiniPhone(title: String, target: String?, seed: Int, done: Boolean) {
    val c = K.c
    val shape = RoundedCornerShape(20.dp)
    val p = pulse()
    Column(
        Modifier.width(138.dp).height(210.dp).clip(shape).background(c.surface).border(2.dp, c.line, shape).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.width(22.dp).height(5.dp).clip(CircleShape).background(c.line))
            Box(Modifier.width(30.dp).height(5.dp).clip(CircleShape).background(c.line))
        }
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        val targetRow = if (done) -1 else 1 + (seed % 3)
        for (i in 0 until 5) {
            if (i == targetRow && target != null) {
                Box(Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(10.dp)).background(c.primary.copy(alpha = 0.18f)).border(1.5.dp, c.primary, RoundedCornerShape(10.dp)), contentAlignment = Alignment.CenterStart) {
                    Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(12.dp).scale(p).clip(CircleShape).background(c.primary))
                        Text(target, fontSize = 10.sp, color = c.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else {
                val w = 0.45f + ((seed * 7 + i * 13) % 45) / 100f
                Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(c.cardHigh))
                    Box(Modifier.fillMaxWidth(w).height(7.dp).clip(CircleShape).background(c.cardHigh))
                }
            }
        }
        if (done) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Box(Modifier.size(34.dp).scale(p).clip(CircleShape).background(c.ok), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** Visual for one guide step. */
@Composable
fun StepVisual(stepText: String) {
    val c = K.c
    val path = remember(stepText) { VisualGuide.path(stepText) }
    if (path != null) {
        val screens = remember(path) {
            val list = mutableListOf<Triple<String, String?, Boolean>>()
            list.add(Triple("صفحهٔ اصلی گوشی", path[0], false))
            for (k in 0 until path.size - 1) list.add(Triple(path[k], path[k + 1], false))
            list.add(Triple(path.last(), null, true))
            list
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                screens.forEachIndexed { i, s ->
                    MiniPhone(s.first, s.second, i + stepText.length, s.third)
                    if (i < screens.size - 1) Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = c.primary, modifier = Modifier.size(18.dp))
                }
            }
            Text("طرح شماتیک مسیر؛ ظاهر دقیق تنظیمات در گوشی شما (بسته به برند و نسخهٔ اندروید) کمی فرق دارد.", color = c.muted, style = MaterialTheme.typography.bodySmall)
        }
    } else {
        val ic = remember(stepText) { VisualGuide.iconFor(stepText) }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.hero).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(ic, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)))
        }
    }
}

/** Large themed illustration shown at the top of a guide. */
@Composable
fun GuideHero(title: String, category: String) {
    val ic = remember(title) { VisualGuide.iconFor(title + " " + category) }
    Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(22.dp)).background(K.c.hero), contentAlignment = Alignment.Center) {
        Box(Modifier.size(150.dp).align(Alignment.TopStart).padding(start = 0.dp).scale(1f).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)))
        Box(Modifier.size(90.dp).align(Alignment.BottomEnd).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)))
        Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Icon(ic, null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}
