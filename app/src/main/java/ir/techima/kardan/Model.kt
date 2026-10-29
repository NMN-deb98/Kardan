package ir.techima.kardan

data class TaskStep(val text: String, val done: Boolean = false)

data class Task(
    val id: Long,
    val title: String,
    val notes: String = "",
    val due: Long? = null,
    /** 0 = کم، 1 = متوسط، 2 = زیاد */
    val priority: Int = 1,
    val category: String = "شخصی",
    /** 0 = بدون تکرار، 1 = روزانه، 2 = هفتگی، 3 = ماهانه */
    val repeat: Int = 0,
    val remind: Boolean = false,
    val done: Boolean = false,
    val steps: List<TaskStep> = emptyList(),
    val estimateMin: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Habit(val id: Long, val title: String, val days: Set<String> = emptySet())

data class WatchItem(
    val id: Long,
    val title: String,
    /** فیلم، سریال، انیمیشن، مستند */
    val type: String = "فیلم",
    /** می‌خواهم ببینم، در حال تماشا، دیده‌شده */
    val status: String = "می‌خواهم ببینم",
    val note: String = "",
    val rating: Int = 0,
    val originalTitle: String = "",
    /** نام‌های جایگزین یا املای دیگر، با ویرگول جدا شده */
    val altNames: String = "",
    val year: String = "",
    val genre: String = "",
    val summary: String = "",
    /** مدت یا تعداد قسمت‌ها، مثلاً «۱۲۰ دقیقه» یا «۱۰ قسمت» */
    val duration: String = "",
    /** لینک تماشای قانونی که خود کاربر وارد کرده است */
    val link: String = ""
)

data class QuickContact(val id: Long, val name: String, val number: String)

data class Settings(
    /** 0 = مطابق سیستم، 1 = روشن، 2 = تیره */
    val theme: Int = 0,
    val senior: Boolean = false,
    val fontScale: Float = 1f,
    val reduceMotion: Boolean = false,
    /** true = ارقام انگلیسی (۱۲۳ → 123) */
    val latinDigits: Boolean = false
)

object Categories {
    val task = listOf("شخصی", "کار", "خرید", "خانه", "مطالعه", "سلامت", "مالی")
    val repeatNames = listOf("بدون تکرار", "هر روز", "هر هفته", "هر ماه")
    val priorityNames = listOf("کم", "متوسط", "زیاد")
    val watchTypes = listOf("فیلم", "سریال", "انیمیشن", "مستند")
    val watchStatus = listOf("می‌خواهم ببینم", "در حال تماشا", "دیده‌شده")
}
