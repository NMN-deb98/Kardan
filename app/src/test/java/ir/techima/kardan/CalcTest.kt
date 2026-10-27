package ir.techima.kardan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CalcTest {
    @Test fun basic() {
        assertEquals(14.0, Calc.eval("2+3*4")!!, 1e-9)
        assertEquals(20.0, Calc.eval("(2+3)*4")!!, 1e-9)
        assertEquals(2.5, Calc.eval("5/2")!!, 1e-9)
        assertEquals(-3.0, Calc.eval("-1-2")!!, 1e-9)
    }
    @Test fun persianInput() {
        assertEquals(12.0, Calc.eval("۴×۳")!!, 1e-9)
        assertEquals(5.0, Calc.eval("۱۰÷۲")!!, 1e-9)
    }
    @Test fun percent() { assertEquals(0.5, Calc.eval("50%")!!, 1e-9) }
    @Test fun invalid() {
        assertNull(Calc.eval("1/0"))
        assertNull(Calc.eval("2+"))
        assertNull(Calc.eval("(2"))
        assertNull(Calc.eval(""))
        assertNull(Calc.eval("abc"))
    }
    @Test fun format() {
        assertEquals("۱٫۲۳۴", Calc.format(1.234))
        assertEquals("۱،۰۰۰", Calc.format(1000.0))
        assertNotNull(Calc.format(0.1 + 0.2))
        assertEquals("۰٫۳", Calc.format(0.1 + 0.2))
    }
    @Test fun converter() {
        val len = Converter.categories[0]
        assertEquals(1000.0, Converter.convert(len, 1, 0, 1.0), 1e-9)
        val t = Converter.categories.last()
        assertEquals(212.0, Converter.convert(t, 0, 1, 100.0), 1e-9)
        assertEquals(273.15, Converter.convert(t, 0, 2, 0.0), 1e-9)
    }
}
