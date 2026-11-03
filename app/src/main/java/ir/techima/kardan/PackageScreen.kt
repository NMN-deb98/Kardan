package ir.techima.kardan

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

private fun num(s: String): Double? = Persian.latinDigits(s).replace(Regex("[,،٬\\s]"), "").toDoubleOrNull()

private fun plain(gb: Double): String = if (gb % 1.0 == 0.0) gb.toInt().toString() else gb.toString()

private fun jDate(millis: Long): String {
    val d = Jalali.fromMillis(millis)
    return Persian.digits(d.day) + " " + Jalali.monthNames[d.month - 1] + " " + Persian.digits(d.year)
}

@Composable
fun PackageTool(state: AppState, title: String, onBack: () -> Unit) {
    val c = K.c
    val host = LocalHost.current
    var op by remember { mutableStateOf(PackageCatalog.RIGHTEL) }
    var sortByValue by remember { mutableStateOf(true) }
    var dayFilter by remember { mutableStateOf<Int?>(null) }
    var p1 by remember { mutableStateOf("") }; var g1 by remember { mutableStateOf("") }; var d1 by remember { mutableStateOf("30") }
    var p2 by remember { mutableStateOf("") }; var g2 by remember { mutableStateOf("") }; var d2 by remember { mutableStateOf("30") }
    var nextSlot by remember { mutableStateOf(1) }

    fun fill(name: String, gb: Double, days: Int, price: Long) {
        if (nextSlot == 1) { p1 = price.toString(); g1 = plain(gb); d1 = days.toString() } else { p2 = price.toString(); g2 = plain(gb); d2 = days.toString() }
        state.say("«$name» در «بستهٔ ${if (nextSlot == 1) "اول" else "دوم"}» گذاشته شد.")
        nextSlot = if (nextSlot == 1) 2 else 1
    }

    Page(title, "بستهٔ واقعی اپراتورها + مقایسهٔ هزینهٔ هر گیگ و هر روز", onBack) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PackageCatalog.operators.forEach { o -> Chip(o, op == o) { op = o } }
        }

        // ------- source / honesty card
        KCard {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Info, null, tint = c.accent)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (op == PackageCatalog.RIGHTEL) {
                        Text("منبع: ${PackageCatalog.RIGHTEL_SOURCE}", style = MaterialTheme.typography.bodyMedium, color = c.text)
                        Text("آخرین بررسی: ${PackageCatalog.RIGHTEL_CHECKED}. قیمت‌ها تومان و طبق همان صفحه است؛ نوع سیم‌کارت، مالیات و شرایط را پیش از خرید در خود برنامه یا سایت رسمی بسنج. قیمت‌ها زود عوض می‌شوند.", style = MaterialTheme.typography.bodySmall, color = c.muted)
                    } else {
                        Text("فهرست رسمی $op در کاردان نیست", style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text("سایت رسمی $op خواندن خودکار فهرست را اجازه نمی‌دهد و کاردان قیمت حدسی یا از منبع غیررسمی نمی‌نویسد. بسته‌ای را که در برنامهٔ رسمی $op دیدی همین‌جا ذخیره کن؛ با تاریخ ثبت در دستگاه می‌ماند و برای مقایسه آماده است.", style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PackageCatalog.officialLinks(op).forEach { l -> KOutlineButton(l.label, { host.openUrl(l.url) }, icon = Icons.Filled.OpenInNew) }
            }
        }

        // ------- bundled official rows (Rightel)
        if (op == PackageCatalog.RIGHTEL) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("ارزان‌ترین هر گیگ", sortByValue) { sortByValue = true }
                Chip("به ترتیب منبع", !sortByValue) { sortByValue = false }
                Chip("همهٔ مدت‌ها", dayFilter == null) { dayFilter = null }
                listOf(1, 3, 7).forEach { d -> Chip(Persian.digits(d) + " روزه", dayFilter == d) { dayFilter = if (dayFilter == d) null else d } }
            }
            val base = PackageCatalog.rightel.filter { dayFilter == null || it.days == dayFilter }
            val rows = if (sortByValue) PackageCatalog.sortedByValue(base) else base
            rows.forEach { pk ->
                KCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${PackageCatalog.gbText(pk.gb!!)} گیگابایت · ${Persian.digits(pk.days)} روز", style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(Calc.format(pk.priceToman.toDouble()) + " تومان", color = c.primary, fontWeight = FontWeight.Bold)
                            PackageCatalog.perGb(pk.priceToman, pk.gb)?.let { Text("هر گیگابایت: " + Calc.format(it.toDouble()) + " تومان", style = MaterialTheme.typography.bodySmall, color = c.muted) }
                            if (pk.note.isNotBlank()) Text(pk.note, style = MaterialTheme.typography.bodySmall, color = c.warn)
                        }
                        KOutlineButton("مقایسه", { fill(pk.name, pk.gb, pk.days, pk.priceToman) })
                    }
                }
            }
            SectionTitle("نامحدود زمان‌دار (قابل مقایسهٔ هر گیگ نیست)")
            PackageCatalog.rightelUnlimited.forEach { pk ->
                KCard {
                    Text(pk.name + if (pk.days > 0) " · ${Persian.digits(pk.days)} روز" else "", style = MaterialTheme.typography.titleMedium, color = c.text)
                    Text(Calc.format(pk.priceToman.toDouble()) + " تومان" + if (pk.note.isNotBlank()) " · ${pk.note}" else "", color = c.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ------- packages the user saved from the official app
        val mine = state.myPackages.filter { it.operator == op }
        SectionTitle("بسته‌هایی که خودت ذخیره کرده‌ای (${op})")
        if (mine.isEmpty() && op != PackageCatalog.RIGHTEL) Text("هنوز بسته‌ای ذخیره نکرده‌ای.", color = c.muted, style = MaterialTheme.typography.bodySmall)
        mine.forEach { u ->
            KCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(u.name.ifBlank { "بسته" } + " · ${PackageCatalog.gbText(u.gb)} گیگابایت · ${Persian.digits(u.days)} روز", style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(Calc.format(u.priceToman.toDouble()) + " تومان", color = c.primary, fontWeight = FontWeight.Bold)
                        Text("ثبت‌شده توسط خودت در " + jDate(u.addedAt) + " (از برنامهٔ رسمی)", style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                    KOutlineButton("مقایسه", { fill(u.name, u.gb, u.days, u.priceToman) })
                    IconButton(onClick = { state.myPackages.removeAll { it.id == u.id }; state.saveMyPackages() }) { Icon(Icons.Filled.Delete, "حذف", tint = c.muted) }
                }
            }
        }
        var n by remember(op) { mutableStateOf("") }
        var pr by remember(op) { mutableStateOf("") }
        var gv by remember(op) { mutableStateOf("") }
        var dv by remember(op) { mutableStateOf("30") }
        KCard {
            Text("ذخیرهٔ یک بستهٔ $op", style = MaterialTheme.typography.titleMedium, color = c.text)
            KField(n, { n = it }, "نام بسته (اختیاری)")
            KField(pr, { pr = it }, "قیمت (تومان)", keyboard = KeyboardType.Number)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KField(gv, { gv = it }, "حجم (گیگابایت)", Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                KField(dv, { dv = it }, "مدت (روز)", Modifier.weight(1f), keyboard = KeyboardType.Number)
            }
            KButton("ذخیره", {
                val price = num(pr); val g = num(gv); val d = num(dv)
                if (price == null || g == null || d == null || price <= 0 || g <= 0 || d < 1) state.say("قیمت، حجم و مدت را درست وارد کن.")
                else {
                    state.myPackages.add(0, UserPackage(state.newId(), op, n.trim(), g, d.toInt(), price.toLong(), System.currentTimeMillis()))
                    state.saveMyPackages(); n = ""; pr = ""; gv = ""
                }
            }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add)
        }

        // ------- comparison
        SectionTitle("مقایسهٔ دو بسته")
        Text("از فهرست بالا «مقایسه» را بزن یا عددها را خودت بنویس.", color = c.muted, style = MaterialTheme.typography.bodySmall)
        @Composable fun block(name: String, p: String, sp: (String) -> Unit, g: String, sg: (String) -> Unit, d: String, sd: (String) -> Unit): Double? {
            val pv = num(p); val gv2 = num(g); val dv2 = num(d)
            val ok = pv != null && gv2 != null && dv2 != null && gv2 > 0 && dv2 > 0 && pv > 0
            KCard {
                Text(name, style = MaterialTheme.typography.titleMedium, color = c.text)
                KField(p, sp, "قیمت (تومان)", keyboard = KeyboardType.Number)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KField(g, sg, "حجم (گیگابایت)", Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                    KField(d, sd, "مدت (روز)", Modifier.weight(1f), keyboard = KeyboardType.Number)
                }
                if (ok) {
                    Text("هر گیگابایت: " + Calc.format(Math.round(pv!! / gv2!!).toDouble()) + " تومان", color = c.primary, fontWeight = FontWeight.Bold)
                    Text("هر روز: " + Calc.format(Math.round(pv / dv2!!).toDouble()) + " تومان", color = c.muted)
                }
            }
            return if (ok) pv!! / gv2!! else null
        }
        val a = block("بستهٔ اول", p1, { p1 = it }, g1, { g1 = it }, d1, { d1 = it })
        val b = block("بستهٔ دوم", p2, { p2 = it }, g2, { g2 = it }, d2, { d2 = it })
        if (a != null && b != null) {
            KCard {
                val better = if (a < b) "بستهٔ اول" else if (b < a) "بستهٔ دوم" else null
                Text(if (better == null) "هزینهٔ هر گیگابایت دو بسته برابر است." else "$better برای هر گیگابایت ارزان‌تر است (${Calc.format(Math.round(Math.abs(a - b)).toDouble())} تومان کمتر).", color = c.ok, style = MaterialTheme.typography.titleMedium)
                Text("این مقایسه فقط بر پایهٔ اعداد است. مالیات، ساعات کم‌ترافیک، محدودیت سرعت و نوع سیم‌کارت را هم بررسی کن. کاردان خرید نمی‌کند؛ خرید را از برنامه یا سایت رسمی اپراتور انجام بده.", color = c.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
