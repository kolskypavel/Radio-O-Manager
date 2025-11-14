import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.json.adapters.PunchJsonAdapter
import kolskypavel.ardfmanager.backend.files.json.temps.UnmatchedResultJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ReadoutData
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.sportident.SITime
import java.util.UUID

class UnmatchedResultJsonAdapter(val race: Race, val dataProcessor: DataProcessor) {
    val punchJsonAdapter = PunchJsonAdapter(race.id, dataProcessor)

    @ToJson
    fun toJson(readoutData: ReadoutData): UnmatchedResultJson {
        val result = readoutData.result

        return UnmatchedResultJson(
            check_time = result.checkTime?.toLocalDateTime(race.startDateTime),
            start_time = result.startTime!!.toLocalDateTime(race.startDateTime),
            finish_time = result.finishTime!!.toLocalDateTime(race.startDateTime),
            si_number = result.siNumber,
            run_time = TimeProcessor.durationToFormattedString(result.runTime, true),
            punches = readoutData.punches.map { ap -> punchJsonAdapter.toJson(ap) },
        )
    }

    @FromJson
    fun fromJson(json: UnmatchedResultJson): ReadoutData {
        val result = Result(
            id = UUID.randomUUID(),
            raceId = race.id,
            siNumber = json.si_number,
            cardType = 0,
            checkTime = json.check_time?.let { SITime(json.check_time, race.startDateTime) },
            origCheckTime = json.check_time?.let { SITime(json.check_time, race.startDateTime) },
            startTime = SITime(json.start_time, race.startDateTime),
            origStartTime = SITime(json.start_time, race.startDateTime),
            finishTime = SITime(json.finish_time, race.startDateTime),
            origFinishTime = SITime(json.finish_time, race.startDateTime),
            automaticStatus = false,
            resultStatus = ResultStatus.NO_RANKING,
            runTime = TimeProcessor.minuteStringToDuration(json.run_time),
            modified = false,
            sent = false
        )

        val punches = ArrayList<AliasPunch>()
        val punchJsonAdapter = PunchJsonAdapter(race.id, dataProcessor)

        val prevTime = result.startTime!!

        json.punches.forEachIndexed { index, punchJson ->

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