package kolskypavel.ardfmanager.backend.files.json.adapters

import ResultJsonAdapter
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.files.json.temps.CompetitorJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorCategory
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ReadoutData
import java.util.UUID

class CompetitorJsonAdapter {
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
                TimeProcessor.durationToMinuteString(
                    it
                )
            } ?: "",
            result = if (competitorData.readoutData != null) {
                ResultJsonAdapter().toJson(competitorData)
            } else null
        )
    }

    @FromJson
    fun fromJson(competitorJson: CompetitorJson): CompetitorData {
        val competitor = Competitor(
            id = UUID.randomUUID(),
            raceId = UUID.randomUUID(),
            categoryId = null,
            firstName = competitorJson.first_name,
            lastName = competitorJson.last_name,
            club = competitorJson.competitor_club,
            index = competitorJson.competitor_index,
            isMan = competitorJson.competitor_gender,
            birthYear = competitorJson.birth_year,
            siNumber = competitorJson.si_number,
            siRent = competitorJson.si_rent,
            startNumber = competitorJson.start_number,
            drawnRelativeStartTime = TimeProcessor.minuteStringToDuration(competitorJson.competitor_start_time)
        )
        val resultData = ResultJsonAdapter().fromJson(competitorJson.result)
        val readoutData = ReadoutData(resultData.result, resultData.punches)

        return CompetitorData(CompetitorCategory(competitor, null), readoutData)
    }
}