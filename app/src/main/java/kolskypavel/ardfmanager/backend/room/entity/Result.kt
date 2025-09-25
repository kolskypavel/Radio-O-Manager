package kolskypavel.ardfmanager.backend.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.sportident.SIConstants
import kolskypavel.ardfmanager.backend.sportident.SITime
import java.io.Serializable
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "result", foreignKeys = [ForeignKey(
        entity = Race::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("race_id"),
        onDelete = ForeignKey.CASCADE
    ),
        ForeignKey(
            entity = Competitor::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("competitor_id"),
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Result(
    @PrimaryKey var id: UUID,
    @ColumnInfo(name = "race_id") var raceId: UUID,
    @ColumnInfo(name = "competitor_id") var competitorId: UUID? = null,
    @ColumnInfo(name = "si_number") var siNumber: Int?,
    @ColumnInfo(name = "card_type") var cardType: Byte,
    @ColumnInfo(name = "check_time") var checkTime: SITime?,
    @ColumnInfo(name = "orig_check_time") var origCheckTime: SITime?, // Immutable copy of original SI Time, used mainly for SI 5 cards
    @ColumnInfo(name = "start_time") var startTime: SITime?,
    @ColumnInfo(name = "orig_start_time") var origStartTime: SITime?, // Immutable copy of original SI Time, used mainly for SI 5 cards
    @ColumnInfo(name = "finish_time") var finishTime: SITime?,
    @ColumnInfo(name = "orig_finish_time") var origFinishTime: SITime?, // Immutable copy of original SI Time, used mainly for SI 5 cards
    @ColumnInfo(name = "readout_time") var readoutTime: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo(name = "automatic_status") var automaticStatus: Boolean,
    @ColumnInfo(name = "result_status") var resultStatus: ResultStatus,
    @ColumnInfo(name = "points") var points: Int = 0,
    @ColumnInfo(name = "run_time") var runTime: Duration,
    @ColumnInfo(name = "modified") var modified: Boolean,
    @ColumnInfo(name = "sent") var sent: Boolean,      //Marked as sent to the server

) : Serializable, Comparable<Result> {
    @Ignore
    var place: Int = 0
    override operator fun compareTo(other: Result): Int {

        //Compare race status
        return if (resultStatus != other.resultStatus) {
            resultStatus.compareTo(other.resultStatus)
        }
        //Compare points - more points are before less points
        else if (points != other.points) {
            points.compareTo(other.points) * -1
        }
        //Compare times
        else {
            runTime.compareTo(other.runTime)
        }
    }

    constructor() : this(
        id = UUID.randomUUID(),
        raceId = UUID.randomUUID(),
        competitorId = null,
        siNumber = 0,
        cardType = SIConstants.SI_CARD5,
        checkTime = SITime(),
        origCheckTime = SITime(),
        startTime = SITime(),
        origStartTime = SITime(),
        finishTime = SITime(),
        origFinishTime = SITime(),
        readoutTime = LocalDateTime.now(),
        automaticStatus = false,
        resultStatus = ResultStatus.OK,
        points = 0,
        runTime = Duration.ZERO,
        modified = false,
        sent = false
    )
}