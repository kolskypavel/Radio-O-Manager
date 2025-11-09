package kolskypavel.ardfmanager.backend.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kolskypavel.ardfmanager.backend.room.database.DateTimeTypeConverter
import kolskypavel.ardfmanager.backend.room.enums.RaceBand
import kolskypavel.ardfmanager.backend.room.enums.RaceType
import java.io.Serializable
import java.time.Duration
import java.util.UUID

@Entity(
    tableName = "category", indices = [
        Index(
            value = ["name", "race_id"],
            unique = true
        )],
    foreignKeys = [ForeignKey(
        entity = Race::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("race_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
@TypeConverters(DateTimeTypeConverter::class)
data class Category(
    @PrimaryKey var id: UUID,
    @ColumnInfo(name = "race_id") var raceId: UUID,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "is_man") var isMan: Boolean,
    @ColumnInfo(name = "max_age") var maxAge: Int?,
    @ColumnInfo(name = "length") var length: Int,
    @ColumnInfo(name = "climb") var climb: Int,
    @ColumnInfo(name = "order") var order: Int,
    @ColumnInfo(name = "different_properties") var differentProperties: Boolean = false,
    @ColumnInfo(name = "race_type") var raceType: RaceType? = null,
    @ColumnInfo(name = "category_band") var categoryBand: RaceBand? = null,
    @ColumnInfo(name = "limit") var timeLimit: Duration? = null,
    @ColumnInfo(name = "control_points_string") var controlPointsString: String
) : Serializable {

    fun toCSVString(): String {
        return "$name;${isMan.compareTo(false)};${maxAge ?: 0};${length};${climb};${order};${raceType?.value ?: ""};${timeLimit?.toMinutes() ?: ""}}"
    }

    constructor(name: String) : this(
        UUID.randomUUID(),
        UUID.randomUUID(),
        name,
        true,
        null,
        0,
        0,
        0,
        false,
        null,
        null,
        null,
        ""
    )
}
