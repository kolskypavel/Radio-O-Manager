package kolskypavel.ardfmanager.backend.helpers

import kolskypavel.ardfmanager.backend.DataProcessor
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object TimeProcessor {
    fun hoursMinutesFormatter(time: LocalDateTime): String {
        return DateTimeFormatter.ofPattern("HH:mm").format(time).toString()
    }

    // Formats the given LocalDateTime to a human readable form
    fun formatDisplayLocalDateTime(time: LocalDateTime): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(time).toString()
    }

    //  Formats the given LocalDateTime to ISO format
    fun formatIsoLocalDateTime(time: LocalDateTime): String {
        return DateTimeFormatter.ISO_DATE_TIME.format(time).toString()
    }

    fun formatLocalDate(time: LocalDate): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(time).toString()
    }

    fun formatLocalTime(time: LocalTime): String {
        return DateTimeFormatter.ofPattern("HH:mm:ss").format(time).toString()
    }

    // Converts a Duration to a string in the format "mm:ss" or "HH:mm:ss"
    fun durationToFormattedString(
        duration: Duration,
        useMinutes: Boolean
    ): String {
        val totalSeconds = duration.seconds
        val absSeconds = kotlin.math.abs(totalSeconds)

        return if (useMinutes) {
            val minutes = totalSeconds / 60
            val seconds = absSeconds % 60

            if (kotlin.math.abs(minutes) <= 99) {
                String.format("%02d:%02d", minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        } else {
            val hours = totalSeconds / 3600
            val minutes = (absSeconds % 3600) / 60
            val seconds = absSeconds % 60

            String.format("%02d:%02d:%02d", hours, minutes, seconds)
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
        relativeStartTime: Duration,
        curTime: LocalDateTime
    ): Duration? {
        //Check if the competitor started
        if (hasStarted(startDateTime, relativeStartTime, curTime)) {
            return Duration.between(startDateTime + relativeStartTime, curTime)
        }
        return null
    }

    // Calculates the duration from competitor's start till now and returns it as a string
    fun runDurationFromStartString(
        startDateTime: LocalDateTime,
        relativeStartTime: Duration,
        dataProcessor: DataProcessor,
        curTime: LocalDateTime
    ): String {
        //Check if the competitor started
        if (hasStarted(startDateTime, relativeStartTime, curTime)) {
            return durationToFormattedString(
                Duration.between(
                    startDateTime + relativeStartTime,
                    curTime
                ), dataProcessor.useMinuteTimeFormat()
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
        return if (hasStarted(startDateTime, relativeStartTime, curTime)) {
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
                (startDateTime + relativeStartTime).plusSeconds(timeLimit.seconds)
            )
        }
        return null
    }
}