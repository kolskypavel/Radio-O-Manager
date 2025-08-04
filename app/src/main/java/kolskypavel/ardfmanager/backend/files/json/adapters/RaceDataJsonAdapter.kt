package kolskypavel.ardfmanager.backend.files.json.adapters

import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.files.json.temps.AliasJson
import kolskypavel.ardfmanager.backend.files.json.temps.RaceJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.RaceData

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

//    @FromJson
//    fun fromJson(jsonString: String):RaceData{
//        return RaceData();
//    }
}