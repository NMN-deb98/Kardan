package ir.techima.kardan

/** Helpers for Persian text: digits, normalisation and searching. */
object Persian {
    private const val FA = "۰۱۲۳۴۵۶۷۸۹"
    private const val AR = "٠١٢٣٤٥٦٧٨٩"

    /** When true, [digits] leaves Latin digits untouched (user preference). */
    @Volatile var latin: Boolean = false

    /** Converts Latin digits in [value] to Persian digits (unless the user chose Latin digits). */
    fun digits(value: Any?): String {
        val s = value?.toString() ?: return ""
        if (latin) return s
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(if (c in '0'..'9') FA[c - '0'] else c)
        return sb.toString()
    }

    /** Converts Persian/Arabic-Indic digits to Latin digits. */
    fun latinDigits(value: String): String {
        val sb = StringBuilder(value.length)
        for (c in value) {
            val fa = FA.indexOf(c)
            val ar = AR.indexOf(c)
            sb.append(
                when {
                    fa >= 0 -> ('0' + fa)
                    ar >= 0 -> ('0' + ar)
                    c == '٫' -> '.'
                    else -> c
                }
            )
        }
        return sb.toString()
    }

    /** Normalises text for forgiving search: unify ی/ک, drop half-spaces and diacritics, lower-case. */
    fun normalize(value: String): String {
        val sb = StringBuilder(value.length)
        for (c in latinDigits(value)) {
            when (c) {
                'ي', 'ى' -> sb.append('ی')
                'ك' -> sb.append('ک')
                '‌', '‍', 'ـ', 'ً', 'ٌ', 'ٍ', 'َ', 'ُ', 'ِ', 'ّ', 'ْ' -> Unit
                'أ', 'إ', 'ٱ' -> sb.append('ا')
                'ة' -> sb.append('ه')
                else -> sb.append(c.lowercaseChar())
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    fun containsLoose(haystack: String, needle: String): Boolean {
        val n = normalize(needle)
        if (n.isEmpty()) return true
        return normalize(haystack).contains(n)
    }

    /** Formats a byte count with Persian digits, e.g. «۱٫۴ گیگابایت». */
    fun bytes(value: Long): String {
        val kb = 1024.0
        val v = value.toDouble()
        return when {
            v >= kb * kb * kb -> digits(String.format(java.util.Locale.US, "%.1f", v / (kb * kb * kb)).replace('.', '٫')) + " گیگابایت"
            v >= kb * kb -> digits(String.format(java.util.Locale.US, "%.0f", v / (kb * kb))) + " مگابایت"
            else -> digits(String.format(java.util.Locale.US, "%.0f", v / kb)) + " کیلوبایت"
        }
    }
}
