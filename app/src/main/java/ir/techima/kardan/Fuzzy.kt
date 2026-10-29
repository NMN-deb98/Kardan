package ir.techima.kardan

/** Small helpers for typo-tolerant matching of Persian and Latin titles. */
object Fuzzy {
    /** Levenshtein distance with early exit once it exceeds [max]. */
    fun distance(a: String, b: String, max: Int = 2): Int {
        if (a == b) return 0
        if (kotlin.math.abs(a.length - b.length) > max) return max + 1
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                if (cur[j] < rowMin) rowMin = cur[j]
            }
            if (rowMin > max) return max + 1
            prev = cur
        }
        return prev[b.length]
    }

    fun within(a: String, b: String, max: Int): Boolean = distance(a, b, max) <= max

    /**
     * True when [query] matches [text] after normalisation: substring, every word present,
     * or (for longer words) a word of [text] within one/two edits.
     */
    fun matches(text: String, query: String): Boolean {
        val q = Persian.normalize(query)
        if (q.isEmpty()) return true
        val t = Persian.normalize(text)
        if (t.contains(q)) return true
        val words = t.split(" ", "-", ":", "،").filter { it.isNotEmpty() }
        return q.split(" ").filter { it.isNotEmpty() }.all { qw ->
            words.any { w -> w.contains(qw) || (qw.length >= 4 && within(w.take(qw.length + 1), qw, if (qw.length >= 7) 2 else 1)) }
        }
    }
}
