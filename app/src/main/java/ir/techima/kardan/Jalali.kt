package ir.techima.kardan

import java.util.Calendar
import java.util.TimeZone

/** Solar Hijri (Jalali) calendar conversion. Algorithm after jalaali-js, validated against known dates. */
object Jalali {
    data class Date(val year: Int, val month: Int, val day: Int)

    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )
    val weekDays = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    private val breaks = intArrayOf(-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178)

    private class Cal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): Cal {
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + ((jump % 33) / 4)
            jp = jm
        }
        var n = jy - jp
        leapJ += (n / 33) * 8 + (((n % 33) + 3) / 4)
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val leapG = (gy / 4) - (((gy / 100) + 1) * 3 / 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return Cal(leap, gy, march)
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = ((gy + (gm - 8) / 6 + 100100) * 1461) / 4 + (153 * ((gm + 9) % 12) + 2) / 5 + gd - 34840408
        d = d - (((gy + 100100 + (gm - 8) / 6) / 100) * 3) / 4 + 752
        return d
    }

    private fun d2g(jdn: Int): IntArray {
        var j = 4 * jdn + 139361631
        j += (((4 * jdn + 183187720) / 146097) * 3 / 4) * 4 - 3908
        val i = ((j % 1461) / 4) * 5 + 308
        val gd = (i % 153) / 5 + 1
        val gm = (i / 153) % 12 + 1
        val gy = j / 1461 - 100100 + (8 - gm) / 6
        return intArrayOf(gy, gm, gd)
    }

    private fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy)
        return g2d(r.gy, 3, r.march) + (jm - 1) * 31 - (jm / 7) * (jm - 7) + jd - 1
    }

    fun fromGregorian(gy: Int, gm: Int, gd: Int): Date {
        val jdn = g2d(gy, gm, gd)
        val gyy = d2g(jdn)[0]
        var jy = gyy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(gyy, 3, r.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) return Date(jy, 1 + k / 31, k % 31 + 1)
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        return Date(jy, 7 + k / 30, k % 30 + 1)
    }

    /** Returns Gregorian (year, month, day). */
    fun toGregorian(jy: Int, jm: Int, jd: Int): IntArray = d2g(j2d(jy, jm, jd))

    fun isLeap(jy: Int): Boolean = jalCal(jy).leap == 0

    fun daysInMonth(jy: Int, jm: Int): Int = when {
        jm in 1..6 -> 31
        jm in 7..11 -> 30
        else -> if (isLeap(jy)) 30 else 29
    }

    fun fromMillis(millis: Long, zone: TimeZone = TimeZone.getDefault()): Date {
        val c = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /** Index into [weekDays]: 0 = شنبه ... 6 = جمعه. */
    fun weekDayIndex(millis: Long, zone: TimeZone = TimeZone.getDefault()): Int {
        val c = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return (c.get(Calendar.DAY_OF_WEEK) + 7 - Calendar.SATURDAY) % 7
    }

    fun startOfDay(millis: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val c = Calendar.getInstance(zone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    fun millisOf(date: Date, hour: Int, minute: Int, zone: TimeZone = TimeZone.getDefault()): Long {
        val g = toGregorian(date.year, date.month, date.day)
        val c = Calendar.getInstance(zone).apply {
            clear()
            set(g[0], g[1] - 1, g[2], hour, minute, 0)
        }
        return c.timeInMillis
    }

    fun format(d: Date, withYear: Boolean = true): String =
        Persian.digits(d.day) + " " + monthNames[d.month - 1] + if (withYear) " " + Persian.digits(d.year) else ""

    fun format(millis: Long, withYear: Boolean = true): String = format(fromMillis(millis), withYear)

    fun time(millis: Long, zone: TimeZone = TimeZone.getDefault()): String {
        val c = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return Persian.digits(String.format(java.util.Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)))
    }

    fun full(millis: Long): String =
        weekDays[weekDayIndex(millis)] + " " + format(millis) + "، " + time(millis)
}
