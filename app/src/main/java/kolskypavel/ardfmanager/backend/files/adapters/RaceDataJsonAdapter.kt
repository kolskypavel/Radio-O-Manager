package kolskypavel.ardfmanager.backend.files.adapters

import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.RaceData

class RaceDataJsonAdapter {
    @ToJson
    fun toJson(raceData: RaceData): String {
        var res = ""

        return res
    }

//    @FromJson
//    fun fromJson(jsonString: String):RaceData{
//        return RaceData();
//    }
}