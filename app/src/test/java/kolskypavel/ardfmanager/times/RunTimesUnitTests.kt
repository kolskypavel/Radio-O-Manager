package kolskypavel.ardfmanager.times

import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class RunTimesUnitTests {

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
    fun durationFromStartTest() {
//        assertEquals(
//            null,
//            TimeProcessor.runDurationFromStart()
//        )
    }
}