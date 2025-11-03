package kolskypavel.ardfmanager.backend.sportident

import java.io.Serializable
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Wrapper class for calculating the split times
 */
class SITime(
    private var time: LocalTime,
    private var dayOfWeek: Int = 0, //0 - Sunday, 6 - Saturday
    private var week: Int = 0
) : Serializable {

    private var seconds: Long = 0

    constructor() : this(LocalTime.MIDNIGHT, 0, 0)
    constructor(time: LocalTime) : this(time, 0, 0) {}
    constructor(other: SITime) : this(other.time, other.dayOfWeek, other.week)

    init {
        calculateSeconds()
    }

    constructor(orig: Long) : this() {
        this.seconds = orig
        this.time = LocalTime.of(
            ((orig / 3600) % 24).toInt(),   // max 24 hours in a day
            ((orig / 60) % 60).toInt(),     // max 60 minutes in an hour
            (orig % 60).toInt()             // max 60 seconds in a minute
        )
        this.week = (orig / SIConstants.SECONDS_WEEK).toInt()    //Weeks from SI synchronization
        this.dayOfWeek =
            ((orig / SIConstants.SECONDS_DAY) % 7).toInt()  //Days from SI synchronization
    }

    private fun calculateSeconds() {
        this.seconds =
            week * SIConstants.SECONDS_WEEK + dayOfWeek * SIConstants.SECONDS_DAY + time.toSecondOfDay()
    }

    override fun toString(): String {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        return "${time.format(timeFormatter)},$dayOfWeek,$week"
    }

    fun localTimeFormatter(): String {
        return DateTimeFormatter.ofPattern("HH:mm:ss").format(time)
    }

    fun addHalfDay() {
        if (time.isAfter(LocalTime.NOON)) {
            dayOfWeek++
            adjustDayWeek()
        }
        this.time = time.plusHours(12)
        calculateSeconds()
    }

    fun addDay() {
        dayOfWeek++
        adjustDayWeek()
        calculateSeconds()
    }

    /**
     * Adjust the weeks and days for SI card 5
     */
    private fun adjustDayWeek() {
        if (dayOfWeek > 6) {
            week++
            dayOfWeek %= 7
        }
    }

    //Getters and setters
    fun getTime() = time
    fun getDayOfWeek() = dayOfWeek
    fun getWeek() = week

    fun getSeconds() = seconds

    fun getTimeString(): String {
        return time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }

    fun setTime(newTime: LocalTime) {
        this.time = newTime
        calculateSeconds()
    }

    fun setDayOfWeek(newDayOfWeek: Int) {
        this.dayOfWeek = newDayOfWeek
        calculateSeconds()
    }

    fun setWeek(newWeek: Int) {
        this.week = newWeek
        calculateSeconds()
    }

    /**
     * Checks if another SITime is after or equal to this SI time
     */
    fun isAtOrAfter(other: SITime): Boolean {
        return this.seconds >= other.seconds
    }

    fun isAfter(other: SITime): Boolean {
        return this.seconds > other.seconds
    }

    fun compareTo(other: SITime?): Int {
        return this.seconds.compareTo(other?.seconds ?: 0)
    }

    /**
     * Converts SI time to localDateTime when start 00 is provided
     */
    fun toLocalDateTime(startZero: LocalDateTime): LocalDateTime {
        // Start from the startZero date at the SITime's local time
        var candidateDate = startZero.toLocalDate()

        // Find the first date >= startZero.date that has the same SI day index
        while (dayOfWeekToSIIndex(candidateDate.dayOfWeek) != this.dayOfWeek) {
            candidateDate = candidateDate.plusDays(1)
        }

        var candidate = candidateDate.atTime(time)

        // Account for the week offset encoded in this SITime
        if (this.week > 0) {
            candidate = candidate.plusWeeks(this.week.toLong())
        }

       return candidate
    }

    companion object {
        @Throws(IllegalArgumentException::class)
        fun from(string: String): SITime {
            try {
                val split = string.split(",")
                val time = LocalTime.parse(split[0])
                val dayOfWeek = split[1].toInt()
                val week = split[2].toInt()

                val siTime = SITime(time, dayOfWeek, week)
                siTime.calculateSeconds()
                return siTime

            } catch (e: Exception) {
                throw java.lang.IllegalArgumentException("Error when parsing SI time", e)
            }
        }

        fun split(start: SITime, end: SITime): Duration {
            return Duration.ofSeconds(end.seconds - start.seconds)
        }

        fun difference(start: SITime, end: SITime): Duration {
            return Duration.ofSeconds(kotlin.math.abs(end.seconds - start.seconds))
        }

        // Convert DayOfWeek (1 - Monday, 7 - Sunday) to SI index (0 - Sunday, 6 - Saturday)
        fun dayOfWeekToSIIndex(dayOfWeek: DayOfWeek): Int {
            return when (dayOfWeek) {
                DayOfWeek.MONDAY -> 1
                DayOfWeek.TUESDAY -> 2
                DayOfWeek.WEDNESDAY -> 3
                DayOfWeek.THURSDAY -> 4
                DayOfWeek.FRIDAY -> 5
                DayOfWeek.SATURDAY -> 6
                DayOfWeek.SUNDAY -> 0
            }
        }
    }
}