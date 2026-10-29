package ir.techima.kardan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideDataTest {
    @Test fun idsAreUnique() { assertEquals(GuideData.all.size, GuideData.all.map { it.id }.toSet().size) }
    @Test fun everyGuideIsComplete() {
        GuideData.all.forEach { g ->
            assertTrue(g.id, g.steps.size >= 3)
            assertTrue(g.id, g.title.isNotBlank() && g.summary.isNotBlank())
            assertTrue(g.id + " category", GuideData.categories.contains(g.category))
        }
    }
    @Test fun operatorsAndMessengersCovered() {
        listOf("op-mci", "op-irancell", "op-rightel", "msg-bale", "msg-eitaa", "msg-rubika", "msg-splus", "msg-telegram", "msg-whatsapp")
            .forEach { assertNotNull(it, GuideData.byId(it)) }
    }
    @Test fun searchFindsMessengers() {
        assertTrue(GuideData.search("روبیکا").any { it.id == "msg-rubika" })
        assertTrue(GuideData.search("ایرانسل").any { it.id == "op-irancell" })
    }
    @Test fun latinDigitsPreference() {
        Persian.latin = true
        assertEquals("12", Persian.digits(12))
        Persian.latin = false
        assertEquals("۱۲", Persian.digits(12))
    }
}

class GuideCenterTest {
    private val messengers = listOf("bale", "eitaa", "rubika", "splus", "telegram", "whatsapp")
    private val topics = listOf("send", "group", "notify", "privacy", "block", "recover", "scam")

    @Test fun everyMessengerHasEveryTopic() {
        messengers.forEach { m -> topics.forEach { t -> assertNotNull("msg-$m-$t", GuideData.byId("msg-$m-$t")) } }
    }
    @Test fun everyOperatorHasPackagesAndRenew() {
        listOf("mci", "irancell", "rightel").forEach { o ->
            assertNotNull(GuideData.byId("op-$o-packages")); assertNotNull(GuideData.byId("op-$o-renew"))
        }
        assertNotNull(GuideData.byId("op-paid-not-active"))
    }
    @Test fun shoppingTopicsCovered() {
        listOf("shop-place-order", "shop-seller", "shop-cancel", "shop-foreign", "shop-order-track", "shop-return", "shop-fake-links").forEach { assertNotNull(it, GuideData.byId(it)) }
    }
    @Test fun appDependentGuidesAreVolatileAndTagged() {
        GuideData.all.filter { it.category == GuideData.categories[2] }.forEach { assertTrue(it.id, it.volatile) }
        GuideData.all.filter { it.id.startsWith("msg-") }.forEach { assertNotNull(it.id, it.group) }
    }
    @Test fun noGuideAsksForCardData() {
        val bad = listOf("شماره کارت خود را بنویسید", "رمز دوم را بفرستید", "cvv2 را وارد")
        GuideData.all.forEach { g -> bad.forEach { b -> assertTrue(g.id, !g.steps.joinToString(" ").contains(b)) } }
    }
    @Test fun staleLogic() {
        val g = GuideData.byId("msg-telegram-send")!!
        val now = java.util.Calendar.getInstance().apply { set(2026, 10, 15) }.timeInMillis   // Nov 2026, 1 month after review
        assertTrue(!GuideData.isStale(g, now))
        val later = java.util.Calendar.getInstance().apply { set(2027, 2, 15) }.timeInMillis  // Mar 2027, 5 months
        assertTrue(GuideData.isStale(g, later))
        val calm = GuideData.byId("slow-phone")!!
        assertTrue(!GuideData.isStale(calm, later))
    }
    @Test fun searchByMessengerAndTypo() {
        assertTrue(GuideData.search("تلگرام مسدود").any { it.id == "msg-telegram-block" })
        assertTrue(GuideData.search("واتساپ").isNotEmpty())
        assertTrue(GuideData.search("تلگرم").any { it.id.startsWith("msg-telegram") })
    }
    @Test fun groupsListed() {
        assertTrue(GuideData.groupsOf(GuideData.categories[2]).containsAll(listOf("بله", "ایتا", "روبیکا", "سروش پلاس", "تلگرام", "واتس‌اپ")))
        assertTrue(GuideData.groupsOf(GuideData.categories[4]).containsAll(listOf("همراه اول", "ایرانسل", "رایتل")))
    }
}

class MovieSearchTest {
    private val items = listOf(
        WatchItem(1, "جدایی نادر از سیمین", "فیلم", originalTitle = "A Separation", year = "2011", genre = "درام"),
        WatchItem(2, "بریکینگ بد", "سریال", originalTitle = "Breaking Bad", altNames = "برکینگ بد, Breaking-Bad", genre = "جنایی"),
        WatchItem(3, "شاهزاده کوچولو", "انیمیشن", originalTitle = "The Little Prince")
    )
    @Test fun persianAndEnglishAndAlternate() {
        assertEquals(listOf(2L), MovieSearch.filter(items, "breaking", null, null).map { it.id })
        assertEquals(listOf(2L), MovieSearch.filter(items, "برکینگ", null, null).map { it.id })
        assertEquals(listOf(1L), MovieSearch.filter(items, "جدایی", null, null).map { it.id })
    }
    @Test fun typosAreTolerated() {
        assertEquals(listOf(2L), MovieSearch.filter(items, "breakin bad", null, null).map { it.id })
        assertEquals(listOf(3L), MovieSearch.filter(items, "little prinse", null, null).map { it.id })
    }
    @Test fun noMatchGivesEmpty() { assertTrue(MovieSearch.filter(items, "ژورژیا", null, null).isEmpty()) }
    @Test fun filtersByType() { assertEquals(1, MovieSearch.filter(items, "", "سریال", null).size) }
    @Test fun onlyHttpsLinks() {
        assertTrue(MovieSearch.validLink("https://example.com/title/123"))
        assertTrue(!MovieSearch.validLink("http://example.com/x"))
        assertTrue(!MovieSearch.validLink("javascript:alert(1)"))
        assertTrue(!MovieSearch.validLink("https://exa mple.com"))
    }
}

class VisualGuideTest {
    @Test fun extractsPath() {
        val p = VisualGuide.path("ابتدا ببینید چه چیزی بیشترین جا را گرفته: تنظیمات ← حافظه (Storage).")!!
        assertEquals(listOf("تنظیمات", "حافظه"), p)
    }
    @Test fun noPathWithoutArrow() { assertEquals(null, VisualGuide.path("گوشی را خاموش و روشن کنید.")) }
    @Test fun longLastSegmentIsTrimmed() {
        val p = VisualGuide.path("داخل برنامه: تنظیمات ← اعلان‌ها را باز کنید و برای پیام‌ها تنظیم کنید.")!!
        assertEquals("اعلان‌ها", p.last())
    }
    @Test fun guidesHaveVisuals() {
        val withPath = GuideData.all.sumOf { g -> g.steps.count { VisualGuide.path(it) != null } }
        assertTrue(withPath >= 5)
    }
}
