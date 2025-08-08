import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.json.temps.ResultJson
import kolskypavel.ardfmanager.backend.files.json.temps.ResultPunchJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData

class ResultJsonAdapter {
    @ToJson
    fun toJson(resultData: CompetitorData): ResultJson {
        val result = resultData.readoutData?.result!!
        return ResultJson(
            run_time = TimeProcessor.durationToMinuteString(result.runTime),
            place = result.place,
            controls_num = result.points,
            result_status = DataProcessor.get()
                .resultStatusToShortString(result.resultStatus),
            punches = resultData.readoutData!!.punches
                .filter { ap -> ap.punch.punchType.name != "START" }
                .map { ap ->
                    val controlType = ap.punch.punchType.name
                    val rawCode = ap.alias?.name ?: ap.punch.siCode.toString()
                    val code = if (controlType == "FINISH" && rawCode == "0") "F" else rawCode

                    ResultPunchJson(
                        code = code,
                        control_type = ap.punch.punchType.name,
                        punch_status = DataProcessor.get()
                            .punchStatusToShortString(ap.punch.punchStatus),
                        real_time = ap.punch.siTime.getTimeString(),
                        week = ap.punch.siTime.getWeek(),
                        day_of_week = ap.punch.siTime.getDayOfWeek(),
                        split_time = TimeProcessor.durationToMinuteString(ap.punch.split)
                    )
                }
        )
    }

    @FromJson
    fun fromJson(json: ResultJson?): ResultData {
        throw NotImplementedError("Deserialization not implemented yet")
    }
}
