package ir.techima.kardan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GuidesScreen(state: AppState) {
    val c = K.c
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<String?>(null) }
    var onlyFav by remember { mutableStateOf(false) }
    val list = GuideData.search(q).filter { (cat == null || it.category == cat) && (!onlyFav || state.favorites.contains("guide:" + it.id)) }
    Page("راهنماها", Persian.digits(GuideData.all.size) + " راهنمای گام‌به‌گام · بازبینی " + GuideData.REVIEWED) {
        KField(q, { q = it }, "جست‌وجو: مثلاً «واتساپ»، «کند شده»، «بسته اینترنت»", trailing = { Icon(Icons.Filled.Search, null, tint = c.muted) })
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("همه", cat == null && !onlyFav) { cat = null; onlyFav = false }
            Chip("★ نشان‌شده‌ها", onlyFav) { onlyFav = !onlyFav }
            GuideData.categories.forEach { k -> Chip(k, cat == k) { cat = if (cat == k) null else k } }
        }
        if (list.isEmpty()) EmptyState(Icons.Filled.MenuBook, "موردی پیدا نشد", "عبارت دیگری را امتحان کن یا از دستیار در صفحهٔ خانه بپرس.")
        list.forEach { g ->
            KCard(onClick = { state.push(Screen.GuideDetail(g.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Filled.MenuBook, c.accent)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(g.title, style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(g.summary, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 2)
                    }
                    if (state.favorites.contains("guide:" + g.id)) Icon(Icons.Filled.Star, null, tint = c.warn)
                }
                Tag(g.category)
            }
        }
    }
}

@Composable
fun GuideDetailScreen(state: AppState, id: String) {
    val c = K.c
    val host = LocalHost.current
    val g = GuideData.byId(id)
    if (g == null) { Page("راهنما", null, { state.back() }) { Text("این راهنما پیدا نشد.", color = c.muted) }; return }
    val done = remember(id) { mutableStateListOf<Int>() }
    val fav = state.favorites.contains("guide:" + g.id)
    Page(g.title, g.category, { host.stopSpeaking(); state.back() }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KOutlineButton(if (fav) "نشان‌شده" else "نشان‌دار کن", { state.toggleFavorite("guide:" + g.id) }, icon = if (fav) Icons.Filled.Star else Icons.Filled.StarBorder)
            KOutlineButton("بخوان", {
                val ok = host.speak(g.title + ". " + g.steps.mapIndexed { i, s -> "گام ${i + 1}. $s" }.joinToString(" "))
                if (!ok) state.say("صدای فارسی روی این گوشی نصب نیست؛ از تنظیمات گوشی، موتور تبدیل متن به گفتار فارسی را اضافه کن.")
            }, icon = Icons.Filled.VolumeUp)
            KOutlineButton("اشتراک", { host.share(g.title + "\n\n" + g.steps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n") + "\n\n— کاردان") }, icon = Icons.Filled.Share)
        }
        GuideHero(g.title, g.category)
        Text(g.summary, style = MaterialTheme.typography.bodyLarge, color = c.text)
        g.warning?.let { w ->
            KCard {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Warning, null, tint = c.warn)
                    Text(w, style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
                }
            }
        }
        SectionTitle("مرحله‌ها (برای پیگیری، هر گام را لمس کن)")
        g.steps.forEachIndexed { i, s ->
            val on = done.contains(i)
            KCard(onClick = { if (on) done.remove(i) else done.add(i) }) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(if (on) c.ok else c.cardHigh), contentAlignment = Alignment.Center) {
                        Text(if (on) "✓" else Persian.digits(i + 1), color = if (on) c.onPrimary else c.text, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(s, style = MaterialTheme.typography.bodyLarge, color = if (on) c.muted else c.text)
                        StepVisual(s)
                    }
                }
            }
        }
        if (g.alt.isNotEmpty()) {
            SectionTitle("اگر نشد")
            KCard { g.alt.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = c.text) } }
        }
        if (g.target != null) KButton(g.targetLabel ?: "باز کردن تنظیمات", { host.openSettings(g.target) }, Modifier.fillMaxWidth())
        if (g.route != null) KOutlineButton(g.routeLabel ?: "باز کردن ابزار", { state.openRoute(g.route) }, Modifier.fillMaxWidth())
        g.links.forEach { l -> KOutlineButton(l.label, { host.openUrl(l.url) }, Modifier.fillMaxWidth(), icon = Icons.Filled.OpenInNew) }
        Text(GuideData.NOTE, style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}
