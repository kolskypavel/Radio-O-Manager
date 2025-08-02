import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.json.temps.ResultDataJson
import kolskypavel.ardfmanager.backend.files.json.temps.ResultPunchJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData

class ResultDataJsonAdapter {
    @ToJson
    fun toJson(resultData: ResultData): ResultDataJson {

        return ResultDataJson(
            run_time = TimeProcessor.durationToMinuteString(resultData.result.runTime),
            place = resultData.result.place,
            controls_num = resultData.result.points,
            result_status = DataProcessor.get().resultStatusToShortString(resultData.result.resultStatus),
            punches = resultData.punches
                .filter { ap -> ap.punch.punchType.name != "START" }
                .map { ap ->
                    val controlType = ap.punch.punchType.name
                    val rawCode = ap.alias?.name ?: ap.punch.siCode.toString()
                    val code = if (controlType == "FINISH" && rawCode == "0") "F" else rawCode

                    ResultPunchJson(
                        code = code,
                        control_type = ap.punch.punchType.name,
                        punch_status = DataProcessor.get().punchStatusToShortString(ap.punch.punchStatus),
                        split_time = TimeProcessor.durationToMinuteString(ap.punch.split)
                    )
                    }
        )
    }

    @FromJson
    fun fromJson(json: ResultDataJson): ResultData {
        throw NotImplementedError("Deserialization not implemented yet")
    }
}
