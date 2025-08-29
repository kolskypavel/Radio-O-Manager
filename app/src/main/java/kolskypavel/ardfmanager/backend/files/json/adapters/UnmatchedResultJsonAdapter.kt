import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.files.json.adapters.PunchJsonAdapter
import kolskypavel.ardfmanager.backend.files.json.adapters.SITimeJsonAdapter
import kolskypavel.ardfmanager.backend.files.json.temps.UnmatchedResultJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ReadoutData
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import java.util.UUID

class UnmatchedResultJsonAdapter(val raceId: UUID) {
    val punchJsonAdapter = PunchJsonAdapter(raceId)
    val siTimeJsonAdapter = SITimeJsonAdapter()

    @ToJson
    fun toJson(readoutData: ReadoutData): UnmatchedResultJson {
        val result = readoutData.result

        return UnmatchedResultJson(
            check_time = result.checkTime?.let { siTimeJsonAdapter.toJson(it) },
            start_time = result.startTime?.let { siTimeJsonAdapter.toJson(it) },
            finish_time = result.finishTime?.let { siTimeJsonAdapter.toJson(it) },
            si_number = result.siNumber,
            run_time = TimeProcessor.durationToFormattedString(result.runTime,true),
            punches = readoutData.punches.map { ap -> punchJsonAdapter.toJson(ap) },
        )
    }

    @FromJson
    fun fromJson(json: UnmatchedResultJson): ReadoutData {
        val result = Result(
            id = UUID.randomUUID(),
            raceId = raceId,
            siNumber = json.si_number,
            cardType = 0,
            checkTime = json.check_time?.let { siTimeJsonAdapter.fromJson(it) },
            origCheckTime = json.check_time?.let { siTimeJsonAdapter.fromJson(it) },
            startTime = json.start_time?.let { siTimeJsonAdapter.fromJson(it) },
            origStartTime = json.start_time?.let { siTimeJsonAdapter.fromJson(it) },
            finishTime = json.finish_time?.let { siTimeJsonAdapter.fromJson(it) },
            origFinishTime = json.finish_time?.let { siTimeJsonAdapter.fromJson(it) },
            automaticStatus = false,
            resultStatus = ResultStatus.NO_RANKING,
            runTime = TimeProcessor.minuteStringToDuration(json.run_time),
            modified = false,
            sent = false
        )


        val punches = ArrayList<AliasPunch>()
        val punchJsonAdapter = PunchJsonAdapter(raceId)
        json.punches.forEachIndexed { index, punchJson ->

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