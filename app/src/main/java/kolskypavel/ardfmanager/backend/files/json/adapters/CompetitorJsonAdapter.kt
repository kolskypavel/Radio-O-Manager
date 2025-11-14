package kolskypavel.ardfmanager.backend.files.json.adapters

import ResultJsonAdapter
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.json.temps.CompetitorJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorCategory
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ReadoutData
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import java.util.UUID

class CompetitorJsonAdapter(val race: Race, val dataProcessor: DataProcessor) {
    @ToJson
    fun toJson(competitorData: CompetitorData): CompetitorJson {
        val competitor = competitorData.competitorCategory.competitor
        return CompetitorJson(

            first_name = competitor.firstName,
            last_name = competitor.lastName,
            competitor_club = competitor.club,
            competitor_category = (competitorData.competitorCategory.category?.name ?: ""),
            competitor_index = competitor.index,
            competitor_gender = competitor.isMan,
            birth_year = competitor.birthYear,
            si_number = competitor.siNumber,
            si_rent = competitor.siRent,
            start_number = competitor.startNumber,
            competitor_start_time = competitor.drawnRelativeStartTime?.let {
                TimeProcessor.durationToFormattedString(
                    it, true
                )
            } ?: "",
            result = if (competitorData.readoutData != null &&
                competitorData.readoutData!!.result.resultStatus != ResultStatus.ERROR      // Do not serialize when start/finish time is missing
            ) {
                ResultJsonAdapter(race, dataProcessor).toJson(competitorData)
            } else null
        )
    }

    @FromJson
    fun fromJson(competitorJson: CompetitorJson): CompetitorData {
        val competitor = Competitor(
            id = UUID.randomUUID(),
            raceId = race.id,
            categoryId = null,
            firstName = competitorJson.first_name,
            lastName = competitorJson.last_name,
            club = competitorJson.competitor_club ?: "",
            index = competitorJson.competitor_index ?: "",
            isMan = competitorJson.competitor_gender,
            birthYear = competitorJson.birth_year,
            siNumber = competitorJson.si_number,
            siRent = competitorJson.si_rent ?: false,
            startNumber = competitorJson.start_number ?: 0,
            drawnRelativeStartTime = if (competitorJson.competitor_start_time?.isNotEmpty() == true) {
                TimeProcessor.minuteStringToDuration(competitorJson.competitor_start_time)
            } else null
        )
        if (competitorJson.result != null) {
            val resultData = ResultJsonAdapter(
                race,
                dataProcessor
            ).fromJson(competitorJson.result)
            resultData.result.competitorId = competitor.id
            resultData.result.siNumber = competitor.siNumber
            val readoutData = ReadoutData(resultData.result, resultData.punches)
            return CompetitorData(CompetitorCategory(competitor, null), readoutData)
        }
        return CompetitorData(CompetitorCategory(competitor, null), null)
    }
}