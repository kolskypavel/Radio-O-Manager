import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.json.adapters.PunchJsonAdapter
import kolskypavel.ardfmanager.backend.files.json.adapters.SITimeJsonAdapter
import kolskypavel.ardfmanager.backend.files.json.temps.ResultJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ReadoutData
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import java.time.LocalDateTime
import java.util.UUID

class ResultJsonAdapter(
    val raceId: UUID,
    val dataProcessor: DataProcessor
) {
    val siTimeJsonAdapter = SITimeJsonAdapter()
    val punchJsonAdapter = PunchJsonAdapter(raceId, dataProcessor)

    @ToJson
    fun toJson(resultData: CompetitorData): ResultJson {
        val result = resultData.readoutData?.result!!
        val punches =
            resultData.readoutData!!.punches.filter { it.punch.punchType != SIRecordType.START }

        return ResultJson(
            check_time = result.checkTime?.let { siTimeJsonAdapter.toJson(it) },
            start_time = result.startTime?.let { siTimeJsonAdapter.toJson(it) },
            finish_time = result.finishTime?.let { siTimeJsonAdapter.toJson(it) },
            modified = result.modified,
            run_time = TimeProcessor.durationToFormattedString(result.runTime, true),
            place = result.place,
            // Use new punch_count field and also populate deprecated controls_num for backward compatibility
            punch_count = result.points,
            controls_num = result.points,
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
        val pointsFromJson = resultJson.punch_count ?: resultJson.controls_num ?: 0

        val result = Result(
            id = UUID.randomUUID(),
            raceId = raceId,
            siNumber = null, // Will be assigned in competitorJson
            cardType = 0, // Not in ResultJson
            checkTime = resultJson.check_time?.let { time -> siTimeJsonAdapter.fromJson(resultJson.check_time) },
            origCheckTime = resultJson.check_time?.let { time ->
                siTimeJsonAdapter.fromJson(
                    resultJson.check_time
                )
            },
            points = pointsFromJson,
            startTime = resultJson.start_time?.let { time -> siTimeJsonAdapter.fromJson(resultJson.start_time) },
            origStartTime = resultJson.start_time?.let { time ->
                siTimeJsonAdapter.fromJson(
                    resultJson.start_time
                )
            },
            finishTime = resultJson.finish_time?.let { time -> siTimeJsonAdapter.fromJson(resultJson.finish_time) },
            origFinishTime = resultJson.finish_time?.let { time ->
                siTimeJsonAdapter.fromJson(
                    resultJson.finish_time
                )
            },
            automaticStatus = resultJson.automatic_status ?: true,
            resultStatus = dataProcessor.resultStatusShortStringToEnum(resultJson.result_status),
            runTime = TimeProcessor.minuteStringToDuration(resultJson.run_time), // must match enum exactly
            modified = resultJson.modified,
            sent = false,
            readoutTime = resultJson.readoutTime ?: LocalDateTime.now()
        )


        val punches = ArrayList<AliasPunch>()
        val punchJsonAdapter = PunchJsonAdapter(raceId, dataProcessor)
        resultJson.punches.forEachIndexed { index, punchJson ->

            val punch = punchJsonAdapter.fromJson(punchJson)
            punch.order = index
            punch.resultId = result.id

            punches.add(
                AliasPunch(punch, null)
            )
        }

        return ReadoutData(result, punches)
    }
}