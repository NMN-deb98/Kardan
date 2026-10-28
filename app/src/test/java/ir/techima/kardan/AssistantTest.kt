package ir.techima.kardan

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantTest {
    private val zone = TimeZone.getTimeZone("Asia/Tehran")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        Calendar.getInstance(zone).apply { clear(); set(y, m - 1, d, h, min, 0) }.timeInMillis
    private val now = at(2026, 10, 27, 10, 0)

    @Test fun reminderTomorrowNine() {
        val r = Assistant.reply("فردا ساعت ۹ یادآوری بساز: خرید نان", now, zone)
        assertEquals(OpStatus.NEEDS_CONFIRM, r.status)
        val a = r.actions.first() as AssistantAction.CreateTask
        assertEquals("خرید نان", a.title)
        assertEquals(at(2026, 10, 28, 9, 0), a.due)
        assertTrue(a.remind)
    }

    @Test fun eveningHourIsPm() {
        val w = Assistant.parseWhen("امروز ساعت ۶ عصر", now, zone)
        assertEquals(at(2026, 10, 27, 18, 0), w.millis)
    }

    @Test fun relativeMinutes() {
        val w = Assistant.parseWhen("۳۰ دقیقه دیگه", now, zone)
        assertEquals(now + 30 * 60_000L, w.millis)
    }

    @Test fun reminderWithoutTimeHasNoAlarm() {
        val r = Assistant.reply("یادآوری بساز: زنگ زدن به مادر", now, zone)
        val a = r.actions.first() as AssistantAction.CreateTask
        assertEquals(null, a.due)
        assertTrue(!a.remind)
    }

    @Test fun timer() {
        val r = Assistant.reply("تایمر ۱۰ دقیقه", now, zone)
        assertEquals(600, (r.actions.first() as AssistantAction.StartTimer).seconds)
    }

    @Test fun worldClockIstanbul() {
        val r = Assistant.reply("ساعت استانبول را نشان بده", now, zone)
        assertEquals(OpStatus.DONE, r.status)
        assertTrue(r.message.contains("استانبول"))
    }

    @Test fun unsupportedLiveData() {
        assertEquals(OpStatus.UNSUPPORTED, Assistant.reply("قیمت دلار چنده", now, zone).status)
    }

    @Test fun guideSearch() {
        val r = Assistant.reply("چرا گوشی من کند شده؟", now, zone)
        assertNotNull(r.actions.firstOrNull { it is AssistantAction.OpenGuide && (it as AssistantAction.OpenGuide).id == "slow-phone" })
    }

    @Test fun settingsAreHonest() {
        val r = Assistant.reply("وای فای را روشن کن", now, zone)
        assertEquals(OpStatus.NEEDS_USER_ACTION, r.status)
    }

    @Test fun ambiguousAsksOneShortQuestion() {
        val r = Assistant.reply("اینترنت", now, zone)
        assertTrue(r.question)
        assertTrue(r.suggestions.isNotEmpty())
    }

    @Test fun confirmRepliesCanBeCancelled() {
        // every action that changes something must first ask for confirmation
        val r = Assistant.reply("چراغ‌قوه را روشن کن", now, zone)
        assertEquals(OpStatus.NEEDS_CONFIRM, r.status)
    }
}
