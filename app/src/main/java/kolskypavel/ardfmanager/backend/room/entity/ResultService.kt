package kolskypavel.ardfmanager.backend.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kolskypavel.ardfmanager.backend.room.enums.ResultServiceStatus
import kolskypavel.ardfmanager.backend.room.enums.ResultServiceType
import java.io.Serializable
import java.time.Duration
import java.time.LocalTime
import java.util.UUID

@Entity(
    tableName = "result_service",
    foreignKeys = [ForeignKey(
        entity = Race::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("race_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class ResultService(
    @PrimaryKey var id: UUID,
    @ColumnInfo(name = "service_type") var serviceType: ResultServiceType,
    @ColumnInfo(name = "race_id") var raceId: UUID,
    @ColumnInfo(name = "url") var url: String,
    @ColumnInfo(name = "api_key") var apiKey: String,
    @ColumnInfo(name = "interval") var interval: Duration,
    @ColumnInfo(name = "enabled") var enabled: Boolean,
    @ColumnInfo(name = "status") var status: ResultServiceStatus,
    @ColumnInfo(name = "error_text") var errorText: String,
    @ColumnInfo(name = "sent") var sent: Int = 0,
    @ColumnInfo(name = "sent_at") var sentAt: LocalTime,
) : Serializable {
    constructor(raceId: UUID) : this(
        UUID.randomUUID(),
        ResultServiceType.ROBIS,
        raceId,
        "",
        "",
        Duration.ofSeconds(2),
        false,
        ResultServiceStatus.RUNNING,
        "",
        0,
        LocalTime.now()
    )
}