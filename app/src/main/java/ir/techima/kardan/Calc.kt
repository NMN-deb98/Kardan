package ir.techima.kardan

import kotlin.math.abs
import kotlin.math.floor

/** Small safe expression evaluator: + - × ÷ ( ) % and decimals. Never throws; returns null for invalid input. */
object Calc {
    fun eval(raw: String): Double? {
        val s = Persian.latinDigits(raw)
            .replace('×', '*').replace('x', '*').replace('X', '*').replace('÷', '/').replace('−', '-').replace('–', '-')
            .replace(" ", "").replace("٬", "").replace(",", "")
        if (s.isEmpty()) return null
        return try {
            val p = Parser(s)
            val v = p.expr()
            if (p.i != s.length || v.isNaN() || v.isInfinite()) null else v
        } catch (e: Exception) { null }
    }

    private class Parser(val s: String) {
        var i = 0
        fun peek(): Char = if (i < s.length) s[i] else '\u0000'
        fun expr(): Double {
            var v = term()
            while (peek() == '+' || peek() == '-') { val op = s[i++]; val r = term(); v = if (op == '+') v + r else v - r }
            return v
        }
        fun term(): Double {
            var v = unary()
            while (peek() == '*' || peek() == '/') {
                val op = s[i++]; val r = unary()
                if (op == '/' && r == 0.0) throw ArithmeticException("div0")
                v = if (op == '*') v * r else v / r
            }
            return v
        }
        fun unary(): Double {
            if (peek() == '-') { i++; return -unary() }
            if (peek() == '+') { i++; return unary() }
            var v = primary()
            while (peek() == '%') { i++; v /= 100.0 }
            return v
        }
        fun primary(): Double {
            if (peek() == '(') {
                i++
                val v = expr()
                if (peek() != ')') throw IllegalStateException("paren")
                i++
                return v
            }
            val st = i
            while (peek().isDigit() || peek() == '.') i++
            if (st == i) throw IllegalStateException("num")
            return s.substring(st, i).toDouble()
        }
    }

    /** Formats with at most 10 significant decimals, Persian digits and «٫». */
    fun format(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "نامعتبر"
        val r = Math.round(v * 1e10) / 1e10
        val text = if (r == floor(r) && abs(r) < 1e15) String.format(java.util.Locale.US, "%.0f", r)
        else java.math.BigDecimal(r).setScale(10, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return Persian.digits(group(text)).replace('.', '٫')
    }

    private fun group(t: String): String {
        val neg = t.startsWith("-")
        val body = if (neg) t.substring(1) else t
        val parts = body.split(".")
        val ip = parts[0].reversed().chunked(3).joinToString("،").reversed()
        return (if (neg) "-" else "") + ip + if (parts.size > 1) "." + parts[1] else ""
    }
}

class UnitCat(val name: String, val units: List<Pair<String, Double>>, val temperature: Boolean = false)

object Converter {
    val categories = listOf(
        UnitCat("طول", listOf("متر" to 1.0, "کیلومتر" to 1000.0, "سانتی‌متر" to 0.01, "میلی‌متر" to 0.001, "مایل" to 1609.344, "یارد" to 0.9144, "فوت" to 0.3048, "اینچ" to 0.0254)),
        UnitCat("وزن", listOf("کیلوگرم" to 1.0, "گرم" to 0.001, "تن" to 1000.0, "پوند" to 0.45359237, "اونس" to 0.028349523125, "مثقال" to 0.0046083, "سیر" to 0.075)),
        UnitCat("مساحت", listOf("متر مربع" to 1.0, "هکتار" to 10000.0, "کیلومتر مربع" to 1e6, "فوت مربع" to 0.09290304, "اینچ مربع" to 0.00064516)),
        UnitCat("حجم", listOf("لیتر" to 1.0, "میلی‌لیتر" to 0.001, "متر مکعب" to 1000.0, "گالن آمریکا" to 3.785411784, "فنجان آمریکا" to 0.2365882365)),
        UnitCat("داده", listOf("بایت" to 1.0, "کیلوبایت" to 1024.0, "مگابایت" to 1048576.0, "گیگابایت" to 1073741824.0, "ترابایت" to 1099511627776.0)),
        UnitCat("سرعت", listOf("کیلومتر بر ساعت" to 1.0, "متر بر ثانیه" to 3.6, "مایل بر ساعت" to 1.609344, "گره دریایی" to 1.852)),
        UnitCat("زمان", listOf("ثانیه" to 1.0, "دقیقه" to 60.0, "ساعت" to 3600.0, "روز" to 86400.0, "هفته" to 604800.0)),
        UnitCat("دما", listOf("سانتی‌گراد" to 1.0, "فارنهایت" to 1.0, "کلوین" to 1.0), temperature = true)
    )

    fun convert(cat: UnitCat, from: Int, to: Int, value: Double): Double {
        if (cat.temperature) {
            val c = when (from) { 0 -> value; 1 -> (value - 32) * 5 / 9; else -> value - 273.15 }
            return when (to) { 0 -> c; 1 -> c * 9 / 5 + 32; else -> c + 273.15 }
        }
        return value * cat.units[from].second / cat.units[to].second
    }
}
