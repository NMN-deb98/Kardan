package ir.techima.kardan

import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.animation.Crossfade
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
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

private val categoryIcons = listOf(
    Icons.Filled.PhoneAndroid, Icons.Filled.Wifi, Icons.Filled.Forum, Icons.Filled.ShoppingCart, Icons.Filled.SignalCellularAlt,
    Icons.Filled.Security, Icons.Filled.FolderOpen, Icons.Filled.Accessibility, Icons.Filled.CloudOff
)

@Composable
private fun categoryColor(i: Int): Color {
    val c = K.c
    return listOf(c.primary, c.accent, c.ok, c.warn, c.accent, c.danger, c.primary, c.ok, c.warn)[i % 9]
}

/** Notes shown on top of categories where a wrong tip could cost money or privacy. */
private fun categoryNote(cat: String): String? = when (cat) {
    GuideData.categories[2] -> "کاردان به پیام‌ها و محتوای داخل پیام‌رسان‌ها دسترسی ندارد؛ فقط راهنمایی می‌کند. منوها با هر نسخه ممکن است عوض شوند."
    GuideData.categories[3] -> "کاردان هرگز اطلاعات کارت، رمز دوم یا رمز پویا نمی‌خواهد و خرید نمی‌کند. نشان تأیید یا امتیاز فروشنده به‌تنهایی تضمین امنیت نیست."
    GuideData.categories[4] -> "قیمت و کد بسته‌ها را فقط از منبع رسمی اپراتور بگیرید. کاردان خرید را شبیه‌سازی نمی‌کند و بدون تأیید شما هیچ پرداختی انجام نمی‌دهد."
    else -> null
}

@Composable
fun GuidesScreen(state: AppState) {
    val c = K.c
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<String?>(null) }
    var group by remember { mutableStateOf<String?>(null) }
    var onlyFav by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val list = GuideData.search(q).filter {
        (cat == null || it.category == cat) && (group == null || it.group == group) && (!onlyFav || state.favorites.contains("guide:" + it.id))
    }
    val browsing = q.isBlank() && cat == null && !onlyFav
    Page("راهنماها", Persian.digits(GuideData.all.size) + " راهنمای گام‌به‌گام · بازبینی " + GuideData.REVIEWED) {
        KField(q, { q = it }, "جست‌وجو: مثلاً «واتساپ»، «کند شده»، «بسته اینترنت»", trailing = { Icon(Icons.Filled.Search, null, tint = c.muted) })
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("همه", cat == null && !onlyFav) { cat = null; group = null; onlyFav = false }
            Chip("★ نشان‌شده‌ها", onlyFav) { onlyFav = !onlyFav }
            GuideData.categories.forEach { k -> Chip(k, cat == k) { cat = if (cat == k) null else k; group = null } }
        }
        val groups = if (cat != null) GuideData.groupsOf(cat!!) else emptyList()
        if (groups.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("همهٔ ${cat}", group == null) { group = null }
                groups.forEach { g -> Chip(g, group == g) { group = if (group == g) null else g } }
            }
        }
        cat?.let { ct -> categoryNote(ct)?.let { n ->
            KCard { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Info, null, tint = c.accent)
                Text(n, style = MaterialTheme.typography.bodySmall, color = c.text, modifier = Modifier.weight(1f))
            } }
        } }
        if (browsing) {
            // category tiles: the visual entry point
            GuideData.categories.chunked(2).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEachIndexed { j, name ->
                        val idx = row * 2 + j
                        val tint = categoryColor(idx)
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(c.card).clickable { cat = name; group = null }.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconBadge(categoryIcons[idx], tint)
                            Text(name, style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(Persian.digits(GuideData.all.count { it.category == name }) + " راهنما", style = MaterialTheme.typography.bodySmall, color = c.muted)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            SectionTitle("پرکاربردترین‌ها")
        }
        if (list.isEmpty()) EmptyState(Icons.Filled.MenuBook, "موردی پیدا نشد", "عبارت دیگری را امتحان کن یا از دستیار در صفحهٔ خانه بپرس.")
        (if (browsing) list.take(6) else list).forEach { g ->
            val ci = GuideData.categories.indexOf(g.category).coerceAtLeast(0)
            KCard(onClick = { state.push(Screen.GuideDetail(g.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(categoryIcons[ci], categoryColor(ci))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(g.title, style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(g.summary, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (state.favorites.contains("guide:" + g.id)) Icon(Icons.Filled.Star, null, tint = c.warn)
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(g.group ?: g.category)
                    if (GuideData.isStale(g, now)) Tag("ممکن است قدیمی باشد", c.warn)
                    else if (g.volatile) Tag("وابسته به نسخهٔ برنامه", c.muted)
                }
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
    var stepper by remember(id) { mutableStateOf(true) }
    var cur by remember(id) { mutableStateOf(0) }
    var failed by remember(id) { mutableStateOf(false) }
    var finished by remember(id) { mutableStateOf(false) }
    val fav = state.favorites.contains("guide:" + g.id)
    val reduce = state.settings.reduceMotion
    val stale = GuideData.isStale(g)
    val related = remember(id) {
        GuideData.all.filter { it.id != g.id && it.category == g.category && (g.group == null || it.group == g.group) }.take(3)
    }
    Page(g.title, g.group ?: g.category, { host.stopSpeaking(); state.back() }) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KOutlineButton(if (fav) "نشان‌شده" else "نشان‌دار کن", { state.toggleFavorite("guide:" + g.id) }, icon = if (fav) Icons.Filled.Star else Icons.Filled.StarBorder)
            KOutlineButton("بخوان", {
                val ok = host.speak(g.title + ". " + g.steps.mapIndexed { i, s -> "گام ${i + 1}. $s" }.joinToString(" "))
                if (!ok) state.say("صدای فارسی روی این گوشی نصب نیست؛ از تنظیمات گوشی، موتور تبدیل متن به گفتار فارسی را اضافه کن.")
            }, icon = Icons.Filled.VolumeUp)
            KOutlineButton("اشتراک", { host.share(g.title + "\n\n" + g.steps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n") + "\n\n— کاردان") }, icon = Icons.Filled.Share)
        }
        GuideHero(g.title, g.category)
        // version + freshness (spec 4.2)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tag(g.appliesTo ?: GuideData.APPLIES_DEFAULT, c.accent)
            Tag("آخرین بازبینی: " + GuideData.REVIEWED, if (stale) c.warn else c.ok)
        }
        if (stale) KCard {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Warning, null, tint = c.warn)
                Text("این راهنما مدتی است بازبینی نشده و ممکن است با ظاهر فعلی برنامه یا اندروید شما نخواند. اگر گزینه‌ای را ندیدید، به «اگر نشد» سر بزنید یا از منبع رسمی بپرسید.", style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
            }
        }
        Text(g.summary, style = MaterialTheme.typography.bodyLarge, color = c.text)
        g.warning?.let { w ->
            KCard {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Warning, null, tint = c.warn)
                    Text(w, style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("یکی‌یکی", stepper) { stepper = true }
            Chip("همهٔ مرحله‌ها", !stepper) { stepper = false }
        }

        if (stepper) {
            val total = g.steps.size
            LinearProgressIndicator(
                progress = { if (finished) 1f else (cur + 1f) / total },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = c.primary, trackColor = c.cardHigh
            )
            Text(if (finished) "همهٔ مرحله‌ها تمام شد" else "مرحله ${Persian.digits(cur + 1)} از ${Persian.digits(total)}", style = MaterialTheme.typography.labelLarge, color = c.muted)
            val stepBody: @Composable (Int) -> Unit = { i ->
                KCard {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                            Text(Persian.digits(i + 1), color = c.onPrimary, fontWeight = FontWeight.Black)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(g.steps[i], style = MaterialTheme.typography.bodyLarge, color = c.text)
                            StepVisual(g.steps[i])
                        }
                    }
                }
            }
            if (!finished) { if (reduce) stepBody(cur) else Crossfade(cur, label = "step") { stepBody(it) } }
            else KCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Filled.Check, c.ok)
                    Text("اگر مشکل حل شد، عالی! اگر نشد، دکمهٔ «این روش جواب نداد» را بزن تا راه‌حل بعدی را ببینی.", style = MaterialTheme.typography.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KOutlineButton("مرحلهٔ قبل", { if (finished) finished = false else if (cur > 0) cur-- }, Modifier.weight(1f), icon = Icons.AutoMirrored.Filled.ArrowBack)
                if (!finished && cur < total - 1) KButton("مرحلهٔ بعد", { cur++ }, Modifier.weight(1f), icon = Icons.AutoMirrored.Filled.ArrowForward)
                else if (!finished) KButton("تمام شد", { finished = true }, Modifier.weight(1f), icon = Icons.Filled.Check)
            }
        } else {
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
        }

        // "this didn't work": alternatives + related guides (spec 4.2)
        if (!failed) KOutlineButton("این روش جواب نداد", { failed = true }, Modifier.fillMaxWidth(), icon = Icons.Filled.ThumbDown)
        if (failed || (!stepper && g.alt.isNotEmpty())) {
            SectionTitle("راه‌حل جایگزین")
            KCard {
                if (g.alt.isEmpty()) Text("برای این راهنما راه‌حل جایگزین ثبت نشده. راهنماهای مرتبط را ببین یا از پشتیبانی رسمی همان برنامه یا سرویس کمک بگیر.", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                g.alt.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = c.text) }
            }
            if (related.isNotEmpty()) {
                Text("راهنماهای مرتبط", style = MaterialTheme.typography.titleMedium, color = c.text)
                related.forEach { r -> KOutlineButton(r.title, { state.push(Screen.GuideDetail(r.id)) }, Modifier.fillMaxWidth()) }
            }
        }
        if (g.target != null) KButton(g.targetLabel ?: "باز کردن تنظیمات", { host.openSettings(g.target) }, Modifier.fillMaxWidth())
        if (g.route != null) KOutlineButton(g.routeLabel ?: "باز کردن ابزار", { state.openRoute(g.route) }, Modifier.fillMaxWidth())
        g.links.forEach { l -> KOutlineButton(l.label, { host.openUrl(l.url) }, Modifier.fillMaxWidth(), icon = Icons.Filled.OpenInNew) }
        Text(GuideData.NOTE, style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}
