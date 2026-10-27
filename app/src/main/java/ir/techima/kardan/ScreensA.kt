package ir.techima.kardan

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun DeviceScreen(state: AppState, title: String, onBack: () -> Unit) {
    val c = K.c
    val host = LocalHost.current
    val ctx = state.ctx
    val stat = StatFs(Environment.getDataDirectory().path)
    val total = stat.totalBytes; val free = stat.availableBytes
    val freeFrac = if (total > 0) free.toDouble() / total else 1.0
    val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    val bat = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = bat?.let { val l = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); val s = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100); if (l >= 0) l * 100 / s else -1 } ?: -1
    val charging = bat?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val temp = (bat?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1) / 10.0
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = try { cm.getNetworkCapabilities(cm.activeNetwork) } catch (e: Exception) { null }
    val net = when {
        caps == null -> "بدون اتصال"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "وای‌فای"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "اینترنت همراه"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "کابلی"
        else -> "متصل"
    }
    val dm = ctx.resources.displayMetrics
    val ver = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName } catch (e: Exception) { "—" }

    @Composable fun line(k: String, v: String) {
        Row(Modifier.fillMaxWidth()) {
            Text(k, color = c.muted, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(v, color = c.text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
    }

    Page(title, "فقط اطلاعاتی که خود اندروید بدون مجوز ویژه می‌دهد", onBack) {
        KCard {
            Text("حافظهٔ ذخیره‌سازی", style = MaterialTheme.typography.titleMedium, color = c.text)
            LinearProgressIndicator(progress = { (1.0 - freeFrac).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = if (freeFrac < 0.1) c.danger else c.primary, trackColor = c.cardHigh)
            Text(Persian.bytes(free) + " آزاد از " + Persian.bytes(total), color = c.muted)
            if (freeFrac < 0.1) Text("کمتر از ۱۰٪ حافظه آزاد است؛ ممکن است گوشی کند شود. راهنمای «حافظه پر شده» را ببین.", color = c.warn, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KOutlineButton("تنظیمات حافظه", { host.openSettings(Target.STORAGE) })
                KOutlineButton("راهنما", { state.push(Screen.GuideDetail("storage-full")) })
            }
        }
        KCard {
            Text("باتری و شبکه", style = MaterialTheme.typography.titleMedium, color = c.text)
            line("شارژ", if (level >= 0) Persian.digits(level) + "٪" + if (charging != 0) " (در حال شارژ)" else "" else "نامشخص")
            if (temp > 0) line("دمای باتری", Persian.digits(String.format(java.util.Locale.US, "%.1f", temp)).replace('.', '٫') + " درجه")
            line("اتصال", net)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KOutlineButton("تنظیمات باتری", { host.openSettings(Target.BATTERY_SAVER) })
                KOutlineButton("وای‌فای", { host.openSettings(Target.WIFI) })
            }
        }
        KCard {
            Text("دستگاه", style = MaterialTheme.typography.titleMedium, color = c.text)
            line("مدل", Build.MANUFACTURER.replaceFirstChar { it.uppercase() } + " " + Build.MODEL)
            line("نسخهٔ اندروید", Persian.digits(Build.VERSION.RELEASE) + " (API " + Persian.digits(Build.VERSION.SDK_INT) + ")")
            if (Build.VERSION.SDK_INT >= 23) line("وصلهٔ امنیتی", Persian.digits(Build.VERSION.SECURITY_PATCH))
            line("حافظهٔ رم", Persian.bytes(mi.totalMem) + " (آزاد: " + Persian.bytes(mi.availMem) + ")")
            line("صفحه", Persian.digits(dm.widthPixels) + "×" + Persian.digits(dm.heightPixels) + " پیکسل")
            line("نسخهٔ کاردان", Persian.digits(ver))
            KOutlineButton("اطلاعات کامل در تنظیمات گوشی", { host.openSettings(Target.DEVICE_INFO) }, Modifier.fillMaxWidth())
        }
        Text("کاردان اطلاعات شخصی (شمارهٔ سریال، IMEI، مخاطبان) نمی‌خواند.", color = c.muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun VaultScreen(state: AppState, title: String, onBack: () -> Unit) {
    val c = K.c
    val host = LocalHost.current
    var unlocked by remember { mutableStateOf(VaultLock.isOpen()) }
    var tick by remember { mutableStateOf(0) }
    val items = remember { mutableStateListOf<VaultItem>() }
    var broken by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<VaultItem?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<VaultItem?>(null) }

    DisposableEffect(Unit) { host.setSecure(true); onDispose { host.setSecure(false) } }
    LaunchedEffect(unlocked) {
        if (unlocked) {
            val l = VaultCrypto.load(state.ctx)
            items.clear()
            if (l == null) broken = true else { broken = false; items.addAll(l) }
            while (VaultLock.isOpen()) { delay(2000); tick++ }
            unlocked = false; items.clear()
        }
    }

    Page(title, "رمزگذاری AES-256 با کلید امن اندروید؛ هیچ‌چیز ارسال یا همگام نمی‌شود", onBack) {
        if (!unlocked) {
            KCard {
                IconBadge(Icons.Filled.Lock, c.accent, 56.dp)
                Text("گاوصندوق قفل است", style = MaterialTheme.typography.titleMedium, color = c.text)
                Text("برای باز کردن، باید قفل صفحهٔ گوشی (رمز، الگو یا اثر انگشت) را تأیید کنی. بعد از ۲ دقیقه یا خروج از برنامه خودکار قفل می‌شود.", color = c.muted, style = MaterialTheme.typography.bodyMedium)
                KButton("باز کردن گاوصندوق", {
                    host.confirmDeviceCredential("گاوصندوق کاردان") { ok -> if (ok) { VaultLock.unlock(); unlocked = true } else state.say("تأیید هویت انجام نشد.") }
                }, Modifier.fillMaxWidth())
            }
            Text("ملاحظات امنیتی: اگر قفل صفحهٔ گوشی را عوض کنی یا گوشی را بازنشانی کنی، ممکن است رمزهای ذخیره‌شده دیگر قابل بازیابی نباشند. نسخهٔ پشتیبان کاردان عمداً شامل گاوصندوق نیست. ضمانت امنیت مطلق وجود ندارد؛ برای حساب‌های بسیار حساس از ابزار تخصصی مدیریت رمز هم استفاده کن.", color = c.muted, style = MaterialTheme.typography.bodySmall)
        } else {
            if (broken) KCard { Text("خواندن گاوصندوق ممکن نشد (کلید امن تغییر کرده است). می‌توانی گاوصندوق را خالی و از نو شروع کنی.", color = c.danger); KOutlineButton("خالی کردن گاوصندوق", { VaultCrypto.wipe(state.ctx); items.clear(); broken = false }, danger = true) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton("رمز جدید", { adding = true }, Modifier.weight(1f), icon = Icons.Filled.Add)
                KOutlineButton("قفل کن", { VaultLock.lock(); unlocked = false; items.clear() }, Modifier.weight(1f))
            }
            if (items.isEmpty() && !broken) EmptyState(Icons.Filled.Lock, "گاوصندوق خالی است", "اولین رمزت را اضافه کن؛ فقط رمزگذاری‌شده روی همین گوشی نگه داشته می‌شود.")
            items.toList().forEach { it2 ->
                var show by remember(it2.id) { mutableStateOf(false) }
                KCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(it2.title, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                        IconButton(onClick = { show = !show }) { Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "نمایش رمز", tint = c.muted) }
                        IconButton(onClick = { deleting = it2 }) { Icon(Icons.Filled.Delete, "حذف", tint = c.muted) }
                    }
                    if (it2.user.isNotBlank()) Text("نام کاربری: " + it2.user, color = c.muted, style = MaterialTheme.typography.bodyMedium)
                    Text(if (show) it2.pass else "••••••••••", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (it2.note.isNotBlank()) Text(it2.note, color = c.muted, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KOutlineButton("کپی رمز", { host.copySensitive("رمز", it2.pass) })
                        if (it2.user.isNotBlank()) KOutlineButton("کپی نام کاربری", { host.copySensitive("نام کاربری", it2.user) })
                        KOutlineButton("ویرایش", { editing = it2 })
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        val e = editing
        var t by remember(e, adding) { mutableStateOf(e?.title ?: "") }
        var u by remember(e, adding) { mutableStateOf(e?.user ?: "") }
        var p by remember(e, adding) { mutableStateOf(e?.pass ?: "") }
        var n by remember(e, adding) { mutableStateOf(e?.note ?: "") }
        AlertDialog(
            onDismissRequest = { adding = false; editing = null },
            title = { Text(if (e == null) "رمز جدید" else "ویرایش رمز") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KField(t, { t = it }, "عنوان (مثلاً حساب بانکی)")
                    KField(u, { u = it }, "نام کاربری")
                    KField(p, { p = it }, "رمز", trailing = { TextButton(onClick = { p = PassGen.generate(16, true, true, true) }) { Text("بساز") } })
                    KField(n, { n = it }, "یادداشت", singleLine = false)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (t.isBlank() || p.isBlank()) { state.say("عنوان و رمز لازم است."); return@TextButton }
                    val item = VaultItem(e?.id ?: state.newId(), t.trim(), u.trim(), p, n.trim())
                    val idx = items.indexOfFirst { it.id == item.id }
                    if (idx >= 0) items[idx] = item else items.add(0, item)
                    if (!VaultCrypto.save(state.ctx, items.toList())) state.say("ذخیرهٔ رمزگذاری‌شده ناموفق بود.") else state.say("ذخیره شد.")
                    adding = false; editing = null
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { adding = false; editing = null }) { Text("انصراف") } }
        )
    }
    deleting?.let { d ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف رمز؟") },
            text = { Text("«${d.title}» برای همیشه از گاوصندوق پاک می‌شود و قابل بازگشت نیست.") },
            confirmButton = { TextButton(onClick = { items.removeAll { it.id == d.id }; VaultCrypto.save(state.ctx, items.toList()); deleting = null; state.say("حذف شد.") }) { Text("حذف", color = c.danger) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("انصراف") } }
        )
    }
}
