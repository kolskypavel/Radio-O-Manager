package kolskypavel.ardfmanager.times

import junit.framework.TestCase.assertEquals
import kolskypavel.ardfmanager.backend.sportident.SITime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalTime

class SITimeUnitTests {

    @Test
    fun checkBasicSeconds() {
        val time = SITime()
        assertEquals("0", time.getSeconds().toString())
        time.setTime(LocalTime.of(1, 0))
        assertEquals("3600", time.getSeconds().toString())
    }

    @Test
    fun checkToString() {
        val time = SITime(LocalTime.of(19, 20), 0, 0)
        assertEquals("19:20:00,0,0", time.toString())
    }

    @Test
    fun checkHalfDayAddition() {
        val time = SITime(LocalTime.of(9, 0))
        assertEquals("32400", time.getSeconds().toString())

        time.addHalfDay()
        assertEquals("75600", time.getSeconds().toString())

        assertEquals("0", time.getDayOfWeek().toString())
        assertEquals("0", time.getWeek().toString())
        time.addHalfDay()
        assertEquals("1", time.getDayOfWeek().toString())

        for (i in 1..13) {
            time.addHalfDay()
        }
        assertEquals("0", time.getDayOfWeek().toString())
        assertEquals("1", time.getWeek().toString())
        time.addHalfDay()
        assertEquals("1", time.getDayOfWeek().toString())
    }

    @Test
    fun checkDayChange() {
        val time = SITime(LocalTime.of(23, 59), 6, 0) // Saturday
        assertEquals(6, time.getDayOfWeek())
        assertEquals(0, time.getWeek())

        time.addDay()
        assertEquals(0, time.getDayOfWeek()) // Should wrap to Sunday
        assertEquals(1, time.getWeek())      // Week increments
    }

    @Test
    fun checkWeekChange() {
        val time = SITime(LocalTime.NOON, 0, 0) // Sunday noon
        for (i in 1..7) {
            time.addDay()
        }
        assertEquals(0, time.getDayOfWeek()) // Back to Sunday
        assertEquals(1, time.getWeek())      // Week incremented
    }

    @Test
    fun checkSplits() {
        val start = SITime(LocalTime.of(10, 0), 0, 0)
        val end = SITime(LocalTime.of(11, 30), 0, 0)

        val split = SITime.split(start, end)
        assertEquals(Duration.ofMinutes(90), split)

        val diff = SITime.difference(end, start)
        assertEquals(Duration.ofMinutes(90), diff)

        // Cross day
        val nextDay = SITime(LocalTime.of(9, 0), 1, 0)
        val daySplit = SITime.split(start, nextDay)
        assertEquals(Duration.ofHours(23), daySplit) // 10:00 -> next day 09:00
    }

    @Test
    fun checkComparison() {
        val t1 = SITime(LocalTime.of(8, 0), 0, 0)
        val t2 = SITime(LocalTime.of(9, 0), 0, 0)

        assertTrue(t2.isAfter(t1))
        assertTrue(t2.isAtOrAfter(t1))
        assertFalse(t1.isAfter(t2))
        assertEquals(-1, t1.compareTo(t2))
        assertEquals(1, t2.compareTo(t1))
        assertEquals(0, t1.compareTo(SITime(LocalTime.of(8, 0), 0, 0)))
    }

    @Test
    fun checkFromString() {
        val time = SITime.from("15:45:30,2,1")
        assertEquals("15:45:30", time.getTimeString())
        assertEquals(2, time.getDayOfWeek())
        assertEquals(1, time.getWeek())
    }

    @Test
    fun checkInvalidFromString() {
        assertThrows(IllegalArgumentException::class.java) {
            SITime.from("invalid,string")
        }
    }
}