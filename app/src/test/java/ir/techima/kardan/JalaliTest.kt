package ir.techima.kardan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliTest {
    @Test fun knownDates() {
        assertEquals(Jalali.Date(1405, 8, 5), Jalali.fromGregorian(2026, 10, 27))
        assertEquals(Jalali.Date(1405, 1, 1), Jalali.fromGregorian(2026, 3, 21))
        assertEquals(Jalali.Date(1404, 12, 29), Jalali.fromGregorian(2026, 3, 20))
        assertEquals(Jalali.Date(1403, 1, 1), Jalali.fromGregorian(2024, 3, 20))
        assertEquals(Jalali.Date(1406, 1, 1), Jalali.fromGregorian(2027, 3, 21))
    }

    @Test fun roundTrip() {
        for (y in 1380..1420) for (m in 1..12) for (d in listOf(1, 15, Jalali.daysInMonth(y, m))) {
            val g = Jalali.toGregorian(y, m, d)
            assertEquals(Jalali.Date(y, m, d), Jalali.fromGregorian(g[0], g[1], g[2]))
        }
    }

    @Test fun leapYears() {
        assertTrue(Jalali.isLeap(1403))
        assertEquals(30, Jalali.daysInMonth(1403, 12))
        assertEquals(29, Jalali.daysInMonth(1404, 12))
    }

    @Test fun digits() {
        assertEquals("۱۴۰۵", Persian.digits(1405))
        assertEquals("1405", Persian.latinDigits("۱۴۰۵"))
        assertEquals("کتاب", Persian.normalize("كتاب"))
    }
}
