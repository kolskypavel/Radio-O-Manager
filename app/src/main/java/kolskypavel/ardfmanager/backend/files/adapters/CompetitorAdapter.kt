package kolskypavel.ardfmanager.backend.files.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.room.entity.Competitor

class CompetitorAdapter {
    @ToJson
    fun toJson(competitor: Competitor): String {
        var res = ""

        return res
    }

    @FromJson
    fun fromJson(jsonString: String): Competitor{
        val parsed = Competitor();

        return parsed
    }
}