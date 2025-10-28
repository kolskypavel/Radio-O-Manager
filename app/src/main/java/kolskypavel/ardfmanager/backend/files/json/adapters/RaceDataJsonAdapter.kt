package kolskypavel.ardfmanager.backend.files.json.adapters

import UnmatchedResultJsonAdapter
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.json.temps.AliasJson
import kolskypavel.ardfmanager.backend.files.json.temps.RaceJson
import kolskypavel.ardfmanager.backend.room.entity.Alias
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.RaceData
import kolskypavel.ardfmanager.backend.room.enums.RaceBand
import kolskypavel.ardfmanager.backend.room.enums.RaceLevel
import java.time.Duration
import java.util.UUID

class RaceDataJsonAdapter(val dataProcessor: DataProcessor) {
    @ToJson
    fun toJson(raceData: RaceData): RaceJson {
        val categoryAdapter = CategoryJsonAdapter(raceData.race.id)
        val competitorAdapter = CompetitorJsonAdapter(raceData.race.id, dataProcessor)
        val unmatchedAdapter = UnmatchedResultJsonAdapter(raceData.race.id, dataProcessor)

        val race = raceData.race
        return RaceJson(
            race_name = race.name,
            race_start = race.startDateTime,
            race_type = race.raceType,
            race_band = race.raceBand,
            race_level = race.raceLevel,
            race_time_limit = race.timeLimit.toMinutes().toString(),
            race_api_key = race.apiKey,
            categories = raceData.categories.map { cat -> categoryAdapter.toJson(cat) },
            aliases = raceData.aliases.map { al -> AliasJson(al.siCode, al.name) },
            competitors = raceData.competitorData.map { cd -> competitorAdapter.toJson(cd) },
            unmatched_results = raceData.unmatchedReadoutData.map { rd -> unmatchedAdapter.toJson(rd) }
        )
    }

    @FromJson
    fun fromJson(raceJson: RaceJson): RaceData {

        val race = Race(
            id = UUID.randomUUID(),
            name = raceJson.race_name,
            apiKey = raceJson.race_api_key ?: "",
            startDateTime = raceJson.race_start,
            raceType = raceJson.race_type,
            raceBand = raceJson.race_band?: RaceBand.M80,
            raceLevel = raceJson.race_level?: RaceLevel.PRACTICE,
            timeLimit = Duration.ofMinutes(raceJson.race_time_limit?.toLong() ?:120)
        )
        val categoryAdapter = CategoryJsonAdapter(race.id)
        val competitorAdapter = CompetitorJsonAdapter(race.id, dataProcessor)
        val unmatchedAdapter = UnmatchedResultJsonAdapter(race.id, dataProcessor)

        val categories = raceJson.categories.mapIndexed { index, catJson ->
            categoryAdapter.fromJson(catJson).also {
                it.category.raceId = race.id
                it.category.order = index
            }
        }

        val aliases = raceJson.aliases?.map { aliasJson ->
            Alias(
                UUID.randomUUID(),
                race.id,
                aliasJson.alias_si_code,
                aliasJson.alias_name
            ).also { it.raceId = race.id }
        }

        val competitorData = ArrayList<CompetitorData>()
        var highestStartingNum = raceJson.competitors
            .maxByOrNull { c -> c.start_number ?: 0 }
            ?.start_number ?: 0

        for (compJson in raceJson.competitors) {
            val cd = competitorAdapter.fromJson(compJson)
                .also { it.competitorCategory.competitor.raceId = race.id }

            if (compJson.competitor_category.isNotBlank()) {
                cd.competitorCategory.competitor.categoryId =
                    categories.find { compJson.competitor_category == it.category.name }?.category?.id
            }

            if (cd.competitorCategory.competitor.startNumber == 0) {
                highestStartingNum++
                cd.competitorCategory.competitor.startNumber = highestStartingNum
            }
            competitorData.add(cd)
        }

        val unmatchedData =
            raceJson.unmatched_results?.map { json -> unmatchedAdapter.fromJson(json) }

        return RaceData(
            race = race,
            categories = categories,
            aliases = aliases ?: emptyList(),
            competitorData = competitorData,
            unmatchedReadoutData = unmatchedData?.toList() ?: emptyList()
        )
    }
}