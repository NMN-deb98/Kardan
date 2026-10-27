package ir.techima.kardan

import java.util.Calendar
import java.util.TimeZone

sealed class AssistantAction {
    abstract val label: String
    data class Open(override val label: String, val route: AppRoute) : AssistantAction()
    data class OpenGuide(override val label: String, val id: String) : AssistantAction()
    data class OpenSettings(override val label: String, val target: Target) : AssistantAction()
    data class CreateTask(override val label: String, val title: String, val due: Long?, val remind: Boolean) : AssistantAction()
    data class StartTimer(override val label: String, val seconds: Int) : AssistantAction()
    data class Torch(override val label: String, val on: Boolean) : AssistantAction()
}

data class AssistantReply(
    val status: OpStatus,
    val message: String,
    val actions: List<AssistantAction> = emptyList(),
    val suggestions: List<String> = emptyList()
)

/**
 * Offline Persian intent router. It never pretends to do more than it does:
 * every reply carries an honest [OpStatus].
 */
object Assistant {
    val examples = listOf(
        "چرا گوشی من کند شده؟",
        "فردا ساعت ۹ یادآوری بساز: خرید نان",
        "ساعت استانبول را نشان بده",
        "تایمر ۱۰ دقیقه",
        "می‌خواهم بسته اینترنت بخرم",
        "چطور یک فایل را به PDF تبدیل کنم؟",
        "رمز قوی بساز",
        "چراغ‌قوه را روشن کن"
    )

    private val wordNumbers = mapOf(
        "یک" to 1, "دو" to 2, "سه" to 3, "چهار" to 4, "پنج" to 5, "شش" to 6, "هفت" to 7,
        "هشت" to 8, "نه" to 9, "ده" to 10, "یازده" to 11, "دوازده" to 12
    )

    private val weekdayWords = listOf(
        "یکشنبه" to Calendar.SUNDAY, "دوشنبه" to Calendar.MONDAY, "سهشنبه" to Calendar.TUESDAY,
        "چهارشنبه" to Calendar.WEDNESDAY, "پنجشنبه" to Calendar.THURSDAY, "جمعه" to Calendar.FRIDAY, "شنبه" to Calendar.SATURDAY
    )

    private fun norm(s: String): String =
        Persian.normalize(s).replace("بعد از ظهر", "بعدازظهر").replace("بعد ازظهر", "بعدازظهر")

    private fun has(text: String, vararg words: String): Boolean = words.any { text.contains(Persian.normalize(it)) }

    data class When(val millis: Long?, val rest: String, val note: String?)

    /** Extracts a date/time from Persian text. [rest] is the text with the time phrases removed. */
    fun parseWhen(input: String, now: Long, zone: TimeZone): When {
        var t = " " + norm(input) + " "
        var day = -1
        var hour = -1
        var minute = 0
        var relative: Long? = null
        var note: String? = null

        // relative: «۲۰ دقیقه دیگه» / «۱ ساعت بعد»
        Regex("(\\d{1,3}|" + wordNumbers.keys.joinToString("|") + ")\\s*(دقیقه|ساعت)\\s*(دیگه|دیگر|بعد)").find(t)?.let { m ->
            val n = m.groupValues[1].toIntOrNull() ?: wordNumbers[m.groupValues[1]] ?: 0
            relative = now + n * (if (m.groupValues[2] == "ساعت") 3_600_000L else 60_000L)
            t = t.replace(m.value, " ")
        }

        if (t.contains("پسفردا")) { day = 2; t = t.replace("پسفردا", " ") }
        else if (t.contains("فردا")) { day = 1; t = t.replace("فردا", " ") }
        else if (t.contains("امروز")) { day = 0; t = t.replace("امروز", " ") }
        else if (t.contains("امشب")) { day = 0; hour = 21; t = t.replace("امشب", " ") }
        var weekday = -1
        for ((w, d) in weekdayWords) {
            if (t.contains(w)) { weekday = d; t = t.replace(w, " "); break }
        }

        val hm = Regex("ساعت\\s*(\\d{1,2}|" + wordNumbers.keys.joinToString("|") + ")(?:\\s*[:٫.]\\s*(\\d{2}))?").find(t)
            ?: Regex("(\\d{1,2})\\s*:\\s*(\\d{2})").find(t)
        if (hm != null) {
            hour = hm.groupValues[1].toIntOrNull() ?: wordNumbers[hm.groupValues[1]] ?: -1
            minute = hm.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            t = t.replace(hm.value, " ")
            if (hour in 0..23 && minute in 0..59) {
                val pm = has(t, "عصر", "بعدازظهر", "شب", "ظهر")
                if (pm && hour in 1..11 && !(has(t, "ظهر") && !has(t, "بعدازظهر") && hour < 11)) hour += 12
                if (has(t, "ظهر") && hour == 0) hour = 12
            } else hour = -1
        } else if (has(t, "ظهر") && day >= 0) hour = 12
        listOf("صبح", "ظهر", "عصر", "بعدازظهر", "شب").forEach { t = t.replace(it, " ") }

        val cal = Calendar.getInstance(zone).apply { timeInMillis = now }
        var millis: Long? = relative
        if (millis == null && (day >= 0 || weekday >= 0 || hour >= 0)) {
            if (weekday >= 0) {
                var add = (weekday - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
                if (add == 0) add = 7
                cal.add(Calendar.DAY_OF_YEAR, add)
            } else if (day > 0) cal.add(Calendar.DAY_OF_YEAR, day)
            if (hour < 0) { hour = 9; minute = 0; note = "ساعت مشخص نبود؛ ۰۹:۰۰ در نظر گرفتم." }
            cal.set(Calendar.HOUR_OF_DAY, hour); cal.set(Calendar.MINUTE, minute); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            if (cal.timeInMillis <= now) {
                if (day < 0 && weekday < 0) { cal.add(Calendar.DAY_OF_YEAR, 1); note = "این ساعت امروز گذشته بود؛ برای فردا گذاشتم." }
                else { note = "این زمان گذشته است."; }
            }
            millis = cal.timeInMillis
        }
        return When(millis, t.replace(Regex("\\s+"), " ").trim(), note)
    }

    private val triggerWords = listOf(
        "یادآوری", "یادآور", "یاد آوری", "یادم بنداز", "یادم بیار", "یادم باشه", "یادم باشد", "یادمه بنداز",
        "ایجاد کن", "بساز", "بسازید", "تنظیم کن", "ثبت کن", "اضافه کن", "برام", "برایم", "برای من", "برای", "لطفا", "لطفاً",
        "به من", "که", "یک", "کار جدید", "تسک"
    )

    private fun cleanTitle(rest: String): String {
        var s = " " + rest.replace(Regex("[:：،,؛.!؟?]"), " ") + " "
        s = s.replace(Regex("\\s+"), " ")
        triggerWords.map { Persian.normalize(it) }.sortedByDescending { it.length }.forEach { s = s.replace(" $it ", " ") }
        return s.replace(Regex("\\s+"), " ").trim()
    }

    fun reply(input: String, now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): AssistantReply {
        val raw = input.trim()
        if (raw.isEmpty()) return AssistantReply(OpStatus.UNKNOWN, "چیزی نوشته نشده است. بنویس چه کاری می‌خواهی.", suggestions = examples.take(4))
        val t = norm(raw)

        // ---- greetings / help
        if (t == "سلام" || has(t, "کمک", "چه کارهایی") && t.length < 18) {
            return AssistantReply(OpStatus.DONE,
                "سلام! من می‌توانم یادآوری و تایمر بسازم، راهنمای مرحله‌به‌مرحله نشان بدهم، ساعت شهرها را بگویم و ابزارهای کاردان را باز کنم. هرچه می‌خواهی ساده بنویس.",
                suggestions = examples.take(5))
        }

        // ---- live information we do not have
        if (has(t, "قیمت دلار", "قیمت طلا", "قیمت سکه", "آب و هوا", "آبوهوا", "اخبار", "نتیجه بازی", "قیمت ارز")) {
            return AssistantReply(OpStatus.UNSUPPORTED,
                "این نسخهٔ کاردان به منبع داده‌ی زنده وصل نیست، پس عدد یا خبر حدسی نمی‌دهم. برای این اطلاعات به یک منبع معتبر و به‌روز مراجعه کن.")
        }

        // ---- timer
        if (has(t, "تایمر", "زمان سنج", "زمانسنج")) {
            val m = Regex("(\\d{1,3}|" + wordNumbers.keys.joinToString("|") + ")\\s*(ثانیه|دقیقه|ساعت)").find(t)
            if (m != null) {
                val n = m.groupValues[1].toIntOrNull() ?: wordNumbers[m.groupValues[1]] ?: 0
                val sec = n * when (m.groupValues[2]) { "ثانیه" -> 1; "دقیقه" -> 60; else -> 3600 }
                if (sec in 1..86_400) return AssistantReply(OpStatus.NEEDS_CONFIRM,
                    "تایمر ${Persian.digits(n)} ${m.groupValues[2]} را شروع کنم؟ در پایان با اعلان خبر می‌دهم.",
                    listOf(AssistantAction.StartTimer("شروع تایمر", sec)))
            }
            return AssistantReply(OpStatus.NEEDS_USER_ACTION, "مدت تایمر را بگو، مثلاً «تایمر ۱۰ دقیقه». یا صفحهٔ تایمر را باز کن.",
                listOf(AssistantAction.Open("باز کردن تایمر", AppRoute.TIMER)))
        }

        // ---- flashlight
        if (has(t, "چراغقوه", "چراغ قوه", "فلش")) {
            val off = has(t, "خاموش", "قطع")
            return AssistantReply(OpStatus.NEEDS_CONFIRM, if (off) "چراغ‌قوه را خاموش کنم؟" else "چراغ‌قوه را روشن کنم؟",
                listOf(AssistantAction.Torch(if (off) "خاموش‌کردن چراغ‌قوه" else "روشن‌کردن چراغ‌قوه", !off)))
        }

        // ---- reminder / task
        if (has(t, "یادآوری", "یادآور", "یاد آوری", "یادم بنداز", "یادم بیار", "یادم باشه", "یادم باشد", "تسک", "کار جدید")) {
            val w = parseWhen(raw, now, zone)
            val title = cleanTitle(w.rest)
            if (title.isBlank()) {
                return AssistantReply(OpStatus.UNKNOWN, "برای چه کاری یادآوری بسازم؟ مثلاً: «فردا ساعت ۹ یادآوری بساز: جلسه با علی».",
                    suggestions = listOf("فردا ساعت ۹ یادآوری بساز: خرید نان", "۳۰ دقیقه دیگه یادم بنداز آب بخورم"))
            }
            val whenText = w.millis?.let { Jalali.full(it) }
            val msg = if (w.millis != null)
                "یادآوری «$title» برای $whenText ساخته شود؟" + (w.note?.let { " ($it)" } ?: "")
            else "کار «$title» را بدون زمان ثبت کنم؟ (چون زمانی نگفتی، اعلان نخواهد داشت.)"
            return AssistantReply(OpStatus.NEEDS_CONFIRM, msg,
                listOf(AssistantAction.CreateTask(if (w.millis != null) "تأیید و ساخت یادآوری" else "ثبت کار", title, w.millis, w.millis != null)))
        }

        // ---- date today
        if (has(t, "تاریخ امروز", "امروز چندمه", "امروز چنده", "تاریخ شمسی", "امروز چه روزیه", "چه روزیه")) {
            return AssistantReply(OpStatus.DONE, "امروز " + Jalali.full(now).substringBefore("،") + " است.",
                listOf(AssistantAction.Open("تبدیل تاریخ شمسی/میلادی", AppRoute.DATECONV)))
        }
        if (has(t, "تبدیل تاریخ", "تاریخ میلادی")) {
            return AssistantReply(OpStatus.DONE, "مبدل تاریخ شمسی و میلادی آماده است.", listOf(AssistantAction.Open("باز کردن مبدل تاریخ", AppRoute.DATECONV)))
        }

        // ---- world clock (answered for real)
        if (has(t, "ساعت", "اختلاف ساعت", "وقت")) {
            val city = WorldClock.find(t)
            if (city != null && !has(t, "ساعت جهانی")) {
                val diff = WorldClock.diffToTehranMinutes(city.zone, now)
                return AssistantReply(OpStatus.DONE,
                    "ساعت ${city.name} (${city.country}) الان ${WorldClock.timeText(city.zone, now)} است؛ ${WorldClock.diffText(diff)}.",
                    listOf(AssistantAction.Open("ساعت جهانی", AppRoute.WORLD)))
            }
            if (has(t, "ساعت جهانی", "ساعت شهر", "اختلاف ساعت")) {
                return AssistantReply(OpStatus.DONE, "ساعت جهانی را باز می‌کنم؛ شهر را جست‌وجو کن.", listOf(AssistantAction.Open("ساعت جهانی", AppRoute.WORLD)))
            }
        }

        // ---- tools & modules
        if (has(t, "ماشین حساب", "ماشینحساب")) return AssistantReply(OpStatus.DONE, "ماشین‌حساب آفلاین آماده است.", listOf(AssistantAction.Open("باز کردن ماشین‌حساب", AppRoute.CALC)))
        if (has(t, "تبدیل واحد", "تبدیل کیلو", "تبدیل متر", "تبدیل دما")) return AssistantReply(OpStatus.DONE, "مبدل واحد آماده است.", listOf(AssistantAction.Open("باز کردن مبدل واحد", AppRoute.CONVERTER)))
        if (has(t, "کرنومتر", "کورنومتر")) return AssistantReply(OpStatus.DONE, "کرنومتر آماده است.", listOf(AssistantAction.Open("باز کردن کرنومتر", AppRoute.STOPWATCH)))
        if (has(t, "رمز قوی", "رمز بساز", "رمز تصادفی", "تولید رمز", "رمزساز")) return AssistantReply(OpStatus.DONE, "رمزساز آماده است؛ رمز در همین دستگاه ساخته می‌شود و جایی فرستاده نمی‌شود.", listOf(AssistantAction.Open("باز کردن رمزساز", AppRoute.PASSGEN)))
        if (has(t, "گاوصندوق", "ذخیره رمز", "مدیر رمز", "رمزهایم", "رمزهای من")) return AssistantReply(OpStatus.NEEDS_USER_ACTION, "گاوصندوق رمزها با قفل صفحهٔ خودِ گوشی باز می‌شود؛ باید هویتت را تأیید کنی.", listOf(AssistantAction.Open("باز کردن گاوصندوق", AppRoute.VAULT)))
        if (has(t, "اطلاعات گوشی", "مشخصات گوشی", "مشخصات دستگاه", "اطلاعات دستگاه", "نسخه اندروید", "مدل گوشی", "حافظه رم", "فضای خالی", "ظرفیت حافظه", "وضعیت باتری")) return AssistantReply(OpStatus.DONE, "اطلاعات دستگاه آماده است.", listOf(AssistantAction.Open("اطلاعات دستگاه", AppRoute.DEVICE)))
        if (has(t, "پشتیبان", "بکاپ", "backup")) return AssistantReply(OpStatus.DONE, "می‌توانی از کارها و فهرست‌های کاردان فایل پشتیبان بگیری یا بازیابی کنی.", listOf(AssistantAction.Open("پشتیبان‌گیری", AppRoute.BACKUP), AssistantAction.OpenGuide("راهنمای پشتیبان‌گیری", "backup-basics")))
        if (has(t, "فیلم", "سریال", "انیمیشن", "مستند")) return AssistantReply(OpStatus.UNSUPPORTED,
            "جست‌وجوی آنلاین فیلم و سریال هنوز در کاردان نیست و لینک تماشا هم نمی‌سازم. اما فهرست تماشای شخصی‌ات را می‌توانی بسازی و مرتب کنی.",
            listOf(AssistantAction.Open("فهرست تماشای من", AppRoute.MOVIES)))
        if (has(t, "زنگ بزن", "تماس بگیر", "پیامک بزن", "اس ام اس", "مخاطب")) return AssistantReply(OpStatus.NEEDS_CONFIRM,
            "برای امنیت، خودم تماس یا پیامک نمی‌فرستم. مخاطبان منتخب را بساز؛ با لمس هر کدام، شماره‌گیر یا پیام‌رسان گوشی با همان شماره باز می‌شود و تو تأیید می‌کنی.",
            listOf(AssistantAction.Open("مخاطبان منتخب", AppRoute.CONTACTS)))
        if (has(t, "کارهای من", "لیست کار", "برنامه امروز", "برنامه هفته", "عادت")) return AssistantReply(OpStatus.DONE, "برنامه‌ریزی و کارهایت آماده است.", listOf(AssistantAction.Open("برنامه‌ریزی", AppRoute.PLANNER)))
        if (has(t, "حالت سالمند", "سالمندان", "فونت بزرگ", "بزرگ کن", "تنظیمات کاردان", "حالت تاریک")) return AssistantReply(OpStatus.NEEDS_USER_ACTION, "این‌ها را از تنظیمات کاردان خودت تغییر می‌دهی.", listOf(AssistantAction.Open("تنظیمات کاردان", AppRoute.SETTINGS)))
        if (has(t, "بسته اینترنت", "بسته ی اینترنت", "حجم اینترنت", "قیمت بسته") && has(t, "مقایسه", "ارزان", "گران", "چقدر")) {
            return AssistantReply(OpStatus.DONE, "قیمت و حجم بسته‌ها را خودت وارد کن تا هزینهٔ هر گیگ را محاسبه کنم (قیمت زنده ندارم).",
                listOf(AssistantAction.Open("ماشین‌حساب بستهٔ اینترنت", AppRoute.PACKAGE), AssistantAction.OpenGuide("خرید امن بستهٔ اینترنت", "op-buy-safe")))
        }

        // ---- settings shortcuts (honest: we only open the page)
        val settings = listOf(
            Triple(listOf("وای فای", "wifi", "wi-fi"), Target.WIFI, "تنظیمات Wi-Fi"),
            Triple(listOf("بلوتوث", "bluetooth"), Target.BLUETOOTH, "تنظیمات بلوتوث"),
            Triple(listOf("هات اسپات", "نقطه اتصال", "اشتراک اینترنت"), Target.WIRELESS, "تنظیمات اتصال‌ها"),
            Triple(listOf("حالت هواپیما"), Target.AIRPLANE, "تنظیمات حالت هواپیما"),
            Triple(listOf("روشنایی", "اندازه فونت گوشی"), Target.DISPLAY, "تنظیمات نمایش"),
            Triple(listOf("صدای گوشی", "صدای زنگ", "ولوم"), Target.SOUND, "تنظیمات صدا"),
            Triple(listOf("صرفه جویی باتری"), Target.BATTERY_SAVER, "صرفه‌جویی باتری"),
            Triple(listOf("مکان یابی", "لوکیشن", "جی پی اس"), Target.LOCATION, "تنظیمات مکان")
        )
        if (has(t, "باز کن", "برو", "روشن", "خاموش", "تنظیمات")) {
            settings.firstOrNull { (k, _, _) -> k.any { has(t, it) } }?.let { (_, target, label) ->
                return AssistantReply(OpStatus.NEEDS_USER_ACTION,
                    "اندروید اجازه نمی‌دهد برنامه‌ها این تنظیم را خودکار تغییر دهند. صفحهٔ «$label» را برایت باز می‌کنم؛ تغییر را خودت انجام بده.",
                    listOf(AssistantAction.OpenSettings("باز کردن $label", target)))
            }
        }

        // ---- guide search
        val found = GuideData.search(raw, 4)
        if (found.isNotEmpty()) {
            val actions = found.map { AssistantAction.OpenGuide(it.title, it.id) }
            return AssistantReply(OpStatus.DONE,
                if (found.size == 1) "این راهنما به درخواستت می‌خورد:" else "این راهنماها به درخواستت نزدیک بود:", actions)
        }
        return AssistantReply(OpStatus.UNSUPPORTED,
            "این درخواست را نفهمیدم یا هنوز پشتیبانی نمی‌شود. می‌توانی ساده‌تر بنویسی یا از نمونه‌ها شروع کنی.",
            suggestions = examples.take(5))
    }
}
