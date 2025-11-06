package kolskypavel.ardfmanager.times

import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RunTimesUnitTests {
    val baseDateTime: LocalDateTime = LocalDateTime.of(2025, 9, 19, 10, 0, 0)


    @Test
    fun durationToMinuteStringTest() {
        assertEquals("00:00", TimeProcessor.durationToFormattedString(Duration.ZERO, true))
        assertEquals(
            "64:00", TimeProcessor.durationToFormattedString(
                Duration.ofMinutes(64), true
            )
        )
        assertEquals(
            "59:22",
            TimeProcessor.durationToFormattedString(
                Duration.ofMinutes(59) + Duration.ofSeconds(22), true
            )
        )
        assertEquals(
            "120:00",
            TimeProcessor.durationToFormattedString(Duration.ofHours(2), true)
        )
        assertEquals(
            "120:25",
            TimeProcessor.durationToFormattedString(
                Duration.ofHours(2) + Duration.ofSeconds(25), true
            )
        )
        assertEquals(
            "1000:00", TimeProcessor.durationToFormattedString(
                Duration.ofMinutes(1000), true
            )
        )
        assertEquals(
            "-10:00", TimeProcessor.durationToFormattedString(
                Duration.ofMinutes(-10), true
            )
        )
        assertEquals(
            "-100:00", TimeProcessor.durationToFormattedString(
                Duration.ofMinutes(-100), true
            )
        )
        assertEquals(
            "-10000:00", TimeProcessor.durationToFormattedString(
                Duration.ofMinutes(-10000), true
            )
        )
    }


    @Test
    fun durationToHourStringTest() {
        assertEquals("00:00:00", TimeProcessor.durationToFormattedString(Duration.ZERO, false))
        assertEquals(
            "00:15:19",
            TimeProcessor.durationToFormattedString(
                Duration.ofMinutes(15) + Duration.ofSeconds(19), false
            )
        )
        assertEquals(
            "01:04:00", TimeProcessor.durationToFormattedString(
                Duration.ofMinutes(64), false
            )
        )
        assertEquals(
            "00:59:22",
            TimeProcessor.durationToFormattedString(
                Duration.ofMinutes(59) + Duration.ofSeconds(22), false
            )
        )
        assertEquals(
            "02:14:22",
            TimeProcessor.durationToFormattedString(
                Duration.ofHours(2) +
                        Duration.ofMinutes(14) + Duration.ofSeconds(22), false
            )
        )
    }

    @Test
    fun testFormatLocalDateTime() {
        val formatted = TimeProcessor.formatDisplayLocalDateTime(LocalDateTime.of(2025, 9, 19, 10, 0, 0))
        assertEquals("2025-09-19 10:00:00", formatted)
    }

    @Test
    fun testFormatLocalDate() {
        val formatted = TimeProcessor.formatLocalDate(LocalDate.of(2025, 9, 19))
        assertEquals("2025-09-19", formatted)
    }

    @Test
    fun testFormatLocalTime() {
        val formatted = TimeProcessor.formatLocalTime(LocalTime.of(12, 34, 56))
        assertEquals("12:34:56", formatted)
    }

    @Test
    fun testMinuteStringToDurationValid() {
        val dur = TimeProcessor.minuteStringToDuration("12:34")
        assertEquals(Duration.ofMinutes(12).plusSeconds(34), dur)
    }

    @Test
    fun testMinuteStringToDurationInvalid() {
        assertThrows(IllegalArgumentException::class.java) {
            TimeProcessor.minuteStringToDuration("12")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TimeProcessor.minuteStringToDuration("12:xx")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TimeProcessor.minuteStringToDuration("12:99")
        }
    }

    @Test
    fun testGetAbsoluteDateTimeFromRelativeTime() {
        val abs =
            TimeProcessor.getAbsoluteDateTimeFromRelativeTime(baseDateTime, Duration.ofMinutes(5))
        assertEquals(baseDateTime.plusMinutes(5), abs)
    }

    @Test
    fun testHasStartedBeforeStart() {
        val result = TimeProcessor.hasStarted(
            baseDateTime,
            Duration.ofMinutes(5),
            baseDateTime.plusMinutes(4)
        )
        assertFalse(result)
    }

    @Test
    fun testHasStartedAfterStart() {

        val result = TimeProcessor.hasStarted(
            baseDateTime,
            Duration.ofMinutes(5),
            baseDateTime.plusMinutes(6)
        )
        assertTrue(result)
    }

    @Test
    fun testRunDurationFromStartBeforeStart() {
        val curTime = baseDateTime.plusMinutes(4)
        val result = TimeProcessor.runDurationFromStart(baseDateTime, Duration.ofMinutes(5), curTime)
        assertNull(result) // not started yet
    }

    @Test
    fun testRunDurationFromStartAfterStart() {
        val curTime = baseDateTime.plusMinutes(15)
        val result = TimeProcessor.runDurationFromStart(baseDateTime, Duration.ofMinutes(5), curTime)
        assertEquals(Duration.ofMinutes(10), result)
    }

    @Test
    fun testRunDurationFromStartStringBeforeStart() {
        val dataProcessor: DataProcessor = mock()
        whenever(dataProcessor.useMinuteTimeFormat()).thenReturn(true)

        val curTime = baseDateTime.plusMinutes(4)
        val str = TimeProcessor.runDurationFromStartString(baseDateTime, Duration.ofMinutes(5), dataProcessor, curTime)
        assertEquals("", str) // not started
    }

    @Test
    fun testRunDurationFromStartStringAfterStart() {
        val dataProcessor: DataProcessor = mock()
        whenever(dataProcessor.useMinuteTimeFormat()).thenReturn(true)

        val curTime = baseDateTime.plusMinutes(15)
        val str = TimeProcessor.runDurationFromStartString(baseDateTime, Duration.ofMinutes(5), dataProcessor, curTime)
        assertEquals("10:00", str) // competitor started at 10:05, so at 10:15 -> 10 minutes
    }

    @Test
    fun testIsInLimitTrueWhenBeforeLimit() {
        val curTime = baseDateTime.plusMinutes(15)
        val result = TimeProcessor.isInLimit(baseDateTime, Duration.ofMinutes(5), Duration.ofMinutes(30), curTime)
        assertTrue(result)
    }

    @Test
    fun testIsInLimitFalseWhenAfterLimit() {
        val curTime = baseDateTime.plusMinutes(40)
        val result = TimeProcessor.isInLimit(baseDateTime, Duration.ofMinutes(5), Duration.ofMinutes(30), curTime)
        assertFalse(result)
    }

    @Test
    fun testDurationToLimitBeforeStart() {
        val curTime = baseDateTime.plusMinutes(4)
        val result = TimeProcessor.durationToLimit(baseDateTime, Duration.ofMinutes(5), Duration.ofMinutes(30), curTime)
        assertNull(result) // not started yet
    }

    @Test
    fun testDurationToLimitAfterStart() {
        val curTime = baseDateTime.plusMinutes(20)
        val result = TimeProcessor.durationToLimit(baseDateTime, Duration.ofMinutes(5), Duration.ofMinutes(30), curTime)
        // competitor started at 10:05, limit is until 10:35, so at 10:20 = 15 minutes left
        assertEquals(Duration.ofMinutes(15), result)
    }
}