package kolskypavel.ardfmanager.backend.room.entity.embeddeds

import androidx.room.Embedded
import androidx.room.Relation
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import java.io.Serializable

// Contains all information about SI readout - including competitor (if matched)
data class ResultData(
    @Embedded var result: Result,

    @Relation(
        entityColumn = "result_id",
        parentColumn = "id",
        entity = Punch::class
    )
    var punches: List<AliasPunch>,
    @Relation(
        parentColumn = "competitor_id",
        entityColumn = "id",
        entity = Competitor::class
    ) var competitorCategory: CompetitorCategory?

) : Serializable {
    fun getPunchList(): List<Punch> {
        return punches.map { p -> p.punch }
    }

    // Convert to CSV for further processing
    fun toReadoutCSVString(): String {
        val siNumber = result.siNumber ?: ""
        val checkTime = result.checkTime?.getTimeString() ?: ""
        val startTime = result.startTime?.getTimeString() ?: ""
        val finishTime = result.finishTime?.getTimeString() ?: ""

        var numControls = 0
        val punchFields = StringBuilder()
        for (punch in punches) {
            if (punch.punch.punchType == SIRecordType.CONTROL) {
                if (punchFields.isNotEmpty()) punchFields.append(";")
                punchFields.append("${punch.punch.siCode};${punch.punch.siTime.getTimeString()}")
                numControls++
            }
        }

        return listOf(siNumber, checkTime, startTime, finishTime, numControls)
            .joinToString(";") + if (punchFields.isNotEmpty()) ";$punchFields" else ""
    }
}
