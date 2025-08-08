package kolskypavel.ardfmanager.backend.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kolskypavel.ardfmanager.backend.room.enums.PunchStatus
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import kolskypavel.ardfmanager.backend.sportident.SITime
import java.io.Serializable
import java.time.Duration
import java.util.UUID

@Entity(
    tableName = "punch", foreignKeys = [ForeignKey(
        entity = Result::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("result_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Punch(
    @PrimaryKey var id: UUID,
    @ColumnInfo(name = "race_id") var raceId: UUID,
    @ColumnInfo(name = "result_id") var resultId: UUID?,
    @ColumnInfo(name = "card_number") var cardNumber: Int? = null,
    @ColumnInfo(name = "si_code") var siCode: Int,
    @ColumnInfo(name = "si_time") var siTime: SITime,
    @ColumnInfo(name = "orig_si_time") var origSiTime: SITime, // Immutable copy of original SI Time, used mainly for SI 5 cards
    @ColumnInfo(name = "punch_type") var punchType: SIRecordType,
    @ColumnInfo(name = "order") var order: Int,
    @ColumnInfo(name = "punch_status") var punchStatus: PunchStatus,      //Holds the original SI Time in case a punch was modified
    @ColumnInfo(name = "split") var split: Duration
) : Serializable {
    fun toCsvString(): String {
        return "${cardNumber ?: ""};${siCode};${siTime}"
    }

    // For debugging purposes
    constructor() : this(
        id = UUID.randomUUID(),
        raceId = UUID.randomUUID(),
        resultId = null,
        cardNumber = null,
        siCode = 0,
        siTime = SITime(),
        origSiTime = SITime(),
        punchType = SIRecordType.CONTROL,
        order = 0,
        punchStatus = PunchStatus.UNKNOWN,
        split = Duration.ZERO
    )
}
