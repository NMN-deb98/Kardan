package ir.techima.kardan

import java.time.Instant
import java.time.ZoneId

data class City(val name: String, val country: String, val zone: String, val aliases: String = "")

object WorldClock {
    val cities = listOf(
        City("تهران", "ایران", "Asia/Tehran"),
        City("مشهد", "ایران", "Asia/Tehran"),
        City("استانبول", "ترکیه", "Europe/Istanbul", "istanbul"),
        City("آنکارا", "ترکیه", "Europe/Istanbul"),
        City("دبی", "امارات", "Asia/Dubai", "dubai"),
        City("ابوظبی", "امارات", "Asia/Dubai"),
        City("دوحه", "قطر", "Asia/Qatar"),
        City("ریاض", "عربستان", "Asia/Riyadh"),
        City("مکه", "عربستان", "Asia/Riyadh"),
        City("بغداد", "عراق", "Asia/Baghdad"),
        City("کربلا", "عراق", "Asia/Baghdad"),
        City("کابل", "افغانستان", "Asia/Kabul"),
        City("باکو", "آذربایجان", "Asia/Baku"),
        City("ایروان", "ارمنستان", "Asia/Yerevan"),
        City("تفلیس", "گرجستان", "Asia/Tbilisi"),
        City("تاشکند", "ازبکستان", "Asia/Tashkent"),
        City("دوشنبه", "تاجیکستان", "Asia/Dushanbe"),
        City("اسلام‌آباد", "پاکستان", "Asia/Karachi", "کراچی"),
        City("دهلی", "هند", "Asia/Kolkata", "دهلی نو بمبئی"),
        City("پکن", "چین", "Asia/Shanghai", "شانگهای"),
        City("توکیو", "ژاپن", "Asia/Tokyo"),
        City("سئول", "کره جنوبی", "Asia/Seoul"),
        City("کوالالامپور", "مالزی", "Asia/Kuala_Lumpur"),
        City("سیدنی", "استرالیا", "Australia/Sydney"),
        City("قاهره", "مصر", "Africa/Cairo"),
        City("مسکو", "روسیه", "Europe/Moscow"),
        City("لندن", "بریتانیا", "Europe/London", "london"),
        City("پاریس", "فرانسه", "Europe/Paris"),
        City("برلین", "آلمان", "Europe/Berlin"),
        City("رم", "ایتالیا", "Europe/Rome"),
        City("مادرید", "اسپانیا", "Europe/Madrid"),
        City("آمستردام", "هلند", "Europe/Amsterdam"),
        City("استکهلم", "سوئد", "Europe/Stockholm"),
        City("نیویورک", "آمریکا", "America/New_York"),
        City("لس‌آنجلس", "آمریکا", "America/Los_Angeles", "لس آنجلس"),
        City("تورنتو", "کانادا", "America/Toronto"),
        City("ونکوور", "کانادا", "America/Vancouver")
    )

    fun find(text: String): City? {
        val t = Persian.normalize(text)
        return cities.firstOrNull { c ->
            t.contains(Persian.normalize(c.name)) || c.aliases.split(" ").any { it.isNotBlank() && t.contains(Persian.normalize(it)) }
        }
    }

    fun offsetSeconds(zone: String, nowMillis: Long): Int =
        ZoneId.of(zone).rules.getOffset(Instant.ofEpochMilli(nowMillis)).totalSeconds

    /** Difference to Tehran in minutes (positive = ahead of Tehran). */
    fun diffToTehranMinutes(zone: String, nowMillis: Long): Int =
        (offsetSeconds(zone, nowMillis) - offsetSeconds("Asia/Tehran", nowMillis)) / 60

    fun timeText(zone: String, nowMillis: Long): String {
        val zdt = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.of(zone))
        return Persian.digits(String.format(java.util.Locale.US, "%02d:%02d", zdt.hour, zdt.minute))
    }

    fun dateText(zone: String, nowMillis: Long): String {
        val zdt = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.of(zone))
        return Persian.digits(String.format(java.util.Locale.US, "%d/%02d/%02d", zdt.year, zdt.monthValue, zdt.dayOfMonth))
    }

    fun diffText(minutes: Int): String {
        if (minutes == 0) return "هم‌ساعت با تهران"
        val abs = Math.abs(minutes)
        val h = abs / 60
        val m = abs % 60
        val span = if (m == 0) "${Persian.digits(h)} ساعت" else if (h == 0) "${Persian.digits(m)} دقیقه" else "${Persian.digits(h)} ساعت و ${Persian.digits(m)} دقیقه"
        return if (minutes > 0) "$span جلوتر از تهران" else "$span عقب‌تر از تهران"
    }
}
