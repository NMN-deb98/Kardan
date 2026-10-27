package ir.techima.kardan

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

const val APP_VERSION_NOTE = "نسخهٔ ۱٫۱ (آزمایشی)"

@Composable
fun SettingsScreen(state: AppState, title: String, onBack: () -> Unit) {
    val c = K.c
    val host = LocalHost.current
    val s = state.settings
    var confirmWipe by remember { mutableStateOf(false) }
    Page(title, null, onBack) {
        SectionTitle("ظاهر")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("مطابق سیستم", "روشن", "تیره").forEachIndexed { i, n -> Chip(n, s.theme == i) { state.updateSettings(s.copy(theme = i)) } }
        }
        SectionTitle("اندازهٔ نوشته")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("کوچک" to 0.9f, "معمولی" to 1f, "بزرگ" to 1.15f, "خیلی بزرگ" to 1.3f, "درشت‌ترین" to 1.5f).forEach { (n, f) ->
                Chip(n, Math.abs(s.fontScale - f) < 0.01f) { state.updateSettings(s.copy(fontScale = f)) }
            }
        }
        KCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("حالت سالمندان و کم‌تجربه‌ها", style = MaterialTheme.typography.titleMedium, color = c.text)
                    Text("نوشته‌ها حداقل ۳۰٪ بزرگ‌تر می‌شود تا خواندن راحت‌تر باشد. راهنمای «سالمندان» را هم از بخش راهنماها ببین.", style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                Switch(checked = s.senior, onCheckedChange = { state.updateSettings(s.copy(senior = it)) })
            }
        }
        KCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("کاهش حرکت و انیمیشن", style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                Switch(checked = s.reduceMotion, onCheckedChange = { state.updateSettings(s.copy(reduceMotion = it)) })
            }
        }
        SectionTitle("اعلان و یادآوری")
        KCard {
            Text(if (state.notifOk) "اعلان‌ها فعال است." else "اعلان‌ها بسته است؛ یادآوری‌ها نمی‌رسند.", color = if (state.notifOk) c.ok else c.warn)
            Text(if (Reminders.canExact(state.ctx)) "زمان‌بندی دقیق مجاز است." else "زمان‌بندی دقیق مجاز نیست؛ یادآوری‌ها ممکن است چند دقیقه دیرتر برسند.", color = c.muted, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KOutlineButton("مجوز اعلان", { host.requestNotifications() })
                KOutlineButton("بهینه‌سازی باتری", { host.openSettings(Target.BATTERY_OPT) })
            }
            Text("بعضی برندها (مثل شیائومی، سامسونگ، هواوی) برنامه‌های پس‌زمینه را می‌بندند؛ اگر یادآوری نرسید، کاردان را از بهینه‌سازی باتری مستثنا کن.", color = c.muted, style = MaterialTheme.typography.bodySmall)
        }
        SectionTitle("داده‌ها")
        KOutlineButton("پاک کردن همهٔ داده‌های کاردان", { confirmWipe = true }, Modifier.fillMaxWidth(), danger = true, icon = Icons.Filled.Delete)
        SectionTitle("دربارهٔ کاردان")
        KCard {
            Text("کاردان $APP_VERSION_NOTE", color = c.text, fontWeight = FontWeight.Bold)
            Text("راهنماها آخرین‌بار ${GuideData.REVIEWED} بازبینی شده‌اند. کاردان به همهٔ برنامه‌های گوشی دسترسی ندارد، خرید یا ارسال یا حذفی را بدون تأیید تو انجام نمی‌دهد و امنیت مطلق را تضمین نمی‌کند. روی دستگاه‌های مختلف اندرویدی کامل آزمایش نشده است.", color = c.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (confirmWipe) AlertDialog(
        onDismissRequest = { confirmWipe = false },
        title = { Text("پاک کردن همه‌چیز؟") },
        text = { Text("کارها، عادت‌ها، فهرست تماشا، تماس‌های سریع، تنظیمات و گاوصندوق از این گوشی پاک می‌شود و برگشتی ندارد. اگر پشتیبان نگرفته‌ای، ابتدا از بخش پشتیبان‌گیری بگیر.") },
        confirmButton = { TextButton(onClick = {
            confirmWipe = false
            state.tasks.toList().forEach { Reminders.cancel(state.ctx, it.id) }
            Repo.wipeAll(state.ctx); VaultCrypto.wipe(state.ctx); state.cancelTimer(); state.reload(); state.say("همهٔ داده‌ها پاک شد.")
        }) { Text("پاک کن", color = c.danger) } },
        dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("انصراف") } }
    )
}

@Composable
fun MoviesScreen(state: AppState, title: String, onBack: () -> Unit) {
    val c = K.c
    val host = LocalHost.current
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(Categories.watchTypes[0]) }
    var filter by remember { mutableStateOf<String?>(null) }
    Page(title, "فهرست شخصی خودت؛ بدون لینک دانلود و بدون ادعای دسترسی به پلتفرم‌ها", onBack) {
        KCard {
            KField(name, { name = it }, "نام فیلم یا سریال")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Categories.watchTypes.forEach { t -> Chip(t, type == t) { type = t } } }
            KButton("افزودن به فهرست", {
                if (name.isBlank()) state.say("نام را بنویس.") else { state.watch.add(0, WatchItem(state.newId(), name.trim(), type)); state.saveWatch(); name = "" }
            }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("همه", filter == null) { filter = null }
            Categories.watchStatus.forEach { k -> Chip(k, filter == k) { filter = if (filter == k) null else k } }
        }
        val list = state.watch.filter { filter == null || it.status == filter }
        if (list.isEmpty()) EmptyState(Icons.Filled.Movie, "فهرست خالی است", "هر چیزی که دوست داری ببینی را اینجا یادداشت کن و بعد از دیدن، امتیاز بده.")
        list.forEach { w ->
            val idx = state.watch.indexOfFirst { it.id == w.id }
            KCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(w.title, style = MaterialTheme.typography.titleMedium, color = c.text)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Tag(w.type); Tag(w.status, if (w.status == "دیده‌شده") c.ok else c.warn) }
                    }
                    IconButton(onClick = { state.watch.removeAt(idx); state.saveWatch() }) { Icon(Icons.Filled.Delete, "حذف", tint = c.muted) }
                }
                Row {
                    for (i in 1..5) IconButton(onClick = { state.watch[idx] = w.copy(rating = if (w.rating == i) 0 else i); state.saveWatch() }) {
                        Icon(if (i <= w.rating) Icons.Filled.Star else Icons.Filled.StarBorder, "امتیاز $i", tint = if (i <= w.rating) c.warn else c.muted)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KOutlineButton(Categories.watchStatus[(Categories.watchStatus.indexOf(w.status) + 1) % Categories.watchStatus.size].let { "← $it" }, {
                        val next = Categories.watchStatus[(Categories.watchStatus.indexOf(w.status) + 1) % Categories.watchStatus.size]
                        state.watch[idx] = w.copy(status = next); state.saveWatch()
                    })
                    KOutlineButton("جست‌وجو در مرورگر", { host.openUrl("https://www.google.com/search?q=" + java.net.URLEncoder.encode(w.title + " " + w.type, "UTF-8")) })
                }
            }
        }
    }
}

@Composable
fun BackupScreen(state: AppState, title: String, onBack: () -> Unit) {
    val c = K.c
    val host = LocalHost.current
    var confirmImport by remember { mutableStateOf(false) }
    Page(title, "فایل پشتیبان را خودت جایی امن نگه می‌داری", onBack) {
        KCard {
            Text("چه چیزی در پشتیبان است؟", style = MaterialTheme.typography.titleMedium, color = c.text)
            Text("کارها، عادت‌ها، فهرست تماشا، تماس‌های سریع، نشان‌ها و تنظیمات. گاوصندوق رمزها عمداً در فایل نیست، چون فایل پشتیبان رمزگذاری نشده است.", color = c.muted, style = MaterialTheme.typography.bodyMedium)
            KButton("ساخت فایل پشتیبان", { host.exportBackup() }, Modifier.fillMaxWidth())
        }
        KCard {
            Text("بازیابی", style = MaterialTheme.typography.titleMedium, color = c.text)
            Text("اطلاعات فعلی کاردان با محتوای فایل جایگزین می‌شود. یادآوری‌ها دوباره تنظیم می‌شوند.", color = c.muted, style = MaterialTheme.typography.bodyMedium)
            KOutlineButton("انتخاب فایل پشتیبان", { confirmImport = true }, Modifier.fillMaxWidth())
        }
        Text("برای انتقال به گوشی جدید: فایل را برای خودت در یک پیام‌رسان بفرست، در گوشی جدید کاردان را نصب کن و بازیابی را بزن. راهنمای کامل انتقال در بخش «فایل و پشتیبان» هست.", color = c.muted, style = MaterialTheme.typography.bodySmall)
        KOutlineButton("راهنمای انتقال اطلاعات", { state.goTab(1) }, Modifier.fillMaxWidth())
    }
    if (confirmImport) AlertDialog(
        onDismissRequest = { confirmImport = false },
        title = { Text("جایگزینی اطلاعات؟") },
        text = { Text("اطلاعات فعلی با فایل انتخابی جایگزین می‌شود. ادامه می‌دهی؟") },
        confirmButton = { TextButton(onClick = { confirmImport = false; host.importBackup() }) { Text("ادامه") } },
        dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("انصراف") } }
    )
}

@Composable
fun ContactsScreen(state: AppState, title: String, onBack: () -> Unit) {
    val c = K.c
    val host = LocalHost.current
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    Page(title, "فقط شماره‌هایی که خودت اینجا می‌نویسی؛ به مخاطبان گوشی دسترسی نمی‌گیرد", onBack) {
        KCard {
            KField(name, { name = it }, "نام (مثلاً پسرم)")
            KField(number, { number = it }, "شماره", keyboard = KeyboardType.Phone)
            KButton("افزودن", {
                val n = Persian.latinDigits(number).filter { it.isDigit() || it == '+' }
                if (name.isBlank() || n.length < 3) state.say("نام و یک شمارهٔ معتبر وارد کن.") else { state.contacts.add(QuickContact(state.newId(), name.trim(), n)); state.saveContacts(); name = ""; number = "" }
            }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add)
        }
        if (state.contacts.isEmpty()) EmptyState(Icons.Filled.Contacts, "هنوز شماره‌ای نیست", "شماره‌های مهم (خانواده، اورژانس، پزشک) را اضافه کن تا با یک لمس شماره‌گیر باز شود.")
        state.contacts.toList().forEachIndexed { i, ct ->
            KCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ct.name, style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(Persian.digits(ct.number), color = c.muted)
                    }
                    IconButton(onClick = { host.dial(ct.number) }) { Icon(Icons.Filled.Call, "تماس", tint = c.ok) }
                    IconButton(onClick = { host.sms(ct.number) }) { Icon(Icons.Filled.Message, "پیامک", tint = c.accent) }
                    IconButton(onClick = { state.contacts.removeAt(i); state.saveContacts() }) { Icon(Icons.Filled.Delete, "حذف", tint = c.muted) }
                }
            }
        }
        Text("دکمهٔ تماس فقط شماره‌گیر را باز می‌کند و تماس را خودت می‌گیری.", color = c.muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun MoreScreen(state: AppState) {
    val c = K.c
    Page("بیشتر", "تنظیمات، پشتیبان و ابزارهای دیگر") {
        listOf(AppRoute.SETTINGS, AppRoute.VAULT, AppRoute.DEVICE, AppRoute.BACKUP, AppRoute.CONTACTS, AppRoute.MOVIES).forEach { r ->
            val t = Tools.info(r)!!
            KCard(onClick = { state.openRoute(r) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(t.icon, t.tint(c))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(t.desc, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
            }
        }
        KCard {
            Text("کاردان $APP_VERSION_NOTE", color = c.text, fontWeight = FontWeight.Bold)
            Text("همهٔ اطلاعات روی همین گوشی می‌ماند. کاردان بدون تأیید تو چیزی نمی‌خرد، نمی‌فرستد و پاک نمی‌کند.", color = c.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
