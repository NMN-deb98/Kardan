package ir.techima.kardan

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneContactsTest {
    private val all = listOf(
        PhoneContact(1, "علی رضایی", "0912 123 4567"),
        PhoneContact(2, "مریم احمدی", "+98 935 765 4321"),
        PhoneContact(3, "Sara", "09101112233")
    )
    @Test fun sameNumberSameKeyWithOrWithoutCountryCode() {
        assertEquals(PhoneContacts.key("09121234567"), PhoneContacts.key("+98 912 123 4567"))
        assertEquals(PhoneContacts.key("۰۹۱۲۱۲۳۴۵۶۷"), PhoneContacts.key("09121234567"))
    }
    @Test fun searchByNameTypoAndNumber() {
        assertEquals(listOf(1L), PhoneContacts.filter(all, "علی").map { it.id })
        assertEquals(listOf(3L), PhoneContacts.filter(all, "sara").map { it.id })
        assertEquals(listOf(2L), PhoneContacts.filter(all, "۰۹۳۵۷۶").map { it.id }.ifEmpty { PhoneContacts.filter(all, "935765").map { it.id } })
        assertEquals(3, PhoneContacts.filter(all, "").size)
    }
    @Test fun noMatchIsEmpty() { assertTrue(PhoneContacts.filter(all, "ژورژیا").isEmpty()) }
}

class AssistantContactsTest {
    private val zone = TimeZone.getTimeZone("Asia/Tehran")
    private val now = 1_790_000_000_000L
    private fun one() = listOf(QuickContact(1, "علی رضایی", "09121234567"))

    @Test fun callNeverStartsWithoutConfirmation() {
        val r = Assistant.reply("به علی زنگ بزن", now, zone) { one() }
        assertEquals(OpStatus.NEEDS_CONFIRM, r.status)
        assertTrue(r.actions.first() is AssistantAction.Dial)
    }
    @Test fun smsOnlyOpensTheMessageApp() {
        val r = Assistant.reply("برای علی پیامک بزن", now, zone) { one() }
        assertEquals(OpStatus.NEEDS_CONFIRM, r.status)
        assertTrue(r.actions.first() is AssistantAction.Sms)
    }
    @Test fun unknownContactIsReportedHonestly() {
        val r = Assistant.reply("به علی زنگ بزن", now, zone) { emptyList() }
        assertEquals(OpStatus.UNKNOWN, r.status)
        assertTrue(r.actions.none { it is AssistantAction.Dial })
    }
    @Test fun severalMatchesAskWhichOne() {
        val r = Assistant.reply("به علی زنگ بزن", now, zone) {
            listOf(QuickContact(1, "علی رضایی", "09121234567"), QuickContact(2, "علی احمدی", "09351112233"))
        }
        assertTrue(r.question)
        assertEquals(2, r.actions.size)
    }
    @Test fun missingNameAsksAQuestion() {
        val r = Assistant.reply("زنگ بزن", now, zone) { one() }
        assertTrue(r.question)
    }
}

class WebUrlsTest {
    @Test fun googleUrlIsEncodedAndPersian() {
        val u = WebUrls.google("فیلم معمایی")
        assertTrue(u.startsWith("https://www.google.com/search?hl=fa&q="))
        assertFalse(u.contains(" "))
    }
    @Test fun scopesAddSiteFilter() {
        assertTrue(WebUrls.google("Inception", "IMDb").contains("site%3Aimdb.com"))
        assertTrue(WebUrls.google("اینسپشن", "ویکی‌پدیا").contains("fa.wikipedia.org"))
    }
    @Test fun typeWordAddedOnlyWhenMissing() {
        assertEquals("سریال معمایی", WebUrls.movieQuery("معمایی", "سریال"))
        assertEquals("فیلم معمایی", WebUrls.movieQuery("فیلم معمایی", "سریال"))
    }
    @Test fun onlyHttpsPagesAllowed() {
        assertTrue(WebRules.allowed("https://www.google.com/search?q=a"))
        assertFalse(WebRules.allowed("http://example.com"))
        assertFalse(WebRules.allowed("intent://scan/#Intent;end"))
        assertFalse(WebRules.allowed("file:///sdcard/a.html"))
        assertFalse(WebRules.allowed("javascript:alert(1)"))
        assertFalse(WebRules.allowed(null))
    }
}

class PackageCatalogTest {
    @Test fun everyRowIsSane() {
        (PackageCatalog.rightel + PackageCatalog.rightelUnlimited).forEach {
            assertTrue(it.name, it.priceToman > 0)
            assertEquals(PackageCatalog.RIGHTEL, it.operator)
        }
        PackageCatalog.rightel.forEach { assertTrue(it.name, (it.gb ?: 0.0) > 0 && it.days > 0) }
    }
    @Test fun perGbMath() {
        assertEquals(3000L, PackageCatalog.perGb(9000, 3.0))
        assertNull(PackageCatalog.perGb(0, 3.0))
        assertNull(PackageCatalog.perGb(1000, 0.0))
        assertEquals(500L, PackageCatalog.perDay(1500, 3))
    }
    @Test fun sortedCheapestPerGbFirst() {
        val s = PackageCatalog.sortedByValue(PackageCatalog.rightel)
        val v = s.map { it.priceToman / it.gb!! }
        assertEquals(v.sorted(), v)
    }
    @Test fun unlimitedNeverEntersPerGbSort() {
        assertTrue(PackageCatalog.sortedByValue(PackageCatalog.rightelUnlimited).isEmpty())
    }
    @Test fun everyOperatorHasOfficialLinks() {
        PackageCatalog.operators.forEach { o -> assertTrue(o, PackageCatalog.officialLinks(o).all { it.url.startsWith("https://") }) }
    }
}
