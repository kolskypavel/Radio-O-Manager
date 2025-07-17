package kolskypavel.ardfmanager.backend.files.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.enums.RaceBand
import kolskypavel.ardfmanager.backend.room.enums.RaceLevel
import kolskypavel.ardfmanager.backend.room.enums.RaceType
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

class RaceAdapter {

    @ToJson
    fun toJson(race: Race): String {
        val map = mutableMapOf<String, Any?>(
            "id" to race.id.toString(),
            "race_name" to race.name,
            "race_start" to race.startDateTime,
            "race_id" to race.externalId,
            "race_type" to race.raceType,
            "race_band" to race.raceBand,
            "race_level" to race.raceLevel,
            "race_time_limit" to race.timeLimit
        )
        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter = moshi.adapter<Map<String, Any?>>(type)
        return adapter.toJson(map)
    }

    @FromJson
    fun fromJson(reader: JsonReader): Race {
        var id: UUID = UUID.randomUUID()
        var name = ""
        var externalId: Long? = null
        var startDateTime = LocalDateTime.now()
        var raceType = RaceType.CLASSICS
        var raceBand = RaceBand.M80
        var raceLevel = RaceLevel.TRAINING
        var timeLimit = Duration.ZERO

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = UUID.fromString(reader.nextString())
                "race_name" -> name = reader.nextString()
                "race_id" -> externalId = reader.nextString().toLongOrNull()
                "race_start" -> startDateTime = LocalDateTime.parse(reader.nextString())
                "race_type" -> raceType = RaceType.valueOf(reader.nextString())
                "race_band" -> raceBand = RaceBand.valueOf(reader.nextString())
                "race_level" -> raceLevel = RaceLevel.valueOf(reader.nextString())
                "race_time_limit" -> timeLimit = Duration.parse(reader.nextString())
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return Race(id, name, externalId, startDateTime, raceType, raceLevel, raceBand, timeLimit)
    }
}