package ir.techima.kardan

/**
 * Real operator packages shown inside the app (spec 4.5). Every row says where it came from and when it was checked.
 * Only data that could be read from an official operator page is bundled; everything else the user saves themselves.
 */
data class OpPackage(
    val operator: String,
    val name: String,
    /** total volume in GB; null = unlimited (time-based) */
    val gb: Double?,
    val days: Int,
    val priceToman: Long,
    val note: String = ""
)

data class UserPackage(val id: Long, val operator: String, val name: String, val gb: Double, val days: Int, val priceToman: Long, val addedAt: Long)

object PackageCatalog {
    const val RIGHTEL = "رایتل"
    const val MCI = "همراه اول"
    const val IRANCELL = "ایرانسل"
    val operators = listOf(RIGHTEL, MCI, IRANCELL)

    /** Date the bundled rows were read from the official page (Jalali, shown to the user). */
    const val RIGHTEL_CHECKED = "۱۱ آبان ۱۴۰۵"
    const val RIGHTEL_SOURCE = "صفحهٔ رسمی فروش بستهٔ رایتل (package.rightel.ir)"
    const val RIGHTEL_URL = "https://package.rightel.ir/ExtraPackageSales/ProductListToSales/ShowProduct"

    fun officialLinks(op: String): List<GuideLink> = when (op) {
        RIGHTEL -> listOf(GuideLink("خرید و دیدن بسته‌ها (رسمی رایتل)", RIGHTEL_URL), GuideLink("وب‌سایت رسمی رایتل", "https://rightel.ir"))
        MCI -> listOf(GuideLink("بسته‌های اینترنت (رسمی همراه اول)", "https://mci.ir/internet-plans"), GuideLink("پنل «همراه من»", "https://my.mci.ir"))
        else -> listOf(GuideLink("تعرفهٔ اینترنت (رسمی ایرانسل)", "https://irancell.ir/p/326685/mobile-internet-tariff"), GuideLink("پنل «ایرانسل من»", "https://my.irancell.ir"))
    }

    private const val LOW = "بخشی از حجم فقط در ساعات کم‌ترافیک (بامداد) مصرف می‌شود"

    // prices on the official page are in rial; stored here in toman (÷10)
    val rightel: List<OpPackage> = listOf(
        OpPackage(RIGHTEL, "بستهٔ ۱ روزه", 0.04, 1, 1_000),
        OpPackage(RIGHTEL, "بستهٔ ۱ روزه", 0.2, 1, 2_000, "۰٫۱ گیگ عادی + ۰٫۱ گیگ ساعات کم‌ترافیک"),
        OpPackage(RIGHTEL, "بستهٔ ۱ روزه", 0.15, 1, 2_300),
        OpPackage(RIGHTEL, "بستهٔ ۱ روزه", 0.5, 1, 3_000, "۰٫۲ گیگ عادی + ۰٫۳ گیگ ساعات کم‌ترافیک"),
        OpPackage(RIGHTEL, "بستهٔ ۱ روزه", 0.25, 1, 3_500),
        OpPackage(RIGHTEL, "بستهٔ ۱ روزه", 1.0, 1, 3_900, "۰٫۳ گیگ عادی + ۰٫۷ گیگ ساعات کم‌ترافیک"),
        OpPackage(RIGHTEL, "بستهٔ ۱ روزه", 1.2, 1, 6_300),
        OpPackage(RIGHTEL, "بستهٔ ۱ روزه", 3.0, 1, 9_000),
        OpPackage(RIGHTEL, "بستهٔ ۱ روزه، فقط ساعات کم‌ترافیک", 1.0, 1, 500, "فقط در ساعات کم‌ترافیک قابل مصرف است"),
        OpPackage(RIGHTEL, "بستهٔ ۳ روزه", 0.5, 3, 4_000, "فقط سیم‌کارت دائمی"),
        OpPackage(RIGHTEL, "بستهٔ ۳ روزه", 0.5, 3, 5_700, "ویژهٔ برنامهٔ «رایتل من» (در منبع نوع سیم‌کارت دقیق ننوشته)"),
        OpPackage(RIGHTEL, "بستهٔ ۳ روزه", 0.9, 3, 5_000, "۰٫۳ گیگ عادی + ۰٫۶ گیگ ساعات کم‌ترافیک"),
        OpPackage(RIGHTEL, "بستهٔ ۳ روزه", 0.35, 3, 5_200),
        OpPackage(RIGHTEL, "بستهٔ ۳ روزه", 1.2, 3, 6_000, "۰٫۴ گیگ عادی + ۰٫۸ گیگ ساعات کم‌ترافیک"),
        OpPackage(RIGHTEL, "بستهٔ ۳ روزه", 1.5, 3, 7_200),
        OpPackage(RIGHTEL, "بستهٔ ۳ روزه", 3.0, 3, 11_000),
        OpPackage(RIGHTEL, "بستهٔ ۷ روزه", 0.1, 7, 2_200),
        OpPackage(RIGHTEL, "بستهٔ ۷ روزه", 0.3, 7, 3_300, "۰٫۱۵ گیگ عادی + ۰٫۱۵ گیگ ساعات کم‌ترافیک"),
        OpPackage(RIGHTEL, "بستهٔ ۷ روزه", 0.2, 7, 4_000),
        OpPackage(RIGHTEL, "بستهٔ ۷ روزه", 0.3, 7, 4_900)
    )

    /** Time-based unlimited offers: no volume, so they cannot be compared per GB. */
    val rightelUnlimited: List<OpPackage> = listOf(
        OpPackage(RIGHTEL, "نیم‌ساعت اینترنت نامحدود", null, 0, 500, "فقط ۲ تا ۷ بامداد"),
        OpPackage(RIGHTEL, "نیم‌ساعت اینترنت نامحدود", null, 0, 2_500),
        OpPackage(RIGHTEL, "۴ ساعت اینترنت نامحدود", null, 0, 2_700, "فقط ۲ تا ۷ بامداد"),
        OpPackage(RIGHTEL, "۳ ساعت اینترنت نامحدود", null, 0, 6_000),
        OpPackage(RIGHTEL, "۱ روزه نامحدود", null, 1, 2_000, "فقط ساعت ۶ صبح تا ۱۲ ظهر")
    )

    fun perGb(price: Long, gb: Double): Long? = if (gb > 0 && price > 0) Math.round(price / gb) else null
    fun perDay(price: Long, days: Int): Long? = if (days > 0 && price > 0) Math.round(price.toDouble() / days) else null

    /** Cheapest per GB first. */
    fun sortedByValue(list: List<OpPackage>): List<OpPackage> =
        list.filter { it.gb != null && it.gb > 0 }.sortedBy { it.priceToman / it.gb!! }

    fun gbText(gb: Double): String = Persian.digits((if (gb % 1.0 == 0.0) gb.toInt().toString() else gb.toString()).replace('.', '٫'))
}
