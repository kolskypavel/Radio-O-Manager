import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.json.adapters.PunchJsonAdapter
import kolskypavel.ardfmanager.backend.files.json.temps.ResultJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ReadoutData
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import kolskypavel.ardfmanager.backend.sportident.SITime
import java.time.LocalDateTime
import java.util.UUID

class ResultJsonAdapter(
    val race: Race,
    val dataProcessor: DataProcessor
) {
    val punchJsonAdapter = PunchJsonAdapter(race.id, dataProcessor)

    @ToJson
    fun toJson(resultData: CompetitorData): ResultJson {
        val result = resultData.readoutData?.result!!
        val punches =
            resultData.readoutData!!.punches.filter { it.punch.punchType != SIRecordType.START }

        return ResultJson(
            check_time = result.checkTime?.toLocalDateTime(race.startDateTime),
            start_time = result.startTime!!.toLocalDateTime(race.startDateTime),
            finish_time = result.finishTime!!.toLocalDateTime(race.startDateTime),
            modified = result.modified,
            run_time = TimeProcessor.durationToFormattedString(result.runTime, true),
            place = result.place,
            // Use new punch_count field and also populate deprecated controls_num for backward compatibility
            punch_count = result.points,
            result_status = dataProcessor
                .resultStatusToShortString(result.resultStatus),
            automatic_status = result.automaticStatus,
            punches = punches
                .map { ap ->
                    val rawCode = ap.alias?.name ?: ap.punch.siCode.toString()
                    val code =
                        if (ap.punch.punchType == SIRecordType.FINISH && rawCode == "0") "F" else rawCode
                    punchJsonAdapter.toJson(ap).also { it.code = code }

                },
            readoutTime = result.readoutTime
        )
    }

    @Suppress("DEPRECATION")
    @FromJson
    fun fromJson(resultJson: ResultJson): ReadoutData {

        val result = Result(
            id = UUID.randomUUID(),
            raceId = race.id,
            siNumber = null, // Will be assigned in competitorJson
            cardType = 0, // Not in ResultJson
            checkTime = resultJson.check_time?.let { SITime(it, race.startDateTime) },
            origCheckTime = resultJson.check_time?.let { SITime(it, race.startDateTime) },
            points = resultJson.punch_count,
            startTime = SITime(resultJson.start_time, race.startDateTime),
            origStartTime = SITime(resultJson.start_time, race.startDateTime),
            finishTime = SITime(resultJson.finish_time, resultJson.start_time),
            origFinishTime = SITime(resultJson.finish_time, resultJson.start_time),
            automaticStatus = resultJson.automatic_status ?: true,
            resultStatus = dataProcessor.resultStatusShortStringToEnum(resultJson.result_status),
            runTime = TimeProcessor.minuteStringToDuration(resultJson.run_time), // must match enum exactly
            modified = resultJson.modified,
            sent = false,
            readoutTime = resultJson.readoutTime ?: LocalDateTime.now()
        )

        val punches = ArrayList<AliasPunch>()
        val punchJsonAdapter = PunchJsonAdapter(race.id, dataProcessor)
        val prevTime = result.startTime!!

        resultJson.punches.forEachIndexed { index, punchJson ->

            val punch = punchJsonAdapter.fromJson(punchJson)
            punch.order = index
            punch.resultId = result.id

            prevTime.addTime(punch.split)
            punch.siTime = prevTime

            punches.add(
                AliasPunch(punch, null)
            )
        }

        return ReadoutData(result, punches)
    }
}