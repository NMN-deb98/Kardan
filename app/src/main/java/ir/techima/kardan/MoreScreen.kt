package ir.techima.kardan

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val MorePanel = Color(0xFF1B2638)
private val MoreMint = Color(0xFF55E0BC)
private val MoreMuted = Color(0xFF9EADC2)
private const val MORE_PREFS = "kardan_local"
private const val VAULT_KEY = "vault_encrypted"
private const val VAULT_ALIAS = "kardan_vault_key"
private data class SavedLogin(val id: Int, val label: String, val username: String, val password: String)

private fun vaultSecretKey(): SecretKey {
    val store = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(VAULT_ALIAS, null) as? SecretKey)?.let { return it }
    val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
    generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
        VAULT_ALIAS,
        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
    ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true).build())
    return generator.generateKey()
}

private fun saveVault(context: Context, entries: List<SavedLogin>) {
    val array = JSONArray()
    entries.forEach { array.put(JSONObject().put("id", it.id).put("label", it.label).put("username", it.username).put("password", it.password)) }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, vaultSecretKey())
    val encrypted = cipher.doFinal(array.toString().toByteArray(Charsets.UTF_8))
    val value = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
    context.getSharedPreferences(MORE_PREFS, Context.MODE_PRIVATE).edit().putString(VAULT_KEY, value).apply()
}

private fun loadVault(context: Context): List<SavedLogin> {
    return try {
        val raw = context.getSharedPreferences(MORE_PREFS, Context.MODE_PRIVATE).getString(VAULT_KEY, null)
            ?: return emptyList()
        val parts = raw.split(":", limit = 2)
        if (parts.size != 2) return emptyList()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, vaultSecretKey(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        val array = JSONArray(String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8))
        (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            SavedLogin(item.getInt("id"), item.getString("label"), item.getString("username"), item.getString("password"))
        }
    } catch (_: Exception) { emptyList() }
}

@Composable
internal fun MoreScreen(context: Context) {
    var section by remember { mutableStateOf("بیشتر") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var logins by remember { mutableStateOf(loadVault(context)) }
    var nextId by remember { mutableIntStateOf((logins.maxOfOrNull { it.id } ?: 0) + 1) }
    val guides = listOf(
        "راهنمای سالمندان" to "اندازه نوشته را از تنظیمات نمایش افزایش دهید، مخاطبان مهم را به علاقه‌مندی‌ها اضافه کنید و برای تماس تصویری از برنامه معتبر استفاده کنید. رمز یک‌بارمصرف و اطلاعات بانکی را به کسی نگویید.",
        "خرید امن بسته اینترنت" to "بسته را فقط از برنامه یا وب‌سایت رسمی اپراتور بخرید. پیش از تأیید، حجم، مدت اعتبار و مبلغ نهایی را بررسی کنید.",
        "امنیت گوشی" to "قفل صفحه و به‌روزرسانی سیستم را فعال نگه دارید، برنامه را از منبع معتبر نصب کنید و پیش از باز کردن لینک پیامک، فرستنده را بررسی کنید."
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("امکانات بیشتر", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("دستیار", "رمزها", "ویدئوها", "راهنما").forEach { name ->
                    FilterChip(selected = section == name, onClick = { section = name }, label = { Text(name) })
                }
            }
        }
        when (section) {
            "دستیار" -> {
                item { Text("دستیار آفلاین کاردان", color = Color.White, fontWeight = FontWeight.Bold) }
                item { Text("پاسخ‌های پایه بر اساس راهنماهای داخلی؛ این نسخه از مدل هوش مصنوعی آنلاین استفاده نمی‌کند.", color = MoreMuted) }
                item { OutlinedTextField(value = question, onValueChange = { question = it }, modifier = Modifier.fillMaxWidth(), label = { Text("سؤالت را بنویس") }) }
                item {
                    Button(onClick = {
                        val q = question.lowercase()
                        answer = when {
                            listOf("رمز", "امنیت", "هک").any { q.contains(it) } -> "برای هر حساب رمز جداگانه انتخاب کن، تأیید دومرحله‌ای را روشن کن و رمز یا کد یک‌بارمصرف را با دیگران به اشتراک نگذار."
                            listOf("اینترنت", "بسته").any { q.contains(it) } -> "بسته را از اپ رسمی اپراتور بخر و حجم، اعتبار و مبلغ نهایی را پیش از پرداخت بررسی کن."
                            listOf("سالمند", "فونت", "بزرگ").any { q.contains(it) } -> "اندازه متن را در تنظیمات نمایش افزایش بده و مخاطبان ضروری را در علاقه‌مندی‌ها قرار بده."
                            listOf("کار", "برنامه", "بهره‌وری").any { q.contains(it) } -> "کار را به یک قدم کوچک تقسیم کن، در بخش کارهای من ثبت کن و بعد از انجام علامت بزن."
                            else -> "راهنمای آفلاین محدود است. درباره امنیت، اینترنت، سالمندان یا مدیریت کار بپرس."
                        }
                    }, enabled = question.isNotBlank()) { Text("پاسخ") }
                }
                if (answer.isNotBlank()) item { Text(answer, modifier = Modifier.fillMaxWidth().background(MorePanel, RoundedCornerShape(14.dp)).padding(14.dp), color = Color.White) }
            }
            "رمزها" -> {
                item { Text("خزانه رمزها", color = Color.White, fontWeight = FontWeight.Bold) }
                item { Text("اطلاعات در حافظه برنامه و با AES-GCM رمزنگاری می‌شود. این نسخه قفل جداگانه با رمز اصلی ندارد؛ برای رمزهای بسیار حساس از مدیر رمز معتبر استفاده کنید.", color = MoreMuted, fontSize = 12.sp) }
                item { OutlinedTextField(value = label, onValueChange = { label = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام سایت یا حساب") }, singleLine = true) }
                item { OutlinedTextField(value = username, onValueChange = { username = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام کاربری") }, singleLine = true) }
                item { OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("رمز عبور") }, singleLine = true, visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { reveal = !reveal }) { Text(if (reveal) "پنهان‌کردن" else "نمایش رمز") }
                        Button(onClick = {
                            if (label.isNotBlank() && password.isNotBlank()) {
                                logins = logins + SavedLogin(nextId, label.trim(), username.trim(), password)
                                nextId += 1
                                saveVault(context, logins)
                                label = ""; username = ""; password = ""
                            }
                        }, enabled = label.isNotBlank() && password.isNotBlank()) { Text("ذخیره") }
                    }
                }
                items(logins, key = { it.id }) { login ->
                    Column(Modifier.fillMaxWidth().background(MorePanel, RoundedCornerShape(14.dp)).padding(12.dp)) {
                        Text(login.label, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("نام کاربری: " + login.username, color = MoreMuted)
                        Text("رمز: " + if (reveal) login.password else "••••••••", color = MoreMint)
                        TextButton(onClick = { logins = logins.filterNot { it.id == login.id }; saveVault(context, logins) }) { Text("حذف") }
                    }
                }
            }
            "ویدئوها" -> {
                item { Text("آموزش ویدئویی", color = Color.White, fontWeight = FontWeight.Bold) }
                item { Text("برای پخش به اینترنت نیاز است؛ پیوندها جست‌وجوی ویدئوهای مرتبط را باز می‌کنند.", color = MoreMuted) }
                listOf(
                    "آموزش امنیت موبایل" to "https://www.youtube.com/results?search_query=آموزش+امنیت+موبایل",
                    "آموزش گوشی برای سالمندان" to "https://www.youtube.com/results?search_query=آموزش+گوشی+برای+سالمندان",
                    "مدیریت هزینه‌های روزانه" to "https://www.youtube.com/results?search_query=آموزش+مدیریت+هزینه"
                ).forEach { (title, url) ->
                    item { Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }, modifier = Modifier.fillMaxWidth()) { Text(title) } }
                }
            }
            "راهنما" -> {
                item { Text("راهنماهای ساده", color = Color.White, fontWeight = FontWeight.Bold) }
                items(guides) { guide ->
                    Column(Modifier.fillMaxWidth().background(MorePanel, RoundedCornerShape(14.dp)).padding(14.dp)) {
                        Text(guide.first, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(guide.second, color = MoreMuted)
                        if (guide.first == "خرید امن بسته اینترنت") {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://my.mci.ir/"))) }) { Text("همراه اول") }
                                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://my.irancell.ir/"))) }) { Text("ایرانسل") }
                                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://my.rightel.ir/"))) }) { Text("رایتل") }
                            }
                        }
                    }
                }
            }
            else -> {
                item { Text("دستیار آفلاین", color = Color.White, fontWeight = FontWeight.Bold); Text("راهنمایی سریع برای موضوعات پایه", color = MoreMuted); TextButton(onClick = { section = "دستیار" }) { Text("باز کردن") } }
                item { Text("خزانه رمزها", color = Color.White, fontWeight = FontWeight.Bold); Text("ذخیره محلی رمزنگاری‌شده", color = MoreMuted); TextButton(onClick = { section = "رمزها" }) { Text("باز کردن") } }
                item { Text("ویدئوهای آموزشی", color = Color.White, fontWeight = FontWeight.Bold); Text("آموزش‌های مرتبط در وب", color = MoreMuted); TextButton(onClick = { section = "ویدئوها" }) { Text("مشاهده") } }
                item { Text("راهنمای سالمندان و اینترنت", color = Color.White, fontWeight = FontWeight.Bold); TextButton(onClick = { section = "راهنما" }) { Text("مشاهده راهنماها") } }
            }
        }
    }
}
