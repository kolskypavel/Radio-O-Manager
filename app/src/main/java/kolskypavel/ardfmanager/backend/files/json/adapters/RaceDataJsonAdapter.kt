package kolskypavel.ardfmanager.backend.files.json.adapters

import UnmatchedResultJsonAdapter
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.files.json.temps.AliasJson
import kolskypavel.ardfmanager.backend.files.json.temps.RaceJson
import kolskypavel.ardfmanager.backend.room.entity.Alias
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.RaceData
import java.time.Duration
import java.util.UUID

class RaceDataJsonAdapter {
    @ToJson
    fun toJson(raceData: RaceData): RaceJson {
        val categoryAdapter = CategoryJsonAdapter(raceData.race.id)
        val competitorAdapter = CompetitorJsonAdapter(raceData.race.id)
        val unmatchedAdapter = UnmatchedResultJsonAdapter(raceData.race.id)

        val race = raceData.race
        return RaceJson(
            race_name = race.name,
            race_start = race.startDateTime,
            race_type = race.raceType,
            race_band = race.raceBand,
            race_level = race.raceLevel,
            race_time_limit = race.timeLimit.toMinutes().toString(),
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
            apiKey = "",
            startDateTime = raceJson.race_start,
            raceType = raceJson.race_type,
            raceBand = raceJson.race_band,
            raceLevel = raceJson.race_level,
            timeLimit = Duration.ofMinutes(raceJson.race_time_limit.toLong())
        )
        val categoryAdapter = CategoryJsonAdapter(race.id)
        val competitorAdapter = CompetitorJsonAdapter(race.id)
        val unmatchedAdapter = UnmatchedResultJsonAdapter(race.id)

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

        for (compJson in raceJson.competitors) {
            val cd = competitorAdapter.fromJson(compJson)
                .also { it.competitorCategory.competitor.raceId = race.id }

            if (compJson.competitor_category.isNotBlank()) {
                cd.competitorCategory.competitor.categoryId =
                    categories.find { compJson.competitor_category == it.category.name }?.category?.id
            }
            competitorData.add(cd)
        }
        val unmatchedData =
            raceJson.unmatched_results.map { json -> unmatchedAdapter.fromJson(json) }

        return RaceData(
            race = race,
            categories = categories,
            aliases = aliases,
            competitorData = competitorData,
            unmatchedReadoutData = unmatchedData.toList()
        )
    }
}