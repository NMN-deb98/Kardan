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
