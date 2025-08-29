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
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import java.util.UUID

class ResultJsonAdapter(val raceId: UUID, val filterStart: Boolean) {
    val siTimeJsonAdapter = SITimeJsonAdapter()
    val punchJsonAdapter = PunchJsonAdapter(raceId)

    @ToJson
    fun toJson(resultData: CompetitorData): ResultJson {
        val result = resultData.readoutData?.result!!
        var punches = resultData.readoutData!!.punches

        if (filterStart) {
            punches = punches.filter { it.punch.punchType != SIRecordType.START }
        }

        return ResultJson(
            check_time = result.checkTime?.let { siTimeJsonAdapter.toJson(it) },
            start_time = result.startTime?.let { siTimeJsonAdapter.toJson(it) },
            finish_time = result.finishTime?.let { siTimeJsonAdapter.toJson(it) },
            modified = result.modified,
            run_time = TimeProcessor.durationToFormattedString(result.runTime, true),
            place = result.place,
            controls_num = result.points,
            result_status = DataProcessor.get()
                .resultStatusToShortString(result.resultStatus),
            punches = punches
                .map { ap ->
                    val rawCode = ap.alias?.name ?: ap.punch.siCode.toString()
                    val code =
                        if (ap.punch.punchType == SIRecordType.FINISH && rawCode == "0") "F" else rawCode
                    punchJsonAdapter.toJson(ap).also { it.code = code }

                }
        )
    }

    @FromJson
    fun fromJson(json: ResultJson): ReadoutData {
        val result = Result(
            id = UUID.randomUUID(),
            raceId = raceId, // replace with real value later
            siNumber = null, // will be assigned elsewhere
            cardType = 0, // Not in ResultJson
            checkTime = null, // default/fallback
            origCheckTime = null,
            startTime = null,
            origStartTime = null,
            finishTime = null,
            origFinishTime = null,
            automaticStatus = false,
            resultStatus = ResultStatus.valueOf(json.result_status),
            runTime = TimeProcessor.minuteStringToDuration(json.run_time), // must match enum exactly
            modified = false,
            sent = false
        )


        val punches = ArrayList<AliasPunch>()
        val punchJsonAdapter = PunchJsonAdapter(raceId)
        json.punches.forEachIndexed { index, punchJson ->

            val punchType = SIRecordType.valueOf(punchJson.control_type)
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