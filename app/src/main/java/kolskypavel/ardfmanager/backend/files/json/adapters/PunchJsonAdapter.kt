package kolskypavel.ardfmanager.backend.files.json.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.json.temps.PunchJson
import kolskypavel.ardfmanager.backend.files.json.temps.SITimeJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import kolskypavel.ardfmanager.backend.sportident.SITime
import java.util.UUID

class PunchJsonAdapter(val raceId: UUID, val dataProcessor: DataProcessor) {

    @ToJson
    fun toJson(aliasPunch: AliasPunch): PunchJson {
        val punch = aliasPunch.punch
        return PunchJson(
            code = aliasPunch.alias?.name ?: punch.siCode.toString(),
            si_code = punch.siCode,
            control_type = punch.punchType.name,
            punch_status = dataProcessor.punchStatusToShortString(punch.punchStatus),
            split_time = TimeProcessor.durationToFormattedString(punch.split, true)
        )
    }

    @FromJson
    fun fromJson(punchJson: PunchJson): Punch {
        val punchType = SIRecordType.valueOf(punchJson.control_type)
        return Punch(
            id = UUID.randomUUID(),
            raceId = raceId,
            resultId = UUID.randomUUID(),
            cardNumber = 0,
            siCode = if (punchType == SIRecordType.CONTROL) {
                if (punchJson.si_code != null) {
                    punchJson.si_code!!
                } else punchJson.code.toInt()
            } else 0,           // For START and FINISH, siCode is set to 0

            siTime = SITime(),
            origSiTime = SITime(),
            punchType = punchType,
            order = 0,
            punchStatus = dataProcessor
                .shortStringToPunchStatus(punchJson.punch_status),
            split = TimeProcessor.minuteStringToDuration(punchJson.split_time),
        )
    }
}