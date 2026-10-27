package ir.techima.kardan

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.SecureRandom
import java.util.TimeZone

object PassGen {
    const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    const val DIGIT = "23456789"
    const val SYMBOL = "!@#\$%^&*-_=+?"

    fun generate(length: Int, upper: Boolean, digits: Boolean, symbols: Boolean, rnd: SecureRandom = SecureRandom()): String {
        val sets = mutableListOf(LOWER)
        if (upper) sets.add(UPPER); if (digits) sets.add(DIGIT); if (symbols) sets.add(SYMBOL)
        val all = sets.joinToString("")
        val out = CharArray(length.coerceAtLeast(sets.size))
        for (i in out.indices) out[i] = all[rnd.nextInt(all.length)]
        // guarantee at least one char of each chosen set
        sets.forEachIndexed { i, s -> out[i] = s[rnd.nextInt(s.length)] }
        for (i in out.size - 1 downTo 1) { val j = rnd.nextInt(i + 1); val t = out[i]; out[i] = out[j]; out[j] = t }
        return String(out)
    }

    /** Rough entropy in bits for the chosen pool. */
    fun bits(length: Int, upper: Boolean, digits: Boolean, symbols: Boolean): Double {
        var pool = LOWER.length
        if (upper) pool += UPPER.length; if (digits) pool += DIGIT.length; if (symbols) pool += SYMBOL.length
        return length * (Math.log(pool.toDouble()) / Math.log(2.0))
    }

    fun strength(bits: Double): String = when {
        bits < 40 -> "ضعیف"; bits < 60 -> "متوسط"; bits < 80 -> "قوی"; else -> "بسیار قوی"
    }
}

@Composable
fun PassGenTool(title: String, onBack: () -> Unit) {
    val c = K.c
    val host = LocalHost.current
    var len by remember { mutableStateOf(16f) }
    var up by remember { mutableStateOf(true) }
    var dg by remember { mutableStateOf(true) }
    var sy by remember { mutableStateOf(true) }
    var pass by remember { mutableStateOf(PassGen.generate(16, true, true, true)) }
    val bits = PassGen.bits(len.toInt(), up, dg, sy)
    Page(title, "با SecureRandom ساخته می‌شود و هیچ‌جا ارسال نمی‌شود", onBack) {
        KCard {
            Text(pass, fontFamily = FontFamily.Monospace, fontSize = 20.sp, color = c.text, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("قدرت: " + PassGen.strength(bits) + " (حدود " + Persian.digits(bits.toInt()) + " بیت)", color = if (bits >= 60) c.ok else c.warn, style = MaterialTheme.typography.bodyMedium)
        }
        Text("طول رمز: " + Persian.digits(len.toInt()), color = c.text)
        Slider(value = len, onValueChange = { len = it; pass = PassGen.generate(it.toInt(), up, dg, sy) }, valueRange = 8f..40f, steps = 31, colors = SliderDefaults.colors(thumbColor = c.primary, activeTrackColor = c.primary))
        listOf(
            Triple("حروف بزرگ", up, { v: Boolean -> up = v }),
            Triple("ارقام", dg, { v: Boolean -> dg = v }),
            Triple("نمادها", sy, { v: Boolean -> sy = v })
        ).forEach { (t, v, set) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t, color = c.text, modifier = Modifier.weight(1f))
                Switch(checked = v, onCheckedChange = { set(it); pass = PassGen.generate(len.toInt(), up, dg, sy) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KButton("رمز تازه", { pass = PassGen.generate(len.toInt(), up, dg, sy) }, Modifier.weight(1f))
            KOutlineButton("کپی", { host.copySensitive("رمز", pass) }, Modifier.weight(1f))
        }
        Text("حروف مشابه (مثل l و ۱ یا O و ۰) عمداً حذف شده‌اند تا خواندن رمز راحت‌تر باشد.", color = c.muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun DateConvTool(title: String, onBack: () -> Unit) {
    val c = K.c
    val today = Jalali.fromMillis(System.currentTimeMillis())
    var toJalali by remember { mutableStateOf(false) }
    var y by remember { mutableStateOf(Persian.digits(today.year)) }
    var m by remember { mutableStateOf(Persian.digits(today.month)) }
    var d by remember { mutableStateOf(Persian.digits(today.day)) }
    val yi = Persian.latinDigits(y).toIntOrNull(); val mi = Persian.latinDigits(m).toIntOrNull(); val di = Persian.latinDigits(d).toIntOrNull()
    var result = "تاریخ را کامل وارد کن."
    var weekday = ""
    var diffText = ""
    if (yi != null && mi != null && di != null && mi in 1..12 && di >= 1) {
        if (!toJalali) {
            if (yi in 1..3000 && di <= Jalali.daysInMonth(yi, mi)) {
                val g = Jalali.toGregorian(yi, mi, di)
                result = Persian.digits(String.format(java.util.Locale.US, "%04d/%02d/%02d", g[0], g[1], g[2])) + " میلادی"
                val cal = java.util.GregorianCalendar(g[0], g[1] - 1, g[2])
                weekday = Jalali.weekDays[Jalali.weekDayIndex(cal.timeInMillis)]
                val diff = Math.round((cal.timeInMillis - Jalali.startOfDay(System.currentTimeMillis())).toDouble() / 86_400_000.0)
                diffText = if (diff == 0L) "امروز" else if (diff > 0) Persian.digits(diff) + " روز دیگر" else Persian.digits(-diff) + " روز پیش"
            } else result = "این تاریخ شمسی وجود ندارد."
        } else {
            val ok = yi in 1..3000 && mi in 1..12 && di <= java.util.GregorianCalendar(yi, mi - 1, 1).getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            if (ok) {
                val j = Jalali.fromGregorian(yi, mi, di)
                result = Persian.digits(String.format(java.util.Locale.US, "%04d/%02d/%02d", j.year, j.month, j.day)) + " شمسی (" + Persian.digits(j.day) + " " + Jalali.monthNames[j.month - 1] + " " + Persian.digits(j.year) + ")"
                val cal = java.util.GregorianCalendar(yi, mi - 1, di)
                weekday = Jalali.weekDays[Jalali.weekDayIndex(cal.timeInMillis)]
                val diff = Math.round((cal.timeInMillis - Jalali.startOfDay(System.currentTimeMillis())).toDouble() / 86_400_000.0)
                diffText = if (diff == 0L) "امروز" else if (diff > 0) Persian.digits(diff) + " روز دیگر" else Persian.digits(-diff) + " روز پیش"
            } else result = "این تاریخ میلادی وجود ندارد."
        }
    }
    Page(title, "امروز: " + Jalali.full(System.currentTimeMillis()), onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("شمسی ← میلادی", !toJalali) { toJalali = false }
            Chip("میلادی ← شمسی", toJalali) { toJalali = true }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KField(y, { y = it }, "سال", Modifier.weight(1.3f), keyboard = KeyboardType.Number)
            KField(m, { m = it }, "ماه", Modifier.weight(1f), keyboard = KeyboardType.Number)
            KField(d, { d = it }, "روز", Modifier.weight(1f), keyboard = KeyboardType.Number)
        }
        KCard {
            Text(result, color = c.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (weekday.isNotEmpty()) Text(weekday + " · " + diffText, color = c.muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun TextTool(title: String, onBack: () -> Unit) {
    val c = K.c
    val host = LocalHost.current
    var text by remember { mutableStateOf("") }
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    val lines = if (text.isEmpty()) 0 else text.lines().size
    Page(title, "همه‌چیز روی خود گوشی انجام می‌شود", onBack) {
        KField(text, { text = it }, "متن را بنویس یا بچسبان", singleLine = false, minLines = 5)
        KCard {
            Text("نویسه: " + Persian.digits(text.length) + " · کلمه: " + Persian.digits(words) + " · خط: " + Persian.digits(lines), color = c.text)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("ارقام فارسی", false) { text = Persian.digits(text) }
            Chip("ارقام انگلیسی", false) { text = Persian.latinDigits(text) }
            Chip("اصلاح ی و ک", false) { text = text.replace('ي', 'ی').replace('ك', 'ک').replace('ى', 'ی') }
            Chip("حذف فاصلهٔ اضافی", false) { text = text.replace(Regex("[ \\t]+"), " ").replace(Regex("\\n{3,}"), "\n\n").trim() }
            Chip("پاک کردن", false) { text = "" }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KOutlineButton("کپی", { if (text.isNotEmpty()) host.copySensitive("متن", text) }, Modifier.weight(1f))
            KOutlineButton("اشتراک", { if (text.isNotEmpty()) host.share(text) }, Modifier.weight(1f))
        }
    }
}

@Composable
fun PackageTool(title: String, onBack: () -> Unit) {
    val c = K.c
    Page(title, "قیمت‌ها را خودت وارد می‌کنی؛ هیچ قیمتی از پیش در برنامه نیست", onBack) {
        Text("قیمت و حجم را از برنامه یا کد دستوری اپراتور خودت بخوان و اینجا وارد کن تا بهترین گزینه مشخص شود.", color = c.muted, style = MaterialTheme.typography.bodyMedium)
        var p1 by remember { mutableStateOf("") }; var g1 by remember { mutableStateOf("") }; var d1 by remember { mutableStateOf("30") }
        var p2 by remember { mutableStateOf("") }; var g2 by remember { mutableStateOf("") }; var d2 by remember { mutableStateOf("30") }
        fun num(s: String) = Persian.latinDigits(s).replace(",", "").replace("،", "").toDoubleOrNull()
        @Composable fun block(name: String, p: String, sp: (String) -> Unit, g: String, sg: (String) -> Unit, d: String, sd: (String) -> Unit): Double? {
            val pv = num(p); val gv = num(g); val dv = num(d)
            val ok = pv != null && gv != null && dv != null && gv > 0 && dv > 0 && pv > 0
            KCard {
                Text(name, style = MaterialTheme.typography.titleMedium, color = c.text)
                KField(p, sp, "قیمت (تومان)", keyboard = KeyboardType.Number)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KField(g, sg, "حجم (گیگابایت)", Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                    KField(d, sd, "مدت (روز)", Modifier.weight(1f), keyboard = KeyboardType.Number)
                }
                if (ok) {
                    Text("هر گیگابایت: " + Calc.format(Math.round(pv!! / gv!!).toDouble()) + " تومان", color = c.primary, fontWeight = FontWeight.Bold)
                    Text("هر روز: " + Calc.format(Math.round(pv / dv!!).toDouble()) + " تومان", color = c.muted)
                }
            }
            return if (ok) pv!! / gv!! else null
        }
        val a = block("بستهٔ اول", p1, { p1 = it }, g1, { g1 = it }, d1, { d1 = it })
        val b = block("بستهٔ دوم", p2, { p2 = it }, g2, { g2 = it }, d2, { d2 = it })
        if (a != null && b != null) {
            KCard {
                val better = if (a < b) "بستهٔ اول" else if (b < a) "بستهٔ دوم" else null
                Text(if (better == null) "هزینهٔ هر گیگابایت دو بسته برابر است." else "$better برای هر گیگابایت ارزان‌تر است (${Calc.format(Math.round(Math.abs(a - b)).toDouble())} تومان کمتر).", color = c.ok, style = MaterialTheme.typography.titleMedium)
                Text("این مقایسه فقط بر پایهٔ اعداد واردشده است. مالیات، ترافیک شبانه یا نیم‌بها بودن ساعاتی از شبانه‌روز و محدودیت سرعت را هم بررسی کن.", color = c.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun TorchTool(state: AppState, title: String, onBack: () -> Unit) {
    val c = K.c
    val host = LocalHost.current
    Page(title, null, onBack) {
        KCard {
            Text(if (state.torchOn) "روشن" else "خاموش", fontSize = 40.sp, fontWeight = FontWeight.Black, color = if (state.torchOn) c.warn else c.muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        KButton(if (state.torchOn) "خاموش کردن" else "روشن کردن", {
            if (!host.setTorch(!state.torchOn)) state.say("این گوشی چراغ‌قوه ندارد یا دوربین در حال استفادهٔ برنامهٔ دیگری است.")
        }, Modifier.fillMaxWidth())
        Text("اگر برنامهٔ دیگری از دوربین استفاده کند، ممکن است چراغ خاموش شود.", color = c.muted, style = MaterialTheme.typography.bodySmall)
    }
}
