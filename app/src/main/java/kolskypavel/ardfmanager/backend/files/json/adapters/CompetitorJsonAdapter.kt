package kolskypavel.ardfmanager.backend.files.json.adapters

import ResultJsonAdapter
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.files.json.temps.CompetitorJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData

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
            result = ResultJsonAdapter().toJson(competitorData)
        )
    }

    @FromJson
    fun fromJson(jsonString: String): Competitor {
        val parsed = Competitor();

        return parsed
    }
}