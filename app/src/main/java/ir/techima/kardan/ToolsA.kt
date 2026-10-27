package ir.techima.kardan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun CalcKey(label: String, modifier: Modifier, accent: Boolean = false, onClick: () -> Unit) {
    val c = K.c
    Box(
        modifier.height(60.dp).clip(RoundedCornerShape(18.dp))
            .background(if (accent) c.primary else c.card).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (accent) c.onPrimary else c.text)
    }
}

@Composable
fun CalcTool(title: String, onBack: () -> Unit) {
    val c = K.c
    var expr by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf<String>() }
    val live = if (expr.isEmpty()) null else Calc.eval(expr)

    fun press(k: String) {
        shown = null
        expr += k
    }
    fun equals() {
        val v = Calc.eval(expr)
        if (v == null) { shown = "عبارت نامعتبر است" } else {
            val r = Calc.format(v)
            history.add(0, Persian.digits(expr).replace('*', '×') + " = " + r)
            if (history.size > 6) history.removeAt(history.size - 1)
            expr = r.replace("،", "").replace('٫', '.').let { Persian.latinDigits(it) }
            shown = null
        }
    }

    Page(title, "با ارقام فارسی یا انگلیسی", onBack) {
        KCard {
            Text(
                if (expr.isEmpty()) "۰" else Persian.digits(expr).replace("*", "×").replace("/", "÷"),
                fontSize = 30.sp, color = c.text, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()
            )
            Text(
                shown ?: (if (live != null && expr.any { it in "+-*/%()" }) "= " + Calc.format(live) else " "),
                color = if (shown != null) c.danger else c.muted, style = MaterialTheme.typography.titleMedium
            )
        }
        val rows = listOf(
            listOf("C", "⌫", "%", "/"), listOf("7", "8", "9", "*"), listOf("4", "5", "6", "-"), listOf("1", "2", "3", "+"), listOf("(", "0", ".", ")")
        )
        rows.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                r.forEach { k ->
                    val face = when (k) { "/" -> "÷"; "*" -> "×"; "-" -> "−"; "." -> "٫"; else -> if (k[0].isDigit()) Persian.digits(k) else k }
                    CalcKey(face, Modifier.weight(1f), accent = k in listOf("/", "*", "-", "+")) {
                        when (k) {
                            "C" -> { expr = ""; shown = null }
                            "⌫" -> { expr = expr.dropLast(1); shown = null }
                            else -> press(k)
                        }
                    }
                }
            }
        }
        KButton("=  محاسبه", { equals() }, Modifier.fillMaxWidth())
        if (history.isNotEmpty()) {
            SectionTitle("تاریخچه")
            KCard { history.forEach { Text(it, color = c.muted, style = MaterialTheme.typography.bodyMedium) } }
        }
    }
}

@Composable
fun ConverterTool(title: String, onBack: () -> Unit) {
    val c = K.c
    var ci by remember { mutableStateOf(0) }
    var from by remember { mutableStateOf(0) }
    var to by remember { mutableStateOf(1) }
    var input by remember { mutableStateOf("1") }
    val cat = Converter.categories[ci]
    val v = Calc.eval(input)
    Page(title, "تبدیل دقیق و آفلاین", onBack) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Converter.categories.forEachIndexed { i, k -> Chip(k.name, i == ci) { ci = i; from = 0; to = 1.coerceAtMost(k.units.size - 1) } }
        }
        KField(input, { input = it }, "مقدار", keyboard = KeyboardType.Decimal)
        SectionTitle("از")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cat.units.forEachIndexed { i, u -> Chip(u.first, i == from) { from = i } }
        }
        SectionTitle("به")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cat.units.forEachIndexed { i, u -> Chip(u.first, i == to) { to = i } }
        }
        KCard {
            if (v == null) Text("مقدار معتبر وارد کن.", color = c.muted) else {
                Text(Calc.format(v) + " " + cat.units[from].first, color = c.muted, style = MaterialTheme.typography.bodyMedium)
                Text(Calc.format(Converter.convert(cat, from, to, v)) + " " + cat.units[to].first, color = c.primary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (cat.name == "وزن") Text("مثقال ≈ ۴٫۶۰۸ گرم و سیر = ۷۵ گرم (معیار رایج بازار طلا).", color = c.muted, style = MaterialTheme.typography.bodySmall)
    }
}
