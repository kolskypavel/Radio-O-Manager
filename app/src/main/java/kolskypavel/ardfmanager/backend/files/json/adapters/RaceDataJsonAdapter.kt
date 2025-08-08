package kolskypavel.ardfmanager.backend.files.json.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.files.constants.FileConstants
import kolskypavel.ardfmanager.backend.files.json.temps.AliasJson
import kolskypavel.ardfmanager.backend.files.json.temps.RaceJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Alias
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.RaceData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ReadoutData
import java.util.UUID

class RaceDataJsonAdapter {
    @ToJson
    fun toJson(raceData: RaceData): RaceJson {
        val categoryAdapter = CategoryJsonAdapter()
        val competitorAdapter = CompetitorJsonAdapter()
        val race = raceData.race
        return RaceJson(
            race_name = race.name,
            race_start = race.startDateTime,
            race_type = race.raceType,
            race_band = race.raceBand,
            race_level = race.raceLevel,
            race_time_limit = TimeProcessor.durationToMinuteString(race.timeLimit),
            categories = raceData.categories.map { cat -> categoryAdapter.toJson(cat) },
            aliases = raceData.aliases.map { al -> AliasJson(al.siCode, al.name) },
            competitors = raceData.competitorData.map { cd -> competitorAdapter.toJson(cd) }
        )
    }

    @FromJson
    fun fromJson(raceJson: RaceJson): RaceData {
        val categoryAdapter = CategoryJsonAdapter()
        val competitorAdapter = CompetitorJsonAdapter()

        val race = Race(
            id = UUID.randomUUID(),
            name = raceJson.race_name,
            apiKey = "",
            startDateTime = raceJson.race_start,
            raceType = raceJson.race_type,
            raceBand = raceJson.race_band,
            raceLevel = raceJson.race_level,
            timeLimit = TimeProcessor.minuteStringToDuration(raceJson.race_time_limit)
        )

        val categories = raceJson.categories.map { catJson ->
            categoryAdapter.fromJson(catJson).also { it.category.raceId = race.id }
        }

        val aliases = raceJson.aliases.map { aliasJson ->
            Alias(
                UUID.randomUUID(),
                race.id,
                aliasJson.alias_si_code,
                aliasJson.alias_name
            ).also { it.raceId = race.id }
        }

        val competitorData = ArrayList<CompetitorData>()
        val unknownData = ArrayList<ReadoutData>()
        raceJson.competitors.forEach { compJson ->
            val cd = competitorAdapter.fromJson(compJson)
                .also { it.competitorCategory.competitor.raceId = race.id }

            // Filter out the unknown results
            if (compJson.first_name == FileConstants.UNKNOWN_COMPETITOR_SYMBOL && cd.readoutData != null) {
                unknownData.add(ReadoutData(cd.readoutData!!.result, cd.readoutData!!.punches))
            }
            else if (compJson.competitor_category.isNotBlank()) {
                cd.competitorCategory.competitor.categoryId =
                    categories.find { compJson.competitor_category == it.category.name }?.category?.id
            }
        }

        return RaceData(
            race = race,
            categories = categories,
            aliases = aliases,
            competitorData = competitorData,
            unknowReadoutData = unknownData.toList()
        )
    }
}