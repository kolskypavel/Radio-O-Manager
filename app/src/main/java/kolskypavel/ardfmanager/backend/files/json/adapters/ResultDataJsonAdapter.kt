import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.files.json.temps.ResultDataJson
import kolskypavel.ardfmanager.backend.files.json.temps.ResultPunchJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData

class ResultDataJsonAdapter {
    @ToJson
    fun toJson(resultData: ResultData): ResultDataJson {
        //TODO: add competitors
        return ResultDataJson(
            run_time = TimeProcessor.durationToMinuteString(resultData.result.runTime),
            place = resultData.result.place,
            controls_num = resultData.result.points,
            result_status = resultData.result.resultStatus.name,
            punches = resultData.punches.map { ap ->
                ResultPunchJson(
                    code = ap.alias?.name ?: ap.punch.siCode.toString(),
                    control_type = ap.punch.punchType.name,
                    punch_status = ap.punch.punchStatus.name,
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
