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
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PAGE = 40

@Composable
fun ContactsScreen(state: AppState, title: String, onBack: () -> Unit) {
    val c = K.c
    val host = LocalHost.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var q by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf(PAGE) }
    var why by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var granted by remember { mutableStateOf(host.hasContactsPermission()) }

    fun load() {
        loading = true
        scope.launch {
            val list = withContext(Dispatchers.IO) { PhoneContacts.load(state.ctx) }
            state.phoneContacts = list; loading = false
        }
    }
    // if permission was given earlier, show the list again when this screen opens
    LaunchedEffect(Unit) { if (granted && state.phoneContacts == null) load() }

    fun addFavorite(n: String, num: String) {
        val clean = Persian.latinDigits(num).filter { it.isDigit() || it == '+' }
        if (state.contacts.any { PhoneContacts.key(it.number) == PhoneContacts.key(clean) }) { state.say("این شماره از قبل در منتخب‌هاست."); return }
        state.contacts.add(QuickContact(state.newId(), n, clean)); state.saveContacts(); state.say("«$n» به منتخب‌ها اضافه شد.")
    }

    Page(title, "مخاطبان منتخب و مخاطبان گوشی؛ همه‌چیز فقط روی همین دستگاه", onBack) {
        // ------------------------------------------------------------ phone contacts
        KCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Filled.Contacts, c.accent)
                Column(Modifier.weight(1f)) {
                    Text("مخاطبان گوشی", style = MaterialTheme.typography.titleMedium, color = c.text)
                    Text(if (granted) "مجوز مخاطبان داده شده است." else "برای دیدن و جست‌وجوی مخاطبان گوشی، اول دلیلش را بگو و اجازه بگیر.", style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
            }
            if (!granted) KButton("دیدن مخاطبان گوشی", { why = true }, Modifier.fillMaxWidth(), icon = Icons.Filled.Lock)
            if (denied) Text("مجوز داده نشد، پس مخاطبان گوشی خوانده نمی‌شوند. می‌توانی از تنظیمات گوشی ← برنامه‌ها ← کاردان ← مجوزها، «مخاطبان» را روشن کنی، یا شماره‌ها را پایین دستی اضافه کنی.", style = MaterialTheme.typography.bodySmall, color = c.warn)
            KOutlineButton("سابقهٔ تماس (در برنامهٔ تلفن گوشی)", { host.openCallLog() }, Modifier.fillMaxWidth(), icon = Icons.Filled.History)
            Text("کاردان خودش سابقهٔ تماس را نمی‌خواند؛ این دکمه فقط سابقهٔ برنامهٔ تلفن گوشی را باز می‌کند.", style = MaterialTheme.typography.bodySmall, color = c.muted)
        }

        if (granted) {
            val all = state.phoneContacts
            KField(q, { q = it; shown = PAGE }, "جست‌وجوی مخاطب (نام یا شماره)", trailing = { Icon(Icons.Filled.Search, null, tint = c.muted) })
            if (loading || all == null) Text("در حال خواندن مخاطبان…", color = c.muted)
            else {
                val list = PhoneContacts.filter(all, q)
                Text(Persian.digits(list.size) + " مخاطب" + if (q.isNotBlank()) " (از ${Persian.digits(all.size)})" else "", style = MaterialTheme.typography.labelLarge, color = c.muted)
                if (all.isEmpty()) EmptyState(Icons.Filled.Contacts, "مخاطبی پیدا نشد", "مخاطبی با شمارهٔ تلفن در گوشی نیست، یا مجوز کامل نیست.")
                else if (list.isEmpty()) EmptyState(Icons.Filled.Search, "نتیجه‌ای نیست", "نام یا شمارهٔ دیگری را امتحان کن.")
                list.take(shown).forEach { pc ->
                    val fav = state.contacts.any { PhoneContacts.key(it.number) == PhoneContacts.key(pc.number) }
                    KCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(pc.name, style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                LtrText(Persian.digits(pc.number), color = c.muted)
                            }
                            IconButton(onClick = { if (fav) state.say("این مخاطب از قبل در منتخب‌هاست.") else addFavorite(pc.name, pc.number) }) {
                                Icon(if (fav) Icons.Filled.Star else Icons.Filled.StarBorder, if (fav) "در منتخب‌ها" else "افزودن به منتخب‌ها", tint = if (fav) c.warn else c.muted)
                            }
                            IconButton(onClick = { host.dial(pc.number) }) { Icon(Icons.Filled.Call, "تماس", tint = c.ok) }
                            IconButton(onClick = { host.sms(pc.number) }) { Icon(Icons.Filled.Message, "پیامک", tint = c.accent) }
                        }
                    }
                }
                if (list.size > shown) KOutlineButton("نمایش ${Persian.digits(minOf(PAGE, list.size - shown))} مورد بعدی", { shown += PAGE }, Modifier.fillMaxWidth())
            }
        }

        // ------------------------------------------------------------ quick (favourite) contacts
        SectionTitle("مخاطبان منتخب")
        KCard {
            KField(name, { name = it }, "نام (مثلاً پسرم)")
            KField(number, { number = it }, "شماره", keyboard = KeyboardType.Phone)
            KButton("افزودن", {
                val n = Persian.latinDigits(number).filter { it.isDigit() || it == '+' }
                if (name.isBlank() || n.length < 3) state.say("نام و یک شمارهٔ معتبر وارد کن.") else { addFavorite(name.trim(), n); name = ""; number = "" }
            }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add)
        }
        if (state.contacts.isEmpty()) EmptyState(Icons.Filled.Contacts, "هنوز منتخبی نیست", "شماره‌های مهم (خانواده، اورژانس، پزشک) را اضافه کن تا با یک لمس شماره‌گیر باز شود.")
        state.contacts.toList().forEachIndexed { i, ct ->
            KCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ct.name, style = MaterialTheme.typography.titleMedium, color = c.text)
                        LtrText(Persian.digits(ct.number), color = c.muted)
                    }
                    IconButton(onClick = { host.dial(ct.number) }) { Icon(Icons.Filled.Call, "تماس", tint = c.ok) }
                    IconButton(onClick = { host.sms(ct.number) }) { Icon(Icons.Filled.Message, "پیامک", tint = c.accent) }
                    IconButton(onClick = { state.contacts.removeAt(i); state.saveContacts() }) { Icon(Icons.Filled.Delete, "حذف", tint = c.muted) }
                }
            }
        }
        Text("دکمهٔ تماس و پیامک فقط شماره‌گیر یا برنامهٔ پیام را با همان شماره باز می‌کند؛ تماس و ارسال را خودت انجام می‌دهی. کاردان هرگز بدون درخواست و تأیید تو تماس نمی‌گیرد و پیامک نمی‌فرستد.", color = c.muted, style = MaterialTheme.typography.bodySmall)
    }

    if (why) AlertDialog(
        onDismissRequest = { why = false },
        title = { Text("چرا کاردان به مخاطبان نیاز دارد؟") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• تا مخاطبان گوشی‌ات را داخل کاردان ببینی، جست‌وجو کنی و با یک لمس شماره‌گیر یا پیام را باز کنی.")
                Text("• نام‌ها و شماره‌ها فقط وقتی این صفحه باز است خوانده می‌شود؛ به هیچ سرور یا سرویسی فرستاده نمی‌شود.")
                Text("• از میان همهٔ مخاطبان، فقط کسانی که خودت ستاره بزنی (منتخب) روی کاردان ذخیره می‌شوند.")
                Text("• کاردان سابقهٔ تماس را نمی‌خواند و بدون درخواست و تأیید تو تماس یا پیامک نمی‌فرستد.")
                Text("• هر وقت خواستی از تنظیمات گوشی ← برنامه‌ها ← کاردان ← مجوزها قطعش کن.")
                Text("اگر موافق نیستی «فعلاً نه» را بزن؛ بقیهٔ برنامه کار می‌کند و می‌توانی شماره‌ها را دستی اضافه کنی.", color = c.muted)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                why = false
                host.requestContacts { ok -> granted = ok; denied = !ok; if (ok) { denied = false; load() } }
            }) { Text("اجازه می‌دهم") }
        },
        dismissButton = { TextButton(onClick = { why = false }) { Text("فعلاً نه") } }
    )
}
