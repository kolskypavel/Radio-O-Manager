package kolskypavel.ardfmanager.backend.files.json.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.json.temps.PunchJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import java.util.UUID

class PunchJsonAdapter(val raceId: UUID) {
    val siTimeJsonAdapter = SITimeJsonAdapter()

    @ToJson
    fun toJson(aliasPunch: AliasPunch): PunchJson {
        val punch = aliasPunch.punch
        return PunchJson(
            code = aliasPunch.alias?.name ?: punch.siCode.toString(),
            control_type = punch.punchType.name,
            punch_status = DataProcessor.get().punchStatusToShortString(punch.punchStatus),
            si_time = siTimeJsonAdapter.toJson(punch.siTime),
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
                punchJson.code.toInt()
            } else 0,           // TODO: solve situation with alias instead of code
            siTime = siTimeJsonAdapter.fromJson(punchJson.si_time),
            origSiTime = siTimeJsonAdapter.fromJson(punchJson.si_time),
            punchType = punchType,
            order = 0,
            punchStatus = DataProcessor.get()
                .shortStringToPunchStatus(punchJson.punch_status),
            split = TimeProcessor.minuteStringToDuration(punchJson.split_time),
        )
    }
}