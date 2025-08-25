package kolskypavel.ardfmanager.backend.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import java.io.Serializable
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "competitor", indices = [Index(
        value = ["start_number", "race_id"],
        unique = true
    ), Index("category_id")],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("category_id"),
            onDelete = ForeignKey.SET_NULL,

            ),
        ForeignKey(
            entity = Race::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("race_id"),
            onDelete = ForeignKey.CASCADE
        )]
)
data class Competitor(
    @PrimaryKey var id: UUID,
    @ColumnInfo(name = "race_id") var raceId: UUID,
    @ColumnInfo(name = "category_id") var categoryId: UUID? = null,
    @ColumnInfo(name = "first_name") var firstName: String,
    @ColumnInfo(name = "last_name") var lastName: String,
    @ColumnInfo(name = "club") var club: String,
    @ColumnInfo(name = "index") var index: String,
    @ColumnInfo(name = "is_man") var isMan: Boolean = false,
    @ColumnInfo(name = "birth_year") var birthYear: Int? = null,
    @ColumnInfo(name = "si_number") var siNumber: Int? = null,
    @ColumnInfo(name = "si_rent") var siRent: Boolean = false,
    @ColumnInfo(name = "start_number") var startNumber: Int,
    @ColumnInfo(name = "drawn_start_time") var drawnRelativeStartTime: Duration? = null,
) : Serializable {
    fun getFullName(): String {
        return "${lastName.uppercase()} $firstName"
    }

    fun getNameWithStartNumber(): String {
        return "${lastName.uppercase()} $firstName (${startNumber})"
    }

    fun toSimpleCsvString(categoryName: String): String {
        return "${siNumber ?: ""};${firstName};${lastName};${categoryName};${isMan.compareTo(false)};${birthYear};;${club};;${startNumber};${index}"
    }

    fun toStartCsvString(
        categoryName: String,
        raceStart: LocalDateTime
    ): String {

        val real =
            if (drawnRelativeStartTime != null) {
                TimeProcessor.hoursMinutesFormatter(raceStart + drawnRelativeStartTime)
            } else null

        return "${startNumber};${lastName};${firstName};${categoryName};;${real ?: ""};${index};;${club};${siNumber ?: ""}"
    }

    //Non-args constructor
    constructor() : this(
        UUID.randomUUID(),
        UUID.randomUUID(),
        null,
        "Test",
        "Tester",
        "AC Test",
        "ACT0001",
        true,
        2000,
        123456789,
        false,
        0,
        null
    )
}