package kolskypavel.ardfmanager.backend.helpers

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object TimeProcessor {
    fun hoursMinutesFormatter(time: LocalDateTime): String {
        return DateTimeFormatter.ofPattern("HH:mm").format(time).toString()
    }

    fun formatLocalDateTime(time: LocalDateTime): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(time).toString()
    }

    fun formatLocalDate(time: LocalDate): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(time).toString()
    }

    fun formatLocalTime(time: LocalTime): String {
        return DateTimeFormatter.ofPattern("HH:mm:ss").format(time).toString()
    }

    // Converts a Duration to a string in the format "mm:ss"
    fun durationToMinuteString(duration: Duration): String {
        val seconds = duration.seconds
        return if (kotlin.math.abs(seconds / 60) <= 99) {
            String.format("%02d:%02d", seconds / 60, kotlin.math.abs(seconds) % 60)
        } else {
            String.format("%d:%02d", seconds / 60, kotlin.math.abs(seconds) % 60)
        }
    }

    @Throws(IllegalArgumentException::class)
    fun minuteStringToDuration(string: String): Duration {
        val parts = string.split(":")
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid time format. Expected format: mmm:ss")
        }

        val minutes = parts[0].toLongOrNull() ?: throw IllegalArgumentException("Invalid minutes")
        val seconds = parts[1].toLongOrNull() ?: throw IllegalArgumentException("Invalid seconds")

        if (minutes < 0 || seconds < 0 || seconds >= 60) {
            throw IllegalArgumentException("Invalid time values")
        }

        return Duration.ofMinutes(minutes).plusSeconds(seconds)
    }

    fun getAbsoluteDateTimeFromRelativeTime(
        startDateTime: LocalDateTime,
        relativeStartTime: Duration
    ): LocalDateTime {
        return startDateTime.plusSeconds(relativeStartTime.seconds)
    }

    /**
     * If a competitor is started or not
     */
    fun hasStarted(
        startDateTime: LocalDateTime,
        relativeStartTime: Duration,
        curTime: LocalDateTime
    ): Boolean {
        return (curTime.isAfter(
            getAbsoluteDateTimeFromRelativeTime(
                startDateTime,
                relativeStartTime
            )
        ))
    }

    // Calculates the duration from competitor's start till now
    fun runDurationFromStart(
        startDateTime: LocalDateTime,
        relativeStartTime: Duration
    ): Duration? {
        //Check if the competitor started
        if (hasStarted(startDateTime, relativeStartTime, LocalDateTime.now())) {
            return Duration.between(startDateTime, LocalDateTime.now())
        }
        return null
    }

    // Calculates the duration from competitor's start till now and returns it as a string
    fun runDurationFromStartString(
        startDateTime: LocalDateTime,
        relativeStartTime: Duration
    ): String {
        //Check if the competitor started
        if (hasStarted(startDateTime, relativeStartTime, LocalDateTime.now())) {
            return durationToMinuteString(
                Duration.between(
                    startDateTime + relativeStartTime,
                    LocalDateTime.now()
                )
            )
        }
        return ""
    }

    /**
     * If a competitor is in limit or not
     */
    fun isInLimit(
        startDateTime: LocalDateTime,
        relativeStartTime: Duration,
        timeLimit: Duration,
        curTime: LocalDateTime
    ): Boolean {
        return if (hasStarted(startDateTime, relativeStartTime, LocalDateTime.now())) {
            curTime.isBefore(startDateTime.plusSeconds(timeLimit.seconds))
        } else true
    }

    //Calculates the duration to limit -
    fun durationToLimit(
        startDateTime: LocalDateTime,
        relativeStartTime: Duration,
        timeLimit: Duration,
        curTime: LocalDateTime
    ): Duration? {
        if (hasStarted(startDateTime, relativeStartTime, curTime)) {
            return Duration.between(
                curTime,
                startDateTime.plusSeconds(timeLimit.seconds)
            )
        }
        return null
    }
}