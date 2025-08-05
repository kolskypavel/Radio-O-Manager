package kolskypavel.ardfmanager.backend.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kolskypavel.ardfmanager.backend.room.database.DateTimeTypeConverter
import kolskypavel.ardfmanager.backend.room.enums.RaceBand
import kolskypavel.ardfmanager.backend.room.enums.RaceLevel
import kolskypavel.ardfmanager.backend.room.enums.RaceType
import java.io.Serializable
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "race"
)
@TypeConverters(DateTimeTypeConverter::class)
data class Race(
    @PrimaryKey var id: UUID,
    var name: String,
    @ColumnInfo(name = "api_key") var apiKey: String,
    @ColumnInfo(name = "start_date_time") var startDateTime: LocalDateTime,
    @ColumnInfo(name = "race_type") var raceType: RaceType,
    @ColumnInfo(name = "race_level") var raceLevel: RaceLevel,
    @ColumnInfo(name = "race_band") var raceBand: RaceBand,
    @ColumnInfo(name = "time_limit") var timeLimit: Duration
) : Serializable {
    constructor() : this(
        UUID.randomUUID(),
        "", "",
        LocalDateTime.now(),
        RaceType.CLASSIC,
        RaceLevel.PRACTICE,
        RaceBand.M80,
        Duration.ZERO
    )
}
